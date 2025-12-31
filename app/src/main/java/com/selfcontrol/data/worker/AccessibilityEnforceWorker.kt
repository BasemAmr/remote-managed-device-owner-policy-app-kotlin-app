package com.selfcontrol.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.selfcontrol.data.local.prefs.AppPreferences
import com.selfcontrol.deviceowner.DeviceOwnerManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Worker that periodically enforces accessibility service to ensure it remains active.
 * Runs every 6 hours to check and re-enable the accessibility service if needed.
 */
@HiltWorker
class AccessibilityEnforceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val deviceOwnerManager: DeviceOwnerManager,
    private val appPreferences: AppPreferences
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Timber.tag(TAG).i("Accessibility enforcement worker started at ${System.currentTimeMillis()}")

            // Check if device is Device Owner
            if (!deviceOwnerManager.isDeviceOwner()) {
                Timber.tag(TAG).d("Device is not Device Owner, skipping accessibility enforcement")
                return Result.success()
            }

            // Check current accessibility service status
            val isActive = deviceOwnerManager.isAccessibilityServiceActive()
            Timber.tag(TAG).d("Accessibility service active: $isActive")

            // Enforce accessibility service
            deviceOwnerManager.enforceAccessibilityService()
            Timber.tag(TAG).i("Accessibility service re-enforced successfully at ${System.currentTimeMillis()}")

            Result.success()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error enforcing accessibility service")
            
            // Retry up to 3 times
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Timber.tag(TAG).w("Retrying accessibility enforcement (attempt ${runAttemptCount + 1}/$MAX_RETRY_ATTEMPTS)")
                Result.retry()
            } else {
                Timber.tag(TAG).e("Max retry attempts reached for accessibility enforcement")
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "accessibility_enforce_worker"
        private const val TAG = "AccessibilityEnforceWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}
