package com.selfcontrol

import android.app.Application
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.selfcontrol.data.repository.DeviceSetupRepository
import com.selfcontrol.data.worker.AppSyncWorker
import com.selfcontrol.data.worker.HeartbeatWorker
import com.selfcontrol.data.worker.PolicySyncWorker
import com.selfcontrol.data.worker.RequestCheckWorker
import com.selfcontrol.data.worker.UrlBlacklistSyncWorker
import com.selfcontrol.data.worker.ViolationUploadWorker
import com.selfcontrol.deviceowner.DeviceOwnerManager
import com.selfcontrol.deviceowner.PackageMonitor
import com.selfcontrol.util.Constants

import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Main Application class for Self-Control MDM app
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection
 */
@HiltAndroidApp
class SelfControlApp : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory
    
    @Inject
    lateinit var packageMonitor: PackageMonitor
    
    // Inject the new components
    @Inject
    lateinit var deviceOwnerManager: DeviceOwnerManager
    
    @Inject
    lateinit var deviceSetupRepository: DeviceSetupRepository
    
    private val appScope = CoroutineScope(Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber logging
        initializeLogging()
        
        Timber.i("--- APP STARTUP ---")
        
        // 1. Initialize Device Owner (Locks policies & Updates UI)
        deviceOwnerManager.initialize()
        
        // 2. Start monitoring package changes
        packageMonitor.startMonitoring()
        
        // 3. Register & Scan Apps (Background)
        appScope.launch {
            try {
                deviceSetupRepository.performStartupChecks()
                
                // 4. Start VPN Service for URL Blocking
                // This creates a local VPN that intercepts DNS requests
                Timber.i("[Startup] Starting URL Filter VPN Service...")
                com.selfcontrol.deviceowner.UrlFilterVpnService.start(this@SelfControlApp)
            } catch (e: Exception) {
                Timber.e(e, "[Startup] Initialization failed")
            }
        }
        
        // 5. Schedule all background workers
        scheduleBackgroundWorkers()
    }
    
    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            // Debug tree with line numbers for development
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    return "${super.createStackElementTag(element)}:${element.lineNumber}"
                }
            })
            Timber.d("Timber: Debug logging enabled")
        } else {
            // Production logging with Crashlytics
            // Timber.plant(CrashReportingTree())
            Timber.i("Timber: Production logging enabled")
        }
    }
    
    /**
     * Schedule all periodic background workers
     */
    private fun scheduleBackgroundWorkers() {
        Timber.i("SelfControlApp: Scheduling background workers")
        
        val workManager = WorkManager.getInstance(this)
        
        // Network constraint - most workers need network
        val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // 1. Policy Sync Worker - Every 15 minutes
        val policySyncWork = PeriodicWorkRequestBuilder<PolicySyncWorker>(
            Constants.POLICY_SYNC_INTERVAL, TimeUnit.MINUTES
        )
            .setConstraints(networkConstraint)
            .addTag(Constants.WORK_TAG_POLICY_SYNC)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            PolicySyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            policySyncWork
        )
        
        // 2. Request Check Worker - Every 15 minutes
        val requestCheckWork = PeriodicWorkRequestBuilder<RequestCheckWorker>(
            Constants.REQUEST_CHECK_INTERVAL, TimeUnit.MINUTES
        )
            .setConstraints(networkConstraint)
            .addTag(Constants.WORK_TAG_REQUEST_CHECK)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            RequestCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            requestCheckWork
        )
        
        // 3. Violation Upload Worker - Every 60 minutes
        val violationUploadWork = PeriodicWorkRequestBuilder<ViolationUploadWorker>(
            Constants.VIOLATION_UPLOAD_INTERVAL, TimeUnit.MINUTES
        )
            .setConstraints(networkConstraint)
            .addTag(Constants.WORK_TAG_VIOLATION_UPLOAD)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            ViolationUploadWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            violationUploadWork
        )
        
        // 4. Heartbeat Worker - Every 30 minutes
        val heartbeatWork = PeriodicWorkRequestBuilder<HeartbeatWorker>(
            Constants.HEARTBEAT_INTERVAL, TimeUnit.MINUTES
        )
            .setConstraints(networkConstraint)
            .addTag(Constants.WORK_TAG_HEARTBEAT)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            HeartbeatWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            heartbeatWork
        )
        
        // 5. URL Blacklist Sync Worker - Every 60 minutes
        val urlSyncWork = PeriodicWorkRequestBuilder<UrlBlacklistSyncWorker>(
            Constants.URL_SYNC_INTERVAL, TimeUnit.MINUTES
        )
            .setConstraints(networkConstraint)
            .addTag(Constants.WORK_TAG_URL_SYNC)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            UrlBlacklistSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            urlSyncWork
        )
        
        // 6. App Sync Worker - Every 60 minutes
        val appSyncWork = PeriodicWorkRequestBuilder<AppSyncWorker>(
            Constants.APP_SYNC_INTERVAL, TimeUnit.MINUTES
        )
            .setConstraints(networkConstraint)
            .addTag(Constants.WORK_TAG_APP_SYNC)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            AppSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            appSyncWork
        )
        
        Timber.i("SelfControlApp: All background workers scheduled")
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO)
            .build()
}

