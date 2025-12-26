package com.selfcontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                    handlePackageAdded(packageName)
                }
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                handlePackageUpdated(packageName)
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (!replacing) {
                    handlePackageRemoved(packageName)
                }
            }
        }
    }

    private fun handlePackageAdded(packageName: String) {
        scope.launch {
            try {
                if (appBlockManager.isAppBlocked(packageName)) {
                    appBlockManager.blockApp(packageName)
                    Timber.i("[PackageChangeReceiver] Blocked newly installed app: $packageName")
                }
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

    private fun handlePackageRemoved(packageName: String) {
        Timber.i("[PackageChangeReceiver] Package removed: $packageName")
        // Typically no action needed, but could clean up local state if necessary
    }
}
