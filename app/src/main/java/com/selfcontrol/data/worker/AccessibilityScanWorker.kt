package com.selfcontrol.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.selfcontrol.domain.repository.AccessibilityRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class AccessibilityScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val accessibilityRepository: AccessibilityRepository
) : CoroutineWorker(appContext, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            Timber.i("[AccessibilityScanWorker] Starting scan")
            
            accessibilityRepository.scanAndSyncServices()
            accessibilityRepository.syncLockedServicesFromBackend()
            
            Timber.i("[AccessibilityScanWorker] Scan completed")
            Result.success()
            
        } catch (e: Exception) {
            Timber.e(e, "[AccessibilityScanWorker] Scan failed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
    
    companion object {
        const val WORK_NAME = "accessibility_scan_worker"
    }
}
