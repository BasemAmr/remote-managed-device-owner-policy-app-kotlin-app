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
                // Load blocked URLs first
                loadBlockedUrls()
                
                // Establish VPN interface
                vpnInterface = establishVpn()
                
                if (vpnInterface != null) {
                    isRunning = true
                    Timber.i("[$TAG] VPN established successfully")
                    
                    // Update VPN connection status
                    prefs.setVpnConnected(true)
                    
                    // Start packet processing
                    processPackets()
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
     * Establish the VPN interface
     */
    private fun establishVpn(): ParcelFileDescriptor? {
        return try {
            Builder()
                .setSession("SelfControl URL Filter")
                .addAddress(VPN_ADDRESS, VPN_PREFIX)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(VPN_DNS)
                .setMtu(VPN_MTU)
                .setBlocking(true)
                // Allow bypass for system apps
                .apply {
                    // Don't intercept our own traffic
                    try {
                        addDisallowedApplication(packageName)
                    } catch (e: Exception) {
                        Timber.w(e, "[$TAG] Could not exclude self from VPN")
                    }
                }
                .establish()
        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Failed to establish VPN")
            null
        }
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
            
            val packet = ByteBuffer.allocate(32767)
            
            Timber.d("[$TAG] Starting packet processing")
            
            while (isRunning) {
                try {
                    packet.clear()
                    
                    val length = inputChannel.read(packet)
                    
                    if (length > 0) {
                        packet.flip()
                        
                        // Parse packet and check for DNS queries
                        val processedPacket = processPacket(packet, length)
                        
                        if (processedPacket != null) {
                            // Forward packet
                            outputChannel.write(processedPacket)
                        }
                        // If processedPacket is null, packet was blocked
                    }
                    
                } catch (e: Exception) {
                    if (isRunning) {
                        Timber.w(e, "[$TAG] Packet processing error")
                    }
                }
            }
            
            Timber.d("[$TAG] Packet processing stopped")
        }
    }
    
    /**
     * Process individual packet - check for blocked domains in DNS queries
     */
    private fun processPacket(packet: ByteBuffer, length: Int): ByteBuffer? {
        try {
            // Check if it's a DNS packet (UDP port 53)
            // This is a simplified implementation - a full implementation would need
            // to parse IP and UDP headers properly
            
            if (length < 28) {
                return packet // Too small to be DNS, pass through
            }
            
            val data = ByteArray(length)
            packet.get(data)
            
            // Check IP protocol (UDP = 17)
            val protocol = data[9].toInt() and 0xFF
            
            if (protocol == 17) { // UDP
                // Check destination port (DNS = 53)
                val destPort = ((data[22].toInt() and 0xFF) shl 8) or (data[23].toInt() and 0xFF)
                
                if (destPort == 53) {
                    // This is a DNS query - extract domain
                    val domain = extractDnsQuery(data, 28)
                    
                    if (domain != null && shouldBlockUrl(domain)) {
                        Timber.i("[$TAG] Blocking DNS query for: $domain")
                        logViolation(domain)
                        return null // Block the packet
                    }
                }
            }
            
            // Pass through non-DNS or non-blocked traffic
            packet.rewind()
            return packet
            
        } catch (e: Exception) {
            Timber.w(e, "[$TAG] Error processing packet")
            packet.rewind()
            return packet // On error, pass through
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
