package com.selfcontrol.domain.usecase.violation

import com.selfcontrol.domain.model.Violation
import com.selfcontrol.domain.repository.ViolationRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to get violation logs
 */
class GetViolationsUseCase @Inject constructor(
    private val violationRepository: ViolationRepository
) {
    /**
     * Get recent violations as Flow
     * @param limit Maximum number of violations to retrieve
     */
    operator fun invoke(limit: Int = 100): Flow<List<Violation>> {
        Timber.d("[GetViolations] Observing recent violations (limit: $limit)")
        return violationRepository.observeRecentViolations(limit)
    }
    
    /**
     * Get violations for a specific app
     */
    fun getViolationsForApp(packageName: String): Flow<List<Violation>> {
        Timber.d("[GetViolations] Observing violations for $packageName")
        return violationRepository.observeViolationsForApp(packageName)
    }
    
    /**
     * Get unsynced violations
     */
    fun getUnsyncedViolations(): Flow<List<Violation>> {
        Timber.d("[GetViolations] Observing unsynced violations")
        return violationRepository.observeUnsyncedViolations()
    }
}
