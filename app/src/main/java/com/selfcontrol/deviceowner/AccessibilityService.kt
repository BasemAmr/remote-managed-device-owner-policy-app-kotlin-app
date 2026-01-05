package com.selfcontrol.deviceowner

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.selfcontrol.deviceowner.AppBlockManager
import com.selfcontrol.domain.repository.UrlRepository
import com.selfcontrol.domain.repository.AccessibilityRepository
import com.selfcontrol.domain.model.UrlBlacklist
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * AccessibilityMonitor - Monitors foreground app changes & prevents tampering
 * 
 * Dual Purpose:
 * 1. BLOCK USAGE: Detects when user tries to open blocked apps
 * 2. ANTI-TAMPER: Detects when user tries to access Settings to kill locked apps
 * 3. SERVICE MONITOR: Continuously monitors accessibility services status
 */
@AndroidEntryPoint
class AccessibilityMonitor : AccessibilityService() {
    
    @Inject
    lateinit var appBlockManager: AppBlockManager
    
    @Inject
    lateinit var urlRepository: UrlRepository
    
    @Inject
    lateinit var accessibilityRepository: AccessibilityRepository
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastPackageName: String? = null
    private var blockedPatterns: List<UrlBlacklist> = emptyList()
    
    // Handler for periodic accessibility service checks
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 30000L // Check every 30 seconds (optimized for battery)
    private var isMonitoringPaused = false // Pause when enforcement screen is active
    
    // Broadcast receiver for pause/resume commands
    private val monitoringReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.selfcontrol.PAUSE_MONITORING" -> {
                    isMonitoringPaused = true
                    Timber.i("[AccessibilityMonitor] Monitoring PAUSED by enforcement")
                }
                "com.selfcontrol.RESUME_MONITORING" -> {
                    isMonitoringPaused = false
                    Timber.i("[AccessibilityMonitor] Monitoring RESUMED")
                }
            }
        }
    }
    
    // Known browser packages (for quick lookup)
    private val knownBrowsers = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "org.mozilla.fenix",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.opera.browser.beta",
        "com.opera.mini.native",
        "com.brave.browser",
        "com.sec.android.app.sbrowser",
        "com.android.browser",
        "com.kiwibrowser.browser",
        "com.vivaldi.browser",
        "com.dolphin.browser",
        "com.yandex.browser",
        "com.ecosia.android",
        "org.mozilla.focus",
        "org.mozilla.firefox_beta",
        "com.duckduckgo.mobile.android",
        "org.torproject.torbrowser",
        // WebView apps
        "org.chromium.webview_shell",
        "com.google.android.webview"
    )
    
    // Cache for dynamic browser detection (avoid repeated checks)
    private val browserCache = mutableMapOf<String, Boolean>()
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.i("[AccessibilityMonitor] 🔐 Service connected - Anti-Tamper Active")
        
        // Register broadcast receiver
        val filter = IntentFilter().apply {
            addAction("com.selfcontrol.PAUSE_MONITORING")
            addAction("com.selfcontrol.RESUME_MONITORING")
        }
        registerReceiver(monitoringReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        
        // Load blocked URL patterns
        scope.launch {
            loadBlockedUrls()
        }
        
        // Start periodic accessibility service monitoring
        startAccessibilityServiceMonitoring()
    }
    
    private suspend fun loadBlockedUrls() {
        try {
            blockedPatterns = urlRepository.observeBlockedUrls().first()
            Timber.i("[AccessibilityMonitor] Loaded ${blockedPatterns.size} blocked URL patterns")
        } catch (e: Exception) {
            Timber.e(e, "[AccessibilityMonitor] Failed to load blocked URLs")
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        val packageName = event.packageName?.toString() ?: return
        
        // 1. BLOCK USAGE: User tries to open a blocked app (Existing Logic)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            checkAppUsage(packageName)
        }

        // 2. ANTI-TAMPER: User tries to open Settings to kill a locked app
        if (packageName == "com.android.settings") {
            checkForTampering(event)
        }
        
        // 3. URL BLOCKING: Check browser URL bar for blocked URLs
        if (isBrowserOrWebView(packageName)) {
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_FOCUSED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    checkBrowserUrl()
                }
            }
        }
    }
    
    /**
     * Check if the user is trying to open a blocked app
     */
    private fun checkAppUsage(packageName: String) {
        // Ignore if same as last package (avoid duplicate checks)
        if (packageName == lastPackageName) {
            return
        }
        
        lastPackageName = packageName
        
        // Check if app is allowed
        scope.launch {
            try {
                val isAllowed = appBlockManager.isAppAllowed(packageName)
                
                if (!isAllowed) {
                    Timber.w("[AccessibilityMonitor] ⚠️ Blocked app detected: $packageName")
                    handleBlockedApp(packageName)
                }
                
            } catch (e: Exception) {
                Timber.e(e, "[AccessibilityMonitor] Error checking app: $packageName")
            }
        }
    }
    
    /**
     * 🛡️ ANTI-TAMPER WATCHDOG
     * Scans the Settings screen text. 
     * If we see the name of a Locked App or SelfControl, we kick the user out.
     */
    private fun checkForTampering(event: AccessibilityEvent) {
        // Only check window state changes in Settings
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        
        val rootNode = rootInActiveWindow ?: return
        
        try {
            // We scan the screen for specific keywords combined with locked app names
            val textList = getAllText(rootNode)
            
            scope.launch {
                // Check if we are looking at "App Info" for "SelfControl" or other locked apps
                val isSelfControlDetailsOpen = textList.any { 
                    it.contains("SelfControl", ignoreCase = true) || 
                    it.contains("com.selfcontrol", ignoreCase = true) 
                }

                if (isSelfControlDetailsOpen) {
                    // If we are in Settings AND "SelfControl" is on screen...
                    // AND we see dangerous buttons like "Force stop" or "Uninstall"
                    val dangerousButtons = textList.any { 
                        it.equals("Force stop", ignoreCase = true) || 
                        it.equals("Uninstall", ignoreCase = true) ||
                        it.equals("Disable", ignoreCase = true) ||
                        it.contains("force", ignoreCase = true) ||
                        it.contains("uninstall", ignoreCase = true)
                    }

                    if (dangerousButtons) {
                        Timber.w("[AccessibilityMonitor] 🚨 TAMPERING DETECTED! User tried to access SelfControl settings. Blocking.")
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        return@launch
                    }
                }
                
                // TODO: To make this generic for ALL locked apps, iterate your locked list here
                // Example:
                // val lockedApps = policyRepository.getLockedApps()
                // if (textList.any { lockedApps.containsName(it) }) { ... }
            }
        } finally {
            rootNode.recycle() // Important for memory!
        }
    }

    /**
     * Recursively extract all text from the screen
     */
    private fun getAllText(node: AccessibilityNodeInfo): List<String> {
        val textList = mutableListOf<String>()
        
        if (node.text != null) {
            textList.add(node.text.toString())
        }
        if (node.contentDescription != null) {
            textList.add(node.contentDescription.toString())
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                textList.addAll(getAllText(child))
            } finally {
                child.recycle() // Important for memory!
            }
        }
        return textList
    }
    
    override fun onInterrupt() {
        Timber.w("[AccessibilityMonitor] Service interrupted")
    }
    
    /**
     * Handle when a blocked app is detected
     */
    private fun handleBlockedApp(packageName: String) {
        try {
            // Return to home screen
            performGlobalAction(GLOBAL_ACTION_HOME)
            
            // Show blocked screen notification
            // This will be handled by the BlockedScreen composable
            
            Timber.i("[AccessibilityMonitor] ✓ Returned to home for blocked app: $packageName")
            
        } catch (e: Exception) {
            Timber.e(e, "[AccessibilityMonitor] Failed to handle blocked app")
        }
    }
    
    /**
     * Hybrid browser detection: static list + dynamic detection
     */
    private fun isBrowserOrWebView(packageName: String): Boolean {
        // Quick check: known browsers
        if (knownBrowsers.contains(packageName)) {
            return true
        }
        
        // Check cache
        browserCache[packageName]?.let { return it }
        
        // Dynamic detection
        val isBrowser = detectBrowserDynamically(packageName)
        browserCache[packageName] = isBrowser
        
        if (isBrowser) {
            Timber.d("[AccessibilityMonitor] Detected browser/WebView: $packageName")
        }
        
        return isBrowser
    }
    
    /**
     * Detect if app is a browser by checking if it handles http/https intents
     */
    private fun detectBrowserDynamically(packageName: String): Boolean {
        try {
            val pm = packageManager
            
            // Check if app can handle http/https URLs
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))
            val resolveInfos = pm.queryIntentActivities(browserIntent, PackageManager.MATCH_ALL)
            
            val handlesBrowserIntent = resolveInfos.any { 
                it.activityInfo.packageName == packageName 
            }
            
            if (handlesBrowserIntent) {
                return true
            }
            
            // Check for WebView-related patterns in package name
            val webViewPatterns = listOf("webview", "browser", "chrome")
            if (webViewPatterns.any { packageName.contains(it, ignoreCase = true) }) {
                return true
            }
            
            return false
            
        } catch (e: Exception) {
            Timber.e(e, "[AccessibilityMonitor] Error detecting browser: $packageName")
            return false
        }
    }
    
    /**
     * Check browser URL bar for blocked URLs
     */
    private fun checkBrowserUrl() {
        val rootNode = rootInActiveWindow ?: return
        
        try {
            val url = extractUrlFromBrowser(rootNode)
            Timber.d("[AccessibilityMonitor] Extracted URL: $url")
            if (url != null && url.isNotBlank()) {
                scope.launch {
                    if (isUrlBlocked(url)) {
                        Timber.w("[AccessibilityMonitor] ⚠️ Blocked URL detected: $url")
                        handleBlockedUrl(url)
                    } else {
                        Timber.d("[AccessibilityMonitor] URL allowed: $url")
                    }
                }
            } else {
                Timber.d("[AccessibilityMonitor] No URL found in browser")
            }
        } finally {
            rootNode.recycle()
        }
    }
    
    /**
     * Extract URL from browser address bar
     */
    private fun extractUrlFromBrowser(node: AccessibilityNodeInfo): String? {
        // Look for URL bar by common resource IDs and hints
        val urlBarIds = listOf(
            "url_bar",
            "search_box",
            "address_bar",
            "omnibox",
            "location_bar"
        )
        
        return findUrlBar(node, urlBarIds)
    }
    
    private fun findUrlBar(node: AccessibilityNodeInfo, urlBarIds: List<String>): String? {
        // Check current node
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val hint = node.hintText?.toString()?.lowercase() ?: ""
        val text = node.text?.toString() ?: ""
        
        // Check if this is a URL bar by resource ID or hint
        val isUrlBar = urlBarIds.any { id ->
            viewId.contains(id) || hint.contains("url") || hint.contains("search") || hint.contains("address")
        }
        
        if (isUrlBar && text.isNotBlank() && looksLikeUrl(text)) {
            Timber.d("[AccessibilityMonitor] Found URL in URL bar: $text")
            return text
        }
        
        // FALLBACK: Check if this is an EditText with URL-like content
        if (node.className == "android.widget.EditText" && text.isNotBlank() && looksLikeUrl(text)) {
            Timber.d("[AccessibilityMonitor] Found URL in EditText: $text")
            return text
        }
        
        // FALLBACK 2: Check if ANY text looks like a URL
        if (text.isNotBlank() && looksLikeUrl(text)) {
            Timber.d("[AccessibilityMonitor] Found URL in text node: $text")
            return text
        }
        
        // Recursively check children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                val url = findUrlBar(child, urlBarIds)
                if (url != null) return url
            } finally {
                child.recycle()
            }
        }
        
        return null
    }
    
    /**
     * Check if text looks like a URL
     */
    private fun looksLikeUrl(text: String): Boolean {
        val lower = text.lowercase()
        return (lower.contains(".com") || 
                lower.contains(".org") || 
                lower.contains(".net") ||
                lower.contains("://") ||
                lower.matches(Regex(".*\\.[a-z]{2,}.*")))
    }
    
    /**
     * Check if URL matches any blocked pattern
     */
    private fun isUrlBlocked(url: String): Boolean {
        val cleanUrl = url.lowercase()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")
        
        return blockedPatterns.any { pattern ->
            pattern.matches(cleanUrl)
        }
    }
    
    /**
     * Handle when a blocked URL is detected
     */
    private fun handleBlockedUrl(url: String) {
        try {
            // Return to home screen
            performGlobalAction(GLOBAL_ACTION_HOME)
            
            Timber.i("[AccessibilityMonitor] ✓ Blocked URL access: $url")
            
        } catch (e: Exception) {
            Timber.e(e, "[AccessibilityMonitor] Failed to handle blocked URL")
        }
    }
    
    // ==================== ACCESSIBILITY SERVICE MONITORING ====================
    
    /**
     * Start periodic monitoring of accessibility services
     * Checks every 5 seconds if locked services are disabled
     */
    private fun startAccessibilityServiceMonitoring() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkAccessibilityServices()
                handler.postDelayed(this, checkInterval)
            }
        }, checkInterval)
        
        Timber.i("[AccessibilityMonitor] Started accessibility service monitoring (every ${checkInterval/1000}s)")
    }
    
    /**
     * Check if any locked accessibility services are disabled
     * If yes, trigger enforcement immediately
     */
    private fun checkAccessibilityServices() {
        // Don't check if monitoring is paused (enforcement screen is active)
        if (isMonitoringPaused) {
            Timber.d("[AccessibilityMonitor] Monitoring paused, skipping check")
            return
        }
        
        scope.launch {
            try {
                // Rescan services to get current status
                accessibilityRepository.scanAndSyncServices()
                
                val lockedServices = accessibilityRepository.getLockedServices().first()
                val disabledLockedServices = lockedServices.filter { !it.isEnabled }
                
                if (disabledLockedServices.isNotEmpty()) {
                    Timber.w("[AccessibilityMonitor] 🚨 ${disabledLockedServices.size} locked services disabled! Triggering enforcement...")
                    
                    // Pause monitoring while enforcement is active
                    isMonitoringPaused = true
                    
                    // Trigger enforcement screen immediately
                    val disabledServiceIds = disabledLockedServices.map { it.serviceId }.toTypedArray()
                    
                    val intent = Intent().apply {
                        setClassName(
                            applicationContext.packageName,
                            "com.selfcontrol.presentation.enforcement.EnforcementActivity"
                        )
                        putExtra("disabled_services", disabledServiceIds)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Timber.e(e, "[AccessibilityMonitor] Failed to check accessibility services")
            }
        }
    }
    
    /**
     * Stop monitoring when service is destroyed
     * CRITICAL: If this service is destroyed, it means accessibility was disabled!
     */
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        
        try {
            unregisterReceiver(monitoringReceiver)
        } catch (e: Exception) {
            Timber.e(e, "[AccessibilityMonitor] Error unregistering receiver")
        }
        
        Timber.w("[AccessibilityMonitor] 🚨 SERVICE DESTROYED - Accessibility was disabled!")
        
        // Trigger enforcement immediately via broadcast
        // This will be picked up by the app even if this service is killed
        try {
            val intent = Intent("com.selfcontrol.ACCESSIBILITY_DESTROYED")
            sendBroadcast(intent)
            Timber.i("[AccessibilityMonitor] Sent destruction broadcast")
        } catch (e: Exception) {
            Timber.e(e, "[AccessibilityMonitor] Failed to send destruction broadcast")
        }
    }
}
