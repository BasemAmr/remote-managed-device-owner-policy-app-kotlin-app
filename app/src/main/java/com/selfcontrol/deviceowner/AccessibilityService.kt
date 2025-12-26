package com.selfcontrol.deviceowner

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.selfcontrol.deviceowner.AppBlockManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * AccessibilityMonitor - Monitors foreground app changes
 * Used to detect when user tries to open blocked apps
 */
@AndroidEntryPoint
class AccessibilityMonitor : AccessibilityService() {
    
    @Inject
    lateinit var appBlockManager: AppBlockManager
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastPackageName: String? = null
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.i("[AccessibilityMonitor] Service connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // Only handle window state changes (app switches)
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        
        val packageName = event.packageName?.toString() ?: return
        
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
                    Timber.w("[AccessibilityMonitor] Blocked app detected: $packageName")
                    handleBlockedApp(packageName)
                }
                
            } catch (e: Exception) {
                Timber.e(e, "[AccessibilityMonitor] Error checking app: $packageName")
            }
        }
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
            
            Timber.i("[AccessibilityMonitor] Returned to home for blocked app: $packageName")
            
        } catch (e: Exception) {
            Timber.e(e, "[AccessibilityMonitor] Failed to handle blocked app")
        }
    }
}
