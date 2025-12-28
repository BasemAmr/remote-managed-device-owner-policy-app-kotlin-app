package com.selfcontrol.domain.usecase.policy

import com.selfcontrol.domain.model.AppPolicy
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.repository.PolicyRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to get active (non-expired) policies
 */
class GetActivePoliciesUseCase @Inject constructor(
    private val policyRepository: PolicyRepository
) {
    /**
     * Get all active policies as Flow
     */
    operator fun invoke(): Flow<List<AppPolicy>> {
        Timber.d("[GetActivePolicies] Observing active policies")
        return policyRepository.observePolicies()
    }
    
    /**
     * Get only blocked policies
     */
    fun getBlockedPolicies(): Flow<List<AppPolicy>> {
        Timber.d("[GetActivePolicies] Observing blocked policies")
        return policyRepository.observeBlockedPolicies()
    }
    
    /**
     * Get active policies (non-expired) as one-time fetch
     */
    suspend fun getActive(): Result<List<AppPolicy>> {
        Timber.d("[GetActivePolicies] Fetching active policies")
        
        // Use a list return and wrap it if necessary, or update repo to return Result
        return try {
            val policies = policyRepository.getActivePolicies()
            val activePolicies = policies.filter { !it.isExpired() }
            Timber.i("[GetActivePolicies] Found ${activePolicies.size} active policies")
            Result.Success(activePolicies)
        } catch (e: Exception) {
            Timber.e(e, "[GetActivePolicies] Error fetching active policies")
            Result.Error(e)
        }
    }
    
    /**
     * Get count of blocked apps
     */
    suspend fun getBlockedCount(): Result<Int> {
        return policyRepository.getBlockedCount()
    }
}
