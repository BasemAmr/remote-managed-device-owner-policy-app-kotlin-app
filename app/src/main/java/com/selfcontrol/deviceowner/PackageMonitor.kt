package com.selfcontrol.deviceowner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.selfcontrol.deviceowner.AppBlockManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PackageMonitor - Monitors app installations and uninstallations
 * Automatically applies policies to newly installed apps
 */
@Singleton
class PackageMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appBlockManager: AppBlockManager
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRegistered = false
    
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            
            val packageName = intent.data?.schemeSpecificPart ?: return
            
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        handlePackageInstalled(packageName)
                    }
                }
                
                Intent.ACTION_PACKAGE_REMOVED -> {
                    if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        handlePackageUninstalled(packageName)
                    }
                }
                
                Intent.ACTION_PACKAGE_REPLACED -> {
                    handlePackageUpdated(packageName)
                }
            }
        }
    }
    
    /**
     * Start monitoring package changes
     */
    fun startMonitoring() {
        if (isRegistered) {
            Timber.w("[PackageMonitor] Already monitoring")
            return
        }
        
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            
            context.registerReceiver(packageReceiver, filter)
            isRegistered = true
            
            Timber.i("[PackageMonitor] Started monitoring package changes")
            
        } catch (e: Exception) {
            Timber.e(e, "[PackageMonitor] Failed to start monitoring")
        }
    }
    
    /**
     * Stop monitoring package changes
     */
    fun stopMonitoring() {
        if (!isRegistered) {
            return
        }
        
        try {
            context.unregisterReceiver(packageReceiver)
            isRegistered = false
            
            Timber.i("[PackageMonitor] Stopped monitoring package changes")
            
        } catch (e: Exception) {
            Timber.e(e, "[PackageMonitor] Failed to stop monitoring")
        }
    }
    
    /**
     * Handle when a new package is installed
     */
    private fun handlePackageInstalled(packageName: String) {
        Timber.i("[PackageMonitor] Package installed: $packageName")
        
        scope.launch {
            try {
                // Check if there's a policy for this app
                val isBlocked = appBlockManager.isAppBlocked(packageName)
                
                if (isBlocked) {
                    // Immediately block the newly installed app
                    appBlockManager.blockApp(packageName)
                    Timber.i("[PackageMonitor] Blocked newly installed app: $packageName")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "[PackageMonitor] Failed to handle package install: $packageName")
            }
        }
    }
    
    /**
     * Handle when a package is uninstalled
     */
    private fun handlePackageUninstalled(packageName: String) {
        Timber.i("[PackageMonitor] Package uninstalled: $packageName")
        
        // No action needed for uninstall
        // Policy will remain in database for if app is reinstalled
    }
    
    /**
     * Handle when a package is updated
     */
    private fun handlePackageUpdated(packageName: String) {
        Timber.i("[PackageMonitor] Package updated: $packageName")
        
        scope.launch {
            try {
                // Re-apply policy after update (in case update reset permissions)
                val isBlocked = appBlockManager.isAppBlocked(packageName)
                
                if (isBlocked) {
                    appBlockManager.blockApp(packageName)
                    Timber.i("[PackageMonitor] Re-applied block after update: $packageName")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "[PackageMonitor] Failed to handle package update: $packageName")
            }
        }
    }
}
