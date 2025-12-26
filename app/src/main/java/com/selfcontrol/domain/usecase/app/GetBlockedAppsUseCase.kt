package com.selfcontrol.domain.usecase.app

import com.selfcontrol.domain.model.App
import com.selfcontrol.domain.model.AppPolicy
import com.selfcontrol.domain.repository.AppRepository
import com.selfcontrol.domain.repository.PolicyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to get apps that are currently blocked
 */
class GetBlockedAppsUseCase @Inject constructor(
    private val appRepository: AppRepository,
    private val policyRepository: PolicyRepository
) {
    /**
     * Get all blocked apps with their details
     * Combines app info with blocking policies
     */
    operator fun invoke(): Flow<List<BlockedApp>> {
        Timber.d("[GetBlockedApps] Observing blocked apps")
        
        return combine(
            appRepository.observeAllApps(),
            policyRepository.observeBlockedPolicies()
        ) { apps, policies ->
            // Create map for quick lookup
            val appMap = apps.associateBy { it.packageName }
            
            // Combine policy with app info
            policies.mapNotNull { policy ->
                val app = appMap[policy.packageName]
                if (app != null) {
                    BlockedApp(
                        app = app,
                        policy = policy
                    )
                } else {
                    Timber.w("[GetBlockedApps] Policy exists for unknown app: ${policy.packageName}")
                    null
                }
            }
        }
    }
}

/**
 * Data class combining app and its blocking policy
 */
data class BlockedApp(
    val app: App,
    val policy: AppPolicy
)
