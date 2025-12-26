package com.selfcontrol.domain.usecase.policy

import com.selfcontrol.deviceowner.AppBlockManager
import com.selfcontrol.domain.model.AppPolicy
import com.selfcontrol.domain.model.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

/**
 * ApplyPolicyUseCase - Applies a blocking policy to an app
 * Integrates domain layer with device owner capabilities
 */
class ApplyPolicyUseCase @Inject constructor(
    private val appBlockManager: AppBlockManager
) {
    
    suspend operator fun invoke(policy: AppPolicy): Result<Unit> {
        return try {
            Timber.d("[ApplyPolicyUseCase] Applying policy for ${policy.packageName}")
            
            // Validate policy
            if (policy.packageName.isBlank()) {
                return Result.Error(Exception("Package name cannot be empty"))
            }
            
            // Enforce policy via AppBlockManager
            appBlockManager.enforcePolicy(policy)
            
            Timber.i("[ApplyPolicyUseCase] Policy applied successfully")
            Result.Success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "[ApplyPolicyUseCase] Failed to apply policy")
            Result.Error(e)
        }
    }
}
