package com.selfcontrol.deviceowner

import com.selfcontrol.domain.model.AppPolicy
import com.selfcontrol.domain.model.Violation
import com.selfcontrol.domain.model.ViolationType
import com.selfcontrol.domain.repository.PolicyRepository
import com.selfcontrol.domain.repository.ViolationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppBlockManager - Enforces app blocking policies
 * Integrates DeviceOwnerManager with policy repository
 */
@Singleton
class AppBlockManager @Inject constructor(
    private val deviceOwnerManager: DeviceOwnerManager,
    private val policyRepository: PolicyRepository,
    private val violationRepository: ViolationRepository
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Enforce a policy (block or unblock an app)
     */
    suspend fun enforcePolicy(policy: AppPolicy) {
        Timber.d("[AppBlockManager] Enforcing policy for ${policy.packageName}")
        
        try {
            // Check if policy is expired
            if (policy.isExpired()) {
                Timber.w("[AppBlockManager] Policy expired for ${policy.packageName}")
                return
            }
            
            // Apply the policy using device owner
            val success = if (policy.isBlocked) {
                deviceOwnerManager.disableApp(policy.packageName)
            } else {
                deviceOwnerManager.enableApp(policy.packageName)
            }
            
            if (success) {
                // Save policy to local database
                policyRepository.savePolicy(policy)
                
                // Sync to server in background
                scope.launch {
                    try {
                        policyRepository.syncToServer(policy)
                    } catch (e: Exception) {
                        Timber.e(e, "[AppBlockManager] Failed to sync policy to server")
                    }
                }
                
                Timber.i("[AppBlockManager] Successfully enforced policy for ${policy.packageName}")
            } else {
                throw Exception("Failed to apply policy via DevicePolicyManager")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "[AppBlockManager] Failed to enforce policy for ${policy.packageName}")
            
            // Log violation
            logPolicyEnforcementFailure(policy, e)
            
            throw e
        }
    }
    
    /**
     * Check if an app is allowed to run
     */
    suspend fun isAppAllowed(packageName: String): Boolean {
        return try {
            val policy = policyRepository.getPolicyForApp(packageName)
            
            // If no policy exists, app is allowed
            if (policy == null) {
                Timber.d("[AppBlockManager] No policy for $packageName - allowed")
                return true
            }
            
            // Check if policy is expired
            if (policy.isExpired()) {
                Timber.d("[AppBlockManager] Policy expired for $packageName - allowed")
                return true
            }
            
            // Check if app is blocked
            val allowed = !policy.isBlocked
            
            if (!allowed) {
                Timber.i("[AppBlockManager] App $packageName is blocked")
                logViolationAttempt(packageName)
            }
            
            allowed
            
        } catch (e: Exception) {
            Timber.e(e, "[AppBlockManager] Error checking if app allowed: $packageName")
            // Default to allowed on error to avoid blocking legitimate usage
            true
        }
    }
    
    /**
     * Enforce all active policies
     * Called during app startup or policy sync
     */
    suspend fun enforceAllPolicies() {
        Timber.i("[AppBlockManager] Enforcing all active policies")
        
        try {
            val policies = policyRepository.getAllPolicies()
            
            policies.forEach { policy ->
                try {
                    enforcePolicy(policy)
                } catch (e: Exception) {
                    Timber.e(e, "[AppBlockManager] Failed to enforce policy for ${policy.packageName}")
                    // Continue with other policies
                }
            }
            
            Timber.i("[AppBlockManager] Enforced ${policies.size} policies")
            
        } catch (e: Exception) {
            Timber.e(e, "[AppBlockManager] Failed to enforce all policies")
        }
    }
    
    /**
     * Block an app immediately
     */
    suspend fun blockApp(packageName: String) {
        val policy = AppPolicy(
            packageName = packageName,
            isBlocked = true,
            isLocked = false
        )
        enforcePolicy(policy)
    }
    
    /**
     * Unblock an app immediately
     */
    suspend fun unblockApp(packageName: String) {
        val policy = AppPolicy(
            packageName = packageName,
            isBlocked = false,
            isLocked = false
        )
        enforcePolicy(policy)
    }
    
    /**
     * Get current blocking status of an app
     */
    suspend fun isAppBlocked(packageName: String): Boolean {
        return try {
            // Check local policy first
            val policy = policyRepository.getPolicyForApp(packageName)
            if (policy != null && !policy.isExpired()) {
                return policy.isBlocked
            }
            
            // Fallback to device owner status
            deviceOwnerManager.isAppHidden(packageName)
            
        } catch (e: Exception) {
            Timber.e(e, "[AppBlockManager] Error checking block status: $packageName")
            false
        }
    }
    
    /**
     * Log a violation attempt (user tried to open blocked app)
     */
    private fun logViolationAttempt(packageName: String) {
        scope.launch {
            try {
                val violation = Violation(
                    packageName = packageName,
                    type = ViolationType.BLOCKED_APP_ACCESS_ATTEMPT,
                    message = "User attempted to access blocked app"
                )
                
                violationRepository.logViolation(violation)
                
            } catch (e: Exception) {
                Timber.e(e, "[AppBlockManager] Failed to log violation")
            }
        }
    }
    
    /**
     * Log a policy enforcement failure
     */
    private fun logPolicyEnforcementFailure(policy: AppPolicy, error: Exception) {
        scope.launch {
            try {
                val violation = Violation(
                    packageName = policy.packageName,
                    type = ViolationType.POLICY_ENFORCEMENT_FAILED,
                    message = "Failed to enforce policy: ${error.message}"
                )
                
                violationRepository.logViolation(violation)
                
            } catch (e: Exception) {
                Timber.e(e, "[AppBlockManager] Failed to log policy enforcement failure")
            }
        }
    }
}
