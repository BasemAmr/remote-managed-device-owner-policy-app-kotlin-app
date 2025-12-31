package com.selfcontrol.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.selfcontrol.data.remote.api.SelfControlApi
import com.selfcontrol.data.remote.dto.AppUploadDto
import com.selfcontrol.data.remote.dto.AppUploadRequest
import com.selfcontrol.domain.model.Result as DomainResult
import com.selfcontrol.domain.model.SyncStatus
import com.selfcontrol.domain.repository.AppRepository
import com.selfcontrol.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * AppSyncWorker - Worker that syncs installed apps to server with retry logic
 * 
 * Retry Strategy:
 * - Retry 1: Wait 5 seconds, try again
 * - Retry 2: Wait 30 seconds, try again
 * - Retry 3: Wait 2 minutes, try again
 * - If all 3 fail: Stop, mark apps as FAILED (pending queue)
 */
@HiltWorker
class AppSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appRepository: AppRepository,
    private val selfControlApi: SelfControlApi,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "app_sync_worker"
        const val TAG = "AppSyncWorker"
        
        // Retry delays in milliseconds
        private const val RETRY_DELAY_1 = 5_000L      // 5 seconds
        private const val RETRY_DELAY_2 = 30_000L     // 30 seconds
        private const val RETRY_DELAY_3 = 120_000L    // 2 minutes
        
        const val MAX_RETRIES = 3
        
        // Input data keys
        const val KEY_IS_MANUAL_SYNC = "is_manual_sync"
        const val KEY_IS_IMMEDIATE_SYNC = "is_immediate_sync"
    }

    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        val isManualSync = inputData.getBoolean(KEY_IS_MANUAL_SYNC, false)
        val isImmediateSync = inputData.getBoolean(KEY_IS_IMMEDIATE_SYNC, false)
        
        Timber.i("[$TAG] Starting app sync (manual: $isManualSync, immediate: $isImmediateSync, attempt: ${runAttemptCount + 1})")

        return@withContext try {
            // Step 1: Get installed apps
            val appsResult = appRepository.getInstalledAppsForUpload()

            if (appsResult is DomainResult.Error) {
                Timber.e("[$TAG] Failed to get installed apps: ${appsResult.message}")
                return@withContext handleSyncFailure()
            }

            val apps = (appsResult as? DomainResult.Success)?.data ?: emptyList()

            if (apps.isEmpty()) {
                Timber.w("[$TAG] No apps found to sync")
                return@withContext ListenableWorker.Result.success()
            }

            Timber.d("[$TAG] Retrieved ${apps.size} apps for sync")

            // Step 2: Convert to upload format
            val appUploads = apps.map { app ->
                // Extract version code from version string (simplified)
                val versionCode = try {
                    app.version.split(".").firstOrNull()?.toInt() ?: 1
                } catch (e: Exception) {
                    1
                }

                AppUploadDto(
                    packageName = app.packageName,
                    appName = app.name,
                    versionCode = versionCode,
                    versionName = app.version
                )
            }

            val uploadRequest = AppUploadRequest(apps = appUploads)

            // Step 3: Try to upload with retry logic
            val syncSuccess = performSyncWithRetries(uploadRequest)
            
            if (syncSuccess) {
                // Success! Mark all apps as synced
                appRepository.markAllAppsSynced()
                settingsRepository.updateLastAppSyncTime(System.currentTimeMillis())
                Timber.i("[$TAG] ✅ Successfully synced ${apps.size} apps")
                ListenableWorker.Result.success()
            } else {
                // All retries failed - mark apps as FAILED for pending queue
                Timber.e("[$TAG] ❌ All sync attempts failed, apps marked as pending")
                ListenableWorker.Result.failure()
            }

        } catch (e: Exception) {
            Timber.e(e, "[$TAG] App sync failed with exception")
            handleSyncFailure()
        }
    }
    
    /**
     * Perform sync with built-in retry logic
     * Returns true if sync succeeds, false if all retries fail
     */
    private suspend fun performSyncWithRetries(uploadRequest: AppUploadRequest): Boolean {
        val retryDelays = listOf(0L, RETRY_DELAY_1, RETRY_DELAY_2, RETRY_DELAY_3)
        
        for (attempt in 0 until MAX_RETRIES) {
            // Wait before retry (first attempt has no delay)
            if (attempt > 0) {
                val delayMs = retryDelays.getOrElse(attempt) { RETRY_DELAY_3 }
                Timber.d("[$TAG] Retry $attempt/$MAX_RETRIES - waiting ${delayMs / 1000}s...")
                delay(delayMs)
            }
            
            try {
                Timber.d("[$TAG] Sync attempt ${attempt + 1}/$MAX_RETRIES")
                val uploadResult = selfControlApi.uploadApps(uploadRequest)
                
                if (uploadResult.success) {
                    Timber.i("[$TAG] Sync succeeded on attempt ${attempt + 1}")
                    return true
                } else {
                    Timber.w("[$TAG] Sync failed on attempt ${attempt + 1}: ${uploadResult.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "[$TAG] Sync attempt ${attempt + 1} threw exception")
            }
        }
        
        // All retries exhausted
        return false
    }
    
    /**
     * Handle sync failure - decide whether to retry via WorkManager or give up
     */
    private suspend fun handleSyncFailure(): ListenableWorker.Result {
        return if (runAttemptCount < MAX_RETRIES) {
            Timber.i("[$TAG] Scheduling WorkManager retry... Attempt ${runAttemptCount + 1}/$MAX_RETRIES")
            ListenableWorker.Result.retry()
        } else {
            Timber.e("[$TAG] ❌ Max WorkManager retries exceeded, apps remain pending")
            ListenableWorker.Result.failure()
        }
    }
}
