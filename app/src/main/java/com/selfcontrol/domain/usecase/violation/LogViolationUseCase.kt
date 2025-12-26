package com.selfcontrol.domain.usecase.violation

import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.model.Violation
import com.selfcontrol.domain.model.ViolationType
import com.selfcontrol.domain.repository.ViolationRepository
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Use case to log a violation attempt
 */
class LogViolationUseCase @Inject constructor(
    private val violationRepository: ViolationRepository
) {
    /**
     * Log a violation
     * @param packageName Package name of the app
     * @param appName Display name of the app
     * @param type Type of violation
     * @param details Optional additional details
     */
    suspend operator fun invoke(
        packageName: String,
        appName: String,
        type: ViolationType,
        details: String? = null
    ): Result<Unit> {
        Timber.i("[LogViolation] Logging violation: $type for $packageName")
        
        val violation = Violation(
            id = UUID.randomUUID().toString(),
            packageName = packageName,
            appName = appName,
            type = type,
            timestamp = System.currentTimeMillis(),
            details = details,
            synced = false
        )
        
        return when (val result = violationRepository.logViolation(violation)) {
            is Result.Success -> {
                Timber.i("[LogViolation] Violation logged successfully")
                result
            }
            
            is Result.Error -> {
                Timber.e("[LogViolation] Failed to log violation: ${result.message}")
                result
            }
            
            is Result.Loading -> result
        }
    }
    
    /**
     * Log app launch attempt
     */
    suspend fun logAppLaunchAttempt(
        packageName: String,
        appName: String
    ): Result<Unit> {
        return invoke(
            packageName = packageName,
            appName = appName,
            type = ViolationType.APP_LAUNCH_ATTEMPT,
            details = "User attempted to launch blocked app"
        )
    }
    
    /**
     * Log URL access attempt
     */
    suspend fun logUrlAccessAttempt(
        url: String,
        details: String? = null
    ): Result<Unit> {
        return invoke(
            packageName = "browser",
            appName = "Browser",
            type = ViolationType.URL_ACCESS_ATTEMPT,
            details = details ?: "Attempted to access: $url"
        )
    }
    
    /**
     * Log policy bypass attempt
     */
    suspend fun logPolicyBypassAttempt(
        packageName: String,
        appName: String,
        details: String
    ): Result<Unit> {
        return invoke(
            packageName = packageName,
            appName = appName,
            type = ViolationType.POLICY_BYPASS_ATTEMPT,
            details = details
        )
    }
}
