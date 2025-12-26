package com.selfcontrol.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.selfcontrol.domain.repository.ViolationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * ViolationUploadWorker - Periodic worker that uploads unsynced violations to server
 * Runs every hour to batch upload violation logs
 */
@HiltWorker
class ViolationUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val violationRepository: ViolationRepository
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "violation_upload_worker"
        const val TAG = "ViolationUploadWorker"
        
        // Clean up violations older than 30 days
        private const val CLEANUP_DAYS = 30
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Timber.i("[$TAG] Starting violation upload")
        val startTime = System.currentTimeMillis()
        
        return@withContext try {
            // Step 1: Get unsynced violations
            val unsyncedViolations = violationRepository.getUnsyncedViolations()
            
            if (unsyncedViolations.isEmpty()) {
                Timber.d("[$TAG] No violations to sync")
                
                // Still perform cleanup of old violations
                cleanupOldViolations()
                
                return@withContext Result.success()
            }
            
            Timber.d("[$TAG] Uploading ${unsyncedViolations.size} violations")
            
            // Step 2: Sync violations to server
            violationRepository.syncViolationsToServer()
            
            // Step 3: Cleanup old synced violations
            cleanupOldViolations()
            
            val duration = System.currentTimeMillis() - startTime
            Timber.i("[$TAG] Completed in ${duration}ms. Uploaded: ${unsyncedViolations.size}")
            
            Result.success()
            
        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Violation upload failed")
            
            if (runAttemptCount < 3) {
                Timber.i("[$TAG] Retrying... Attempt ${runAttemptCount + 1}/3")
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    /**
     * Cleanup old synced violations to save storage
     */
    private suspend fun cleanupOldViolations() {
        try {
            val cutoffTime = System.currentTimeMillis() - (CLEANUP_DAYS * 24 * 60 * 60 * 1000L)
            violationRepository.deleteOldSyncedViolations(cutoffTime)
            Timber.d("[$TAG] Cleaned up old violations")
        } catch (e: Exception) {
            Timber.w(e, "[$TAG] Failed to cleanup old violations")
        }
    }
}
