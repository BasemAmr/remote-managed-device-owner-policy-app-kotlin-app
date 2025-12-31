package com.selfcontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.selfcontrol.data.worker.AppSyncWorker
import com.selfcontrol.deviceowner.AppBlockManager
import com.selfcontrol.domain.repository.AppRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * PackageChangeReceiver - Handles package installation, removal, and updates
 * This static receiver ensures we catch package changes even if the app process isn't active.
 * 
 * When a package is installed/uninstalled:
 * 1. Mark the app for immediate sync
 * 2. Trigger AppSyncWorker immediately
 * 3. Worker will sync all apps with retry logic
 */
@AndroidEntryPoint
class PackageChangeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appBlockManager: AppBlockManager
    
    @Inject
    lateinit var appRepository: AppRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return

        val packageName = intent.data?.schemeSpecificPart ?: return
        val action = intent.action ?: return

        Timber.d("[PackageChangeReceiver] Received action: $action for package: $packageName")

        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (!replacing) {
                    handlePackageAdded(context, packageName)
                }
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                handlePackageUpdated(context, packageName)
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (!replacing) {
                    handlePackageRemoved(context, packageName)
                }
            }
        }
    }

    private fun handlePackageAdded(context: Context?, packageName: String) {
        scope.launch {
            try {
                // Check and apply block policy if needed
                if (appBlockManager.isAppBlocked(packageName)) {
                    appBlockManager.blockApp(packageName)
                    Timber.i("[PackageChangeReceiver] Blocked newly installed app: $packageName")
                }
                
                // Mark this app for immediate sync
                appRepository.markAppForImmediateSync(packageName)
                Timber.d("[PackageChangeReceiver] Marked $packageName for immediate sync")
                
                // Trigger immediate app sync
                triggerImmediateAppSync(context, packageName, isInstall = true)
                
            } catch (e: Exception) {
                Timber.e(e, "[PackageChangeReceiver] Error handling package added: $packageName")
            }
        }
    }

    private fun handlePackageUpdated(context: Context?, packageName: String) {
        scope.launch {
            try {
                if (appBlockManager.isAppBlocked(packageName)) {
                    appBlockManager.blockApp(packageName)
                    Timber.i("[PackageChangeReceiver] Re-applied block for updated app: $packageName")
                }
                
                // Mark for sync on update as well
                appRepository.markAppForImmediateSync(packageName)
                triggerImmediateAppSync(context, packageName, isInstall = false)
                
            } catch (e: Exception) {
                Timber.e(e, "[PackageChangeReceiver] Error handling package updated: $packageName")
            }
        }
    }

    private fun handlePackageRemoved(context: Context?, packageName: String) {
        scope.launch {
            try {
                Timber.i("[PackageChangeReceiver] Package removed: $packageName")
                
                // Delete from local database
                appRepository.deleteApp(packageName)
                Timber.d("[PackageChangeReceiver] Deleted $packageName from local database")
                
                // Trigger sync to update server about the removal
                triggerImmediateAppSync(context, packageName, isInstall = false)
                
            } catch (e: Exception) {
                Timber.e(e, "[PackageChangeReceiver] Error handling package removed: $packageName")
            }
        }
    }
    
    /**
     * Triggers an immediate one-time app sync worker
     */
    private fun triggerImmediateAppSync(context: Context?, packageName: String, isInstall: Boolean) {
        if (context == null) {
            Timber.e("[PackageChangeReceiver] Context is null, cannot trigger app sync")
            return
        }
        
        try {
            val workManager = WorkManager.getInstance(context)
            
            // Create input data for the worker
            val inputData = Data.Builder()
                .putBoolean(AppSyncWorker.KEY_IS_IMMEDIATE_SYNC, true)
                .putBoolean(AppSyncWorker.KEY_IS_MANUAL_SYNC, false)
                .build()
            
            val appSyncWork = OneTimeWorkRequestBuilder<AppSyncWorker>()
                .setInputData(inputData)
                .addTag("immediate_app_sync")
                .build()
            
            // Use REPLACE policy to avoid duplicate work
            workManager.enqueueUniqueWork(
                "immediate_app_sync",
                ExistingWorkPolicy.REPLACE,
                appSyncWork
            )
            
            val action = if (isInstall) "install" else "change"
            Timber.i("[PackageChangeReceiver] ⚡ Triggered immediate app sync after $action of $packageName")
            
        } catch (e: Exception) {
            Timber.e(e, "[PackageChangeReceiver] Failed to trigger immediate app sync")
        }
    }
}

