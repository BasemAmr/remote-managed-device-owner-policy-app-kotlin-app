package com.selfcontrol.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.selfcontrol.data.local.prefs.AppPreferences
import com.selfcontrol.data.remote.api.SelfControlApi
import com.selfcontrol.deviceowner.DeviceOwnerManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * HeartbeatWorker - Periodic worker that sends device heartbeat to server
 * Runs every 30 minutes to signal device is alive and functioning
 */
@HiltWorker
class HeartbeatWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: SelfControlApi,
    private val prefs: AppPreferences,
    private val deviceOwnerManager: DeviceOwnerManager
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "heartbeat_worker"
        const val TAG = "HeartbeatWorker"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Timber.i("[$TAG] Sending heartbeat")
        val startTime = System.currentTimeMillis()
        
        return@withContext try {
            val deviceId = prefs.deviceId.first()
            
            // Build heartbeat payload with device status
            val heartbeatData = mapOf(
                "device_id" to deviceId,
                "timestamp" to System.currentTimeMillis().toString(),
                "is_device_owner" to deviceOwnerManager.isDeviceOwner().toString(),
                "is_admin_active" to deviceOwnerManager.isAdminActive().toString()
            )
            
            // Send heartbeat to server
            val response = api.sendHeartbeat(heartbeatData)
            
            if (response.success) {
                val duration = System.currentTimeMillis() - startTime
                Timber.i("[$TAG] Heartbeat sent successfully in ${duration}ms")
                Result.success()
            } else {
                Timber.w("[$TAG] Server rejected heartbeat: ${response.message}")
                
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
            
        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Heartbeat failed")
            
            if (runAttemptCount < 3) {
                Timber.i("[$TAG] Retrying... Attempt ${runAttemptCount + 1}/3")
                Result.retry()
            } else {
                // Don't mark as failure - heartbeat isn't critical
                Timber.w("[$TAG] Heartbeat max retries exceeded, continuing...")
                Result.success()
            }
        }
    }
}
