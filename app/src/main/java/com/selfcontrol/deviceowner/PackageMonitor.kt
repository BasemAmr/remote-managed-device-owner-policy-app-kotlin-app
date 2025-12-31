package com.selfcontrol.deviceowner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.selfcontrol.data.worker.AppSyncWorker
import com.selfcontrol.domain.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
    private val appBlockManager: AppBlockManager,
    private val appRepository: AppRepository
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
                // Step 1: Refresh local database to include the new app
                appRepository.refreshInstalledApps()
                
                // Step 2: Check if there's a policy for this app
                val isBlocked = appBlockManager.isAppBlocked(packageName)
                
                if (isBlocked) {
                    // Immediately block the newly installed app
                    appBlockManager.blockApp(packageName)
                    Timber.i("[PackageMonitor] Blocked newly installed app: $packageName")
                }
                
                // Step 3: Trigger backend sync
                triggerAppSync()
                
            } catch (e: Exception) {
                Timber.e(e, "[PackageMonitor] Failed to handle package install: $packageName")
            }
        }
    }
    
    /**
     * Handle when a package is uninstalled
     */
    private fun handlePackageUninstalled(packageName: String) {
        Timber.i("[PackageMonitor] Package uninstalled event: $packageName")
        
        scope.launch {
            try {
                // IMPORTANT: setApplicationHidden triggers ACTION_PACKAGE_REMOVED.
                // We must verify if the app is truly uninstalled or just hidden.
                val isTrulyUninstalled = try {
                    context.packageManager.getPackageInfo(
                        packageName, 
                        android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
                    )
                    false // Still exists in some form (likely hidden)
                } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                    true // Truly gone
                }

                if (isTrulyUninstalled) {
                    Timber.i("[PackageMonitor] Package $packageName is truly uninstalled. Deleting.")
                    // Step 1: Remove from local database
                    appRepository.deleteApp(packageName)
                    
                    // Step 2: Trigger backend sync (backend cleanup will remove it)
                    triggerAppSync()
                } else {
                    Timber.i("[PackageMonitor] Package $packageName was hidden/blocked, not uninstalled. Keeping in list.")
                    // Trigger sync anyway to update state, but don't delete locally
                    triggerAppSync()
                }
            } catch (e: Exception) {
                Timber.e(e, "[PackageMonitor] Failed to handle package uninstall: $packageName")
            }
        }
    }

    /**
     * Trigger background app sync
     */
    private fun triggerAppSync() {
        val workManager = WorkManager.getInstance(context)
        
        // Give local DB a moment to finalize
        val inputData = Data.Builder()
            .putBoolean(AppSyncWorker.KEY_IS_MANUAL_SYNC, false)
            .putBoolean(AppSyncWorker.KEY_IS_IMMEDIATE_SYNC, true)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<AppSyncWorker>()
            .setInputData(inputData)
            .addTag("auto_package_sync")
            .build()

        workManager.enqueueUniqueWork(
            "auto_package_sync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
        
        Timber.d("[PackageMonitor] 🚀 Triggered automatic app sync")
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
