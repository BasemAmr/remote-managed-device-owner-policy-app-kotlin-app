package com.selfcontrol.domain.repository

import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.model.Violation
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing violation logs
 */
interface ViolationRepository {
    // Flows
    fun observeViolations(): Flow<List<Violation>>
    fun observeRecentViolations(limit: Int): Flow<List<Violation>>
    fun observeViolationsForApp(packageName: String): Flow<List<Violation>>
    fun observeUnsyncedViolations(): Flow<List<Violation>>
    
    // One-time operations
    suspend fun logViolation(violation: Violation): Result<Unit>
    suspend fun getViolations(): Result<List<Violation>>
    suspend fun getUnsyncedViolations(): List<Violation>
    suspend fun syncViolationsToServer(): Result<Unit>
    suspend fun deleteOldSyncedViolations(olderThanMillis: Long): Result<Unit>
    suspend fun getViolationCount(): Result<Int>
}
