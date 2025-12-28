package com.selfcontrol.domain.usecase.app

import com.selfcontrol.domain.model.AppPolicy
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.repository.PolicyRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to sync app policies from server
 */
class SyncAppPoliciesUseCase @Inject constructor(
    private val policyRepository: PolicyRepository
) {
    /**
     * Fetch latest policies from server and save locally
     */
    suspend operator fun invoke(): Result<List<AppPolicy>> {
        Timber.i("[SyncAppPolicies] Starting policy sync")
        
        return when (val result = policyRepository.syncPoliciesFromServer()) {
            is Result.Success -> {
                Timber.i("[SyncAppPolicies] Successfully synced ${result.data.size} policies")
                result
            }
            
            is Result.Error -> {
                Timber.e("[SyncAppPolicies] Sync failed: ${result.message}")
                result
            }
            
            is Result.Loading -> result
        }
    }
    
    /**
     * Get sync status (last sync time, pending changes, etc.)
     */
    suspend fun getSyncStatus(): SyncStatus {
        return SyncStatus(
            lastSyncTime = 0L, // TODO: Get from preferences
            hasPendingChanges = false,
            totalPolicies = 0
        )
    }
}

/**
 * Sync status information
 */
data class SyncStatus(
    val lastSyncTime: Long,
    val hasPendingChanges: Boolean,
    val totalPolicies: Int
)
