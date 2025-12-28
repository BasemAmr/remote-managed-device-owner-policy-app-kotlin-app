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
import com.selfcontrol.domain.repository.AppRepository
import com.selfcontrol.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * AppSyncWorker - Periodic worker that syncs installed apps to server
 * Runs every 60 minutes to upload list of installed apps
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
    }

    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        Timber.i("[$TAG] Starting app sync")

        return@withContext try {
            // Step 1: Get installed apps
            val appsResult = appRepository.getInstalledAppsForUpload()

            if (appsResult is DomainResult.Error) {
                Timber.e("[$TAG] Failed to get installed apps: ${appsResult.message}")
                return@withContext ListenableWorker.Result.retry()
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

            // Step 3: Upload to server
            val uploadResult = selfControlApi.uploadApps(uploadRequest)

            if (!uploadResult.success) {
                Timber.e("[$TAG] Failed to upload apps: ${uploadResult.message}")
                return@withContext ListenableWorker.Result.retry()
            }

            // Step 4: Update last sync time
            settingsRepository.updateLastAppSyncTime(System.currentTimeMillis())

            Timber.i("[$TAG] Successfully synced ${apps.size} apps")

            ListenableWorker.Result.success()

        } catch (e: Exception) {
            Timber.e(e, "[$TAG] App sync failed")

            // Retry with exponential backoff (max 3 attempts)
            if (runAttemptCount < 3) {
                Timber.i("[$TAG] Retrying... Attempt ${runAttemptCount + 1}/3")
                ListenableWorker.Result.retry()
            } else {
                Timber.e("[$TAG] Max retries exceeded, marking as failure")
                ListenableWorker.Result.failure()
            }
        }
    }
}