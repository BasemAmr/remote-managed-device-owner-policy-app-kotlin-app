package com.selfcontrol.domain.usecase.app

import com.selfcontrol.deviceowner.AppBlockManager
import com.selfcontrol.domain.model.Result
import timber.log.Timber
import javax.inject.Inject

/**
 * CheckAppAllowedUseCase - Checks if an app is allowed to run
 * Used by accessibility service and other monitoring components
 */
class CheckAppAllowedUseCase @Inject constructor(
    private val appBlockManager: AppBlockManager
) {
    
    suspend operator fun invoke(packageName: String): Result<Boolean> {
        return try {
            Timber.d("[CheckAppAllowedUseCase] Checking if app allowed: $packageName")
            
            if (packageName.isBlank()) {
                return Result.Error(Exception("Package name cannot be empty"))
            }
            
            val isAllowed = appBlockManager.isAppAllowed(packageName)
            
            Timber.d("[CheckAppAllowedUseCase] App $packageName allowed: $isAllowed")
            Result.Success(isAllowed)
            
        } catch (e: Exception) {
            Timber.e(e, "[CheckAppAllowedUseCase] Error checking app")
            Result.Error(e)
        }
    }
}
