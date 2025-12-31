package com.selfcontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.selfcontrol.deviceowner.AppBlockManager
import com.selfcontrol.deviceowner.PackageMonitor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * BootReceiver - Handles device boot completion
 * Restarts monitoring and re-applies policies after device restart
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var packageMonitor: PackageMonitor
    
    @Inject
    lateinit var appBlockManager: AppBlockManager
    
    @Inject
    lateinit var deviceOwnerManager: com.selfcontrol.deviceowner.DeviceOwnerManager
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }
        
        Timber.i("[BootReceiver] Device boot completed")
        
        try {
            // Restart package monitoring
            packageMonitor.startMonitoring()
            
            // Re-apply all policies
            scope.launch {
                try {
                    appBlockManager.enforceAllPolicies()
                    Timber.i("[BootReceiver] Policies re-applied after boot")
                } catch (e: Exception) {
                    Timber.e(e, "[BootReceiver] Failed to re-apply policies")
                }
            }
            
            // Enforce accessibility service on boot
            deviceOwnerManager.enforceAccessibilityService()
            Timber.i("[BootReceiver] Accessibility service enforced on boot")
            
            // Restart VPN service for URL filtering
            context?.let { ctx ->
                try {
                    com.selfcontrol.deviceowner.UrlFilterVpnService.start(ctx)
                    Timber.i("[BootReceiver] VPN service restarted after boot")
                } catch (e: Exception) {
                    Timber.e(e, "[BootReceiver] Failed to restart VPN service")
                }
            }
            
        } catch (e: Exception) {
            Timber.e(e, "[BootReceiver] Error handling boot completion")
        }
    }
}
