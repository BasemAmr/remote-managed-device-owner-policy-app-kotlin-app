package com.selfcontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.selfcontrol.data.worker.AppSyncWorker
import com.selfcontrol.deviceowner.AppBlockManager
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
 */
@AndroidEntryPoint
class PackageChangeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appBlockManager: AppBlockManager

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
                handlePackageUpdated(packageName)
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
                if (appBlockManager.isAppBlocked(packageName)) {
                    appBlockManager.blockApp(packageName)
                    Timber.i("[PackageChangeReceiver] Blocked newly installed app: $packageName")
                }
                // Trigger app sync after installation
                triggerAppSync(context)
            } catch (e: Exception) {
                Timber.e(e, "[PackageChangeReceiver] Error handling package added: $packageName")
            }
        }
    }

    private fun handlePackageUpdated(packageName: String) {
        scope.launch {
            try {
                if (appBlockManager.isAppBlocked(packageName)) {
                    appBlockManager.blockApp(packageName)
                    Timber.i("[PackageChangeReceiver] Re-applied block for updated app: $packageName")
                }
            } catch (e: Exception) {
                Timber.e(e, "[PackageChangeReceiver] Error handling package updated: $packageName")
            }
        }
    }

    private fun handlePackageRemoved(context: Context?, packageName: String) {
        Timber.i("[PackageChangeReceiver] Package removed: $packageName")
        // Trigger app sync after removal
        triggerAppSync(context)
        // Typically no action needed, but could clean up local state if necessary
    }
    
    private fun triggerAppSync(context: Context?) {
        if (context == null) {
            Timber.e("[PackageChangeReceiver] Context is null, cannot trigger app sync")
            return
        }
        try {
            val workManager = WorkManager.getInstance(context)
            val appSyncWork = OneTimeWorkRequestBuilder<AppSyncWorker>()
                .addTag("one_time_app_sync")
                .build()
            
            workManager.enqueueUniqueWork(
                "one_time_app_sync_${System.currentTimeMillis()}",
                ExistingWorkPolicy.REPLACE,
                appSyncWork
            )
            
            Timber.d("[PackageChangeReceiver] Triggered one-time app sync")
        } catch (e: Exception) {
            Timber.e(e, "[PackageChangeReceiver] Failed to trigger app sync")
        }
    }
}
