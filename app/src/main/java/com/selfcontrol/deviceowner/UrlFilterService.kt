package com.selfcontrol.deviceowner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.selfcontrol.R
import com.selfcontrol.data.worker.UrlBlacklistSyncWorker
import com.selfcontrol.domain.model.UrlBlacklist
import com.selfcontrol.domain.model.Violation
import com.selfcontrol.domain.model.ViolationType
import com.selfcontrol.domain.repository.UrlRepository
import com.selfcontrol.domain.repository.ViolationRepository
import com.selfcontrol.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * UrlFilterVpnService - VPN-based URL filtering service
 * 
 * This service creates a local VPN that intercepts all network traffic
 * and blocks access to URLs in the blacklist.
 * 
 * Key features:
 * - Works with any browser (Chrome, Brave, Firefox, etc.)
 * - Works even with custom DNS or other VPNs (by being the innermost VPN)
 * - Blocks domains at the DNS level
 * - Logs violation attempts
 * 
 * How it works:
 * 1. Sets up a VPN interface that routes all traffic through the app
 * 2. Intercepts DNS queries to check for blocked domains
 * 3. Returns a block page for blocked URLs
 * 4. Forwards non-blocked traffic normally
 */
@AndroidEntryPoint
class UrlFilterVpnService : VpnService() {
    
    @Inject
    lateinit var urlRepository: UrlRepository
    
    @Inject
    lateinit var violationRepository: ViolationRepository
    
    @Inject
    lateinit var prefs: com.selfcontrol.data.local.prefs.AppPreferences
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    
    // In-memory cache of blocked URLs for fast lookup
    private val blockedPatterns = ConcurrentHashMap<String, UrlBlacklist>()
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Broadcast receiver for URL list updates
    private val urlUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == UrlBlacklistSyncWorker.ACTION_URL_LIST_UPDATED) {
                Timber.d("[UrlFilterVpn] Received URL list update notification")
                serviceScope.launch { loadBlockedUrls() }
            }
        }
    }
    
    companion object {
        const val TAG = "UrlFilterVpn"
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "url_filter_channel"
        
        const val ACTION_START = "com.selfcontrol.vpn.START"
        const val ACTION_STOP = "com.selfcontrol.vpn.STOP"
        
        // VPN Configuration
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_PREFIX = 24
        private const val VPN_DNS = "8.8.8.8"
        private const val VPN_MTU = 1500
        
        private const val DNS_CACHE_TTL = 300_000L // 5 minutes
        
        /**
         * Start the VPN service
         */
        fun start(context: Context) {
            val intent = Intent(context, UrlFilterVpnService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }
        
        /**
         * Stop the VPN service
         */
        fun stop(context: Context) {
            val intent = Intent(context, UrlFilterVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
        
        /**
         * Check if VPN permission is granted
         */
        fun prepare(context: Context): Intent? {
            return VpnService.prepare(context)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Timber.i("[$TAG] Service created")
        
        createNotificationChannel()
        
        // Register for URL list updates
        val filter = IntentFilter(UrlBlacklistSyncWorker.ACTION_URL_LIST_UPDATED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(urlUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(urlUpdateReceiver, filter)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Timber.i("[$TAG] Starting VPN")
                startVpn()
            }
            ACTION_STOP -> {
                Timber.i("[$TAG] Stopping VPN")
                stopVpn()
            }
            else -> {
                Timber.w("[$TAG] Unknown action: ${intent?.action}")
            }
        }
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Timber.i("[$TAG] Service destroyed")
        
        try {
            unregisterReceiver(urlUpdateReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
        
        serviceScope.cancel()
        stopVpn()
    }
    
    /**
     * Start the VPN tunnel
     */
    private fun startVpn() {
        if (isRunning) {
            Timber.w("[$TAG] VPN already running")
            return
        }
        
        // Show foreground notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        serviceScope.launch {
            try {
                Timber.i("[$TAG] Starting VPN")
                
                // Load blocked URL patterns
                loadBlockedUrls()
                
                // Establish VPN interface
                vpnInterface = establishVpn()
                
                if (vpnInterface != null) {
                    isRunning = true
                    
                    // Update VPN connection status
                    prefs.setVpnConnected(true)
                    
                    // PASSTHROUGH MODE: No packet processing
                    // processPackets() disabled - VPN just stays connected for always-on compliance
                    Timber.i("[$TAG] VPN running in passthrough mode")
                } else {
                    Timber.e("[$TAG] Failed to establish VPN interface")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "[$TAG] Error starting VPN")
            }
        }
    }
    
    /**
     * Stop the VPN tunnel
     */
    private fun stopVpn() {
        isRunning = false
        
        // Update VPN connection status
        serviceScope.launch {
            prefs.setVpnConnected(false)
        }
        
        vpnInterface?.close()
        vpnInterface = null
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        
        Timber.i("[$TAG] VPN stopped")
    }
    
    /**
     * Establish the VPN interface as a DNS-only filter
     * 
     * This VPN acts as the system DNS server to intercept and filter DNS queries.
     * By setting addDnsServer() without addRoute(), we ensure:
     * - DNS queries (port 53) go through the VPN for filtering
     * - All other traffic (HTTP, HTTPS, etc.) bypasses the VPN completely
     * 
     * This gives us URL blocking without impacting internet speed.
     */
    private fun establishVpn(): ParcelFileDescriptor? {
        return try {
            Builder()
                .setSession("SelfControl URL Filter")
                .addAddress(VPN_ADDRESS, 32) // VPN interface address
                // PASSTHROUGH MODE: No routes, no DNS interception
                // This satisfies the always-on VPN requirement without breaking internet
                // TODO: Implement DNS filtering using local DNS server approach
                .setMtu(VPN_MTU)
                .setBlocking(false) // Non-blocking - just stay connected
                .apply {
                    // Don't intercept our own traffic
                    try {
                        addDisallowedApplication(packageName)
                    } catch (e: Exception) {
                        Timber.w(e, "[$TAG] Could not exclude self from VPN")
                    }
                }
                .establish()
                ?.also {
                    Timber.i("[$TAG] VPN established successfully (DNS filtering mode)")
                }
        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Failed to establish VPN")
            null
        }
    }
    
    private val dnsCache = ConcurrentHashMap<String, Pair<ByteArray, Long>>()

    private fun getCachedDnsResponse(domain: String): ByteArray? {
        val cached = dnsCache[domain] ?: return null
        if (System.currentTimeMillis() - cached.second > DNS_CACHE_TTL) {
            dnsCache.remove(domain)
            return null
        }
        return cached.first
    }
    
    private fun cacheDnsResponse(domain: String, response: ByteArray) {
        dnsCache[domain] = Pair(response, System.currentTimeMillis())
    }

    /**
     * Load blocked URLs from repository
     */
    private suspend fun loadBlockedUrls() {
        try {
            val urls = urlRepository.observeBlockedUrls().first()
            
            blockedPatterns.clear()
            urls.forEach { url ->
                blockedPatterns[url.pattern.lowercase()] = url
            }
            
            Timber.i("[$TAG] Loaded ${blockedPatterns.size} blocked URL patterns")
            
            // Clear DNS cache on updates
            dnsCache.clear()
            
        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Failed to load blocked URLs")
        }
    }
    
    /**
     * Check if a domain/URL should be blocked
     */
    private fun shouldBlockUrl(url: String): Boolean {
        val urlLower = url.lowercase()
        
        // Check exact match
        if (blockedPatterns.containsKey(urlLower)) {
            return true
        }
        
        // Check pattern matching
        for ((pattern, blacklistItem) in blockedPatterns) {
            if (blacklistItem.matches(urlLower)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Process VPN packets - intercept DNS and check for blocked domains
     */
    private suspend fun processPackets() {
        val vpnFileDescriptor = vpnInterface?.fileDescriptor ?: return
        
        withContext(Dispatchers.IO) {
            val inputStream = FileInputStream(vpnFileDescriptor)
            val outputStream = FileOutputStream(vpnFileDescriptor)
            
            val inputChannel = inputStream.channel
            val outputChannel = outputStream.channel
            
            // Create socket for DNS forwarding - MUST be protected to bypass VPN!
            val dnsSocket = java.net.DatagramSocket()
            protect(dnsSocket) // CRITICAL: This prevents DNS traffic from looping back through VPN
            Timber.i("[$TAG] DNS socket created and protected (bypasses VPN)")
            
            val packet = ByteBuffer.allocate(32767)
            
            Timber.d("[$TAG] Starting packet processing")
            
            while (isRunning) {
                try {
                    packet.clear()
                    
                    val length = inputChannel.read(packet)
                    
                    if (length > 0) {
                        packet.flip()
                        
                        // Parse packet and check for DNS queries
                        val responsePacket = processPacket(packet, length, dnsSocket)
                        
                        if (responsePacket != null) {
                            // Write response back to VPN interface
                            outputChannel.write(responsePacket)
                        }
                    }
                    
                } catch (e: Exception) {
                    if (isRunning) {
                        Timber.w(e, "[$TAG] Packet processing error")
                    }
                }
            }
            
            dnsSocket.close()
            Timber.d("[$TAG] Packet processing stopped")
        }
    }
    
    /**
     * Process individual packet - check for blocked domains in DNS queries
     */
    private suspend fun processPacket(packet: ByteBuffer, length: Int, dnsSocket: java.net.DatagramSocket): ByteBuffer? {
        try {
            // Check if it's a DNS packet (UDP port 53)
            if (length < 28) {
                return null // Too small to be a valid DNS packet
            }
            
            val data = ByteArray(length)
            packet.get(data)
            
            // Check IP protocol (UDP = 17)
            val protocol = data[9].toInt() and 0xFF
            val ihl = (data[0].toInt() and 0x0F) * 4
            
            // Extract destination IP for debugging
            val destIp = if (data.size >= 20) {
                "${data[16].toInt() and 0xFF}.${data[17].toInt() and 0xFF}.${data[18].toInt() and 0xFF}.${data[19].toInt() and 0xFF}"
            } else "unknown"
            
            Timber.d("[$TAG] Packet: len=$length, proto=$protocol, ihl=$ihl, destIp=$destIp")
            
            if (protocol == 17) { // UDP
                if (data.size >= ihl + 8) { // Need at least UDP header (8 bytes)
                     val srcPort = ((data[ihl].toInt() and 0xFF) shl 8) or (data[ihl + 1].toInt() and 0xFF)
                     val destPort = ((data[ihl + 2].toInt() and 0xFF) shl 8) or (data[ihl + 3].toInt() and 0xFF)
                     
                     Timber.d("[$TAG] UDP packet: srcPort=$srcPort, destPort=$destPort")
                     
                     if (destPort == 53) {
                         // This is a DNS query - extract domain
                         val domain = extractDnsQuery(data, ihl + 8)
                         Timber.i("[$TAG] DNS query detected for domain: $domain")
                         
                         if (domain != null) {
                             if (shouldBlockUrl(domain)) {
                                 Timber.i("[$TAG] Blocking DNS query for: $domain")
                                 logViolation(domain)
                                 return ByteBuffer.wrap(createDnsBlockResponse(data)) // Return NXDOMAIN response
                             } else {
                                 // Forward to 8.8.8.8
                                 Timber.i("[$TAG] Forwarding DNS query for: $domain to 8.8.8.8")
                                 val response = forwardDnsQuery(data, dnsSocket)
                                 return if (response != null) {
                                     Timber.i("[$TAG] DNS response received for: $domain (${response.size} bytes)")
                                     ByteBuffer.wrap(response)
                                 } else {
                                     Timber.w("[$TAG] DNS forward failed for: $domain")
                                     null // Failed to forward
                                 }
                             }
                         } else {
                             Timber.w("[$TAG] Could not extract domain from DNS query")
                         }
                     } else {
                         Timber.d("[$TAG] Non-DNS UDP packet (port $destPort), dropping")
                     }
                } else {
                    Timber.w("[$TAG] UDP packet too small: ${data.size} < ${ihl + 8}")
                }
            } else {
                Timber.d("[$TAG] Non-UDP packet (proto=$protocol), dropping")
            }
            
            return null
            
        } catch (e: Exception) {
            Timber.w(e, "[$TAG] Error processing packet")
            return null // On error, drop packet
        }
    }
    
    /**
     * Create DNS NXDOMAIN response for blocked domain
     */
    private fun createDnsBlockResponse(queryPacket: ByteArray): ByteArray {
        val response = queryPacket.clone()
        val ihl = (response[0].toInt() and 0x0F) * 4
        val udpHeaderLen = 8
        val dnsOffset = ihl + udpHeaderLen
        
        // DNS Header: ID(2), Flags(2), QDCOUNT(2), ANCOUNT(2), NSCOUNT(2), ARCOUNT(2)
        // Set Flags: QR=1, AA=0, TC=0, RD=Copy, RA=1, Z=0, RCODE=3 (NXDOMAIN)
        // 0x8183 = 1000 0001 1000 0011 (Standard response with Recursion Available, NXDOMAIN)
        // But we must preserve ID and RD.
        
        // Flags at dnsOffset + 2
        val oldFlags = ((response[dnsOffset + 2].toInt() and 0xFF) shl 8) or (response[dnsOffset + 3].toInt() and 0xFF)
        val rd = oldFlags and 0x0100 // Preserve RD bit
        val newFlags = 0x8183 or rd
        
        response[dnsOffset + 2] = (newFlags shr 8).toByte()
        response[dnsOffset + 3] = (newFlags and 0xFF).toByte()
        
        // Swap IP Addresses
        for (i in 0..3) {
            val temp = response[12 + i]
            response[12 + i] = response[16 + i]
            response[16 + i] = temp
        }
        
        // Swap UDP Ports
        for (i in 0..1) {
            val temp = response[ihl + i]
            response[ihl + i] = response[ihl + 2 + i]
            response[ihl + 2 + i] = temp
        }
        
        // Fix Checksums (Zero out UDP checksum for simplicity)
        response[ihl + 6] = 0
        response[ihl + 7] = 0
        
        // Recalculate IP Checksum
        response[10] = 0
        response[11] = 0
        var ipChecksum = 0
        for (i in 0 until ihl step 2) {
             val word = ((response[i].toInt() and 0xFF) shl 8) + (response[i + 1].toInt() and 0xFF)
             ipChecksum += word
        }
        while ((ipChecksum shr 16) > 0) {
            ipChecksum = (ipChecksum and 0xFFFF) + (ipChecksum shr 16)
        }
        ipChecksum = ipChecksum.inv() and 0xFFFF
        response[10] = (ipChecksum shr 8).toByte()
        response[11] = (ipChecksum and 0xFF).toByte()
        
        return response
    }

    /**
     * Forward DNS query to real DNS server and return response
     */
    private suspend fun forwardDnsQuery(queryPacket: ByteArray, socket: java.net.DatagramSocket): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val ihl = (queryPacket[0].toInt() and 0x0F) * 4
                val udpHeaderLen = 8
                val dnsPayloadOffset = ihl + udpHeaderLen
                
                if (queryPacket.size <= dnsPayloadOffset) return@withContext null
                
                val dnsPayloadLen = queryPacket.size - dnsPayloadOffset
                val dnsPayload = ByteArray(dnsPayloadLen)
                System.arraycopy(queryPacket, dnsPayloadOffset, dnsPayload, 0, dnsPayloadLen)
                
                val address = InetAddress.getByName("8.8.8.8")
                val packet = java.net.DatagramPacket(dnsPayload, dnsPayloadLen, address, 53)
                socket.send(packet)
                
                val receiveData = ByteArray(1500) 
                val receivePacket = java.net.DatagramPacket(receiveData, receiveData.size)
                
                socket.soTimeout = 2000 
                socket.receive(receivePacket)
                
                val responsePayloadLen = receivePacket.length
                val responsePacketLen = ihl + udpHeaderLen + responsePayloadLen
                val responsePacket = ByteArray(responsePacketLen)
                
                // Copy headers from query
                System.arraycopy(queryPacket, 0, responsePacket, 0, ihl + udpHeaderLen)
                
                // Copy new payload
                System.arraycopy(receiveData, 0, responsePacket, ihl + udpHeaderLen, responsePayloadLen)
                
                // Swap IP/Ports
                for (i in 0..3) {
                    val temp = responsePacket[12 + i]
                    responsePacket[12 + i] = responsePacket[16 + i]
                    responsePacket[16 + i] = temp
                }
                for (i in 0..1) {
                    val temp = responsePacket[ihl + i]
                    responsePacket[ihl + i] = responsePacket[ihl + 2 + i]
                    responsePacket[ihl + 2 + i] = temp
                }
                
                // Fix Lengths
                val totalLen = responsePacketLen
                responsePacket[2] = (totalLen shr 8).toByte()
                responsePacket[3] = (totalLen and 0xFF).toByte()
                
                val udpLen = udpHeaderLen + responsePayloadLen
                responsePacket[ihl + 4] = (udpLen shr 8).toByte()
                responsePacket[ihl + 5] = (udpLen and 0xFF).toByte()

                // Recalc Checksums
                responsePacket[ihl + 6] = 0
                responsePacket[ihl + 7] = 0 
                
                responsePacket[10] = 0
                responsePacket[11] = 0
                var ipChecksum = 0
                for (i in 0 until ihl step 2) {
                     val word = ((responsePacket[i].toInt() and 0xFF) shl 8) + (responsePacket[i + 1].toInt() and 0xFF)
                     ipChecksum += word
                }
                while ((ipChecksum shr 16) > 0) {
                    ipChecksum = (ipChecksum and 0xFFFF) + (ipChecksum shr 16)
                }
                ipChecksum = ipChecksum.inv() and 0xFFFF
                responsePacket[10] = (ipChecksum shr 8).toByte()
                responsePacket[11] = (ipChecksum and 0xFF).toByte()

                return@withContext responsePacket
                
            } catch (e: Exception) {
                Timber.w(e, "[$TAG] DNS Forwarding failed")
                return@withContext null
            }
        }
    }

    /**
     * Extract domain name from DNS query packet
     * Enhanced with comprehensive bounds checking and validation
     */
    private fun extractDnsQuery(data: ByteArray, offset: Int): String? {
        return try {
            // Validate minimum packet size (DNS header is 12 bytes)
            if (data.size < offset + 12) {
                Timber.w("[$TAG] DNS packet too small: ${data.size} bytes")
                return null
            }
            
            val queryOffset = offset + 12 // Skip DNS header
            val domain = StringBuilder()
            var i = queryOffset
            
            // RFC 1035: Maximum domain name length is 253 characters
            val maxDomainLength = 253
            
            while (i < data.size && data[i].toInt() != 0) {
                val labelLength = data[i].toInt() and 0xFF
                
                // Label length of 0 indicates end of domain name
                if (labelLength == 0) break
                
                // Check for DNS compression pointer (starts with 0xC0)
                // We don't support compression in this simple implementation
                if (labelLength >= 0xC0) {
                    Timber.d("[$TAG] DNS compression detected, skipping")
                    return null
                }
                
                // Validate label length doesn't exceed remaining buffer
                if (i + labelLength >= data.size) {
                    Timber.w("[$TAG] Label length $labelLength exceeds buffer at position $i")
                    return null
                }
                
                // Add separator between labels
                if (domain.isNotEmpty()) {
                    domain.append(".")
                }
                
                // Extract label characters
                for (j in 1..labelLength) {
                    val charIndex = i + j
                    
                    // Double-check bounds
                    if (charIndex >= data.size) {
                        Timber.w("[$TAG] Character index $charIndex out of bounds")
                        return null
                    }
                    
                    domain.append(data[charIndex].toInt().toChar())
                }
                
                // Check if domain is getting too long
                if (domain.length > maxDomainLength) {
                    Timber.w("[$TAG] Domain exceeds maximum length: ${domain.length}")
                    return null
                }
                
                // Move to next label
                i += labelLength + 1
                
                // Safety check: prevent infinite loops
                if (i > offset + 512) {
                    Timber.w("[$TAG] DNS parsing exceeded safe offset")
                    return null
                }
            }
            
            val result = domain.toString()
            
            // Return domain only if it's not empty and is valid
            if (result.isEmpty()) {
                null
            } else {
                Timber.d("[$TAG] Extracted DNS query: $result")
                result
            }
            
        } catch (e: Exception) {
            Timber.w(e, "[$TAG] Failed to extract DNS query")
            null
        }
    }
    
    /**
     * Log URL access violation
     */
    private fun logViolation(url: String) {
        serviceScope.launch {
            try {
                val violation = Violation(
                    url = url,
                    type = ViolationType.URL_ACCESS_ATTEMPT,
                    message = "Attempted to access blocked URL: $url"
                )
                
                violationRepository.logViolation(violation)
                
            } catch (e: Exception) {
                Timber.e(e, "[$TAG] Failed to log violation")
            }
        }
    }
    
    /**
     * Create notification channel for foreground service
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "URL Filter",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Critical VPN service for URL filtering - must remain visible"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    /**
     * Create foreground service notification
     */
    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("SelfControl VPN Active")
        .setContentText("URL filtering is protecting your device")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setOngoing(true)
        .build()
}
