package com.selfcontrol.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.selfcontrol.deviceowner.AppBlockManager
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.repository.PolicyRepository
import com.selfcontrol.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * PolicySyncWorker - Periodic worker that syncs policies from server
 * Runs every 15 minutes to fetch latest policies and enforce them
 */
@HiltWorker
class PolicySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val policyRepository: PolicyRepository,
    private val appBlockManager: AppBlockManager,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "policy_sync_worker"
        const val TAG = "PolicySyncWorker"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Timber.i("[$TAG] Starting policy sync")
        val startTime = System.currentTimeMillis()
        
        return@withContext try {
            // Step 1: Fetch latest policies from server
            val fetchResult = policyRepository.syncPoliciesFromServer()
            
            if (fetchResult.isEmpty()) {
                Timber.w("[$TAG] No policies fetched from server")
                return@withContext Result.success()
            }
            
            Timber.d("[$TAG] Fetched ${fetchResult.size} policies from server")
            
            // Step 2: Save policies locally
            policyRepository.savePolicies(fetchResult)
            
            // Step 3: Enforce each policy
            var enforceSuccess = 0
            var enforceFailed = 0
            
            fetchResult.forEach { policy ->
                try {
                    appBlockManager.enforcePolicy(policy)
                    enforceSuccess++
                } catch (e: Exception) {
                    Timber.e(e, "[$TAG] Failed to enforce policy for ${policy.packageName}")
                    enforceFailed++
                }
            }
            
            // Step 4: Update last sync time
            settingsRepository.updateLastSyncTime(System.currentTimeMillis())
            
            val duration = System.currentTimeMillis() - startTime
            Timber.i("[$TAG] Completed in ${duration}ms. Enforced: $enforceSuccess, Failed: $enforceFailed")
            
            Result.success()
            
        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Policy sync failed")
            
            // Retry with exponential backoff (max 3 attempts)
            if (runAttemptCount < 3) {
                Timber.i("[$TAG] Retrying... Attempt ${runAttemptCount + 1}/3")
                Result.retry()
            } else {
                Timber.e("[$TAG] Max retries exceeded, marking as failure")
                Result.failure()
            }
        }
    }
}
