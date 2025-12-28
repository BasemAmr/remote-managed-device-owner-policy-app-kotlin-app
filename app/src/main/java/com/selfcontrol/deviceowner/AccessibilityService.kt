package com.selfcontrol.deviceowner

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.selfcontrol.deviceowner.AppBlockManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * AccessibilityMonitor - Monitors foreground app changes & prevents tampering
 * 
 * Dual Purpose:
 * 1. BLOCK USAGE: Detects when user tries to open blocked apps
 * 2. ANTI-TAMPER: Detects when user tries to access Settings to kill locked apps
 */
@AndroidEntryPoint
class AccessibilityMonitor : AccessibilityService() {
    
    @Inject
    lateinit var appBlockManager: AppBlockManager
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastPackageName: String? = null
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.i("[AccessibilityMonitor] 🔐 Service connected - Anti-Tamper Active")
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
    
    override fun onDestroy() {
        super.onDestroy()
        Timber.i("[AccessibilityMonitor] Service destroyed")
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
}
