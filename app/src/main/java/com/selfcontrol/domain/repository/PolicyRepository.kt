package com.selfcontrol.domain.repository

import com.selfcontrol.domain.model.AppPolicy
import com.selfcontrol.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing app policies
 */
interface PolicyRepository {
    // Flows
    fun observeAllPolicies(): Flow<List<AppPolicy>>
    fun observeBlockedPolicies(): Flow<List<AppPolicy>>
    fun observePolicyForApp(packageName: String): Flow<AppPolicy?>
    
    // One-time operations
    suspend fun getPolicyForApp(packageName: String): AppPolicy?
    suspend fun getAllPolicies(): List<AppPolicy>
    suspend fun getActivePolicies(): List<AppPolicy> // Added this
    suspend fun getBlockedCount(): Result<Int> // Added this
    suspend fun savePolicy(policy: AppPolicy): Result<Unit>
    suspend fun savePolicies(policies: List<AppPolicy>): Result<Unit>
    suspend fun deletePolicy(packageName: String): Result<Unit>
    suspend fun syncPoliciesFromServer(): Result<List<AppPolicy>> // Changed to Result
    suspend fun syncToServer(policy: AppPolicy): Result<Unit>
    suspend fun getUnsyncedPolicies(): List<AppPolicy>
    
    // Compatibility/Aliases if needed
    suspend fun fetchLatestPolicies(): Result<List<AppPolicy>> = syncPoliciesFromServer()
    fun observePolicies(): Flow<List<AppPolicy>> = observeAllPolicies()
}
