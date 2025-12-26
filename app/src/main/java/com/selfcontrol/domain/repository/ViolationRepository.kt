package com.selfcontrol.domain.repository

import com.selfcontrol.domain.model.Violation
import kotlinx.coroutines.flow.Flow

interface ViolationRepository {
    fun observeViolations(): Flow<List<Violation>>
    suspend fun logViolation(violation: Violation)
    suspend fun getUnsyncedViolations(): List<Violation>
    suspend fun syncViolationsToServer()
    suspend fun deleteOldSyncedViolations(olderThanMillis: Long)
}

