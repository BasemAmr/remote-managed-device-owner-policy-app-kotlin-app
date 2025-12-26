package com.selfcontrol.domain.repository

import com.selfcontrol.domain.model.AppPolicy
import kotlinx.coroutines.flow.Flow

interface PolicyRepository {
    fun observeAllPolicies(): Flow<List<AppPolicy>>
    fun observeBlockedPolicies(): Flow<List<AppPolicy>>
    fun observePolicyForApp(packageName: String): Flow<AppPolicy?>
    suspend fun getPolicyForApp(packageName: String): AppPolicy?
    suspend fun getAllPolicies(): List<AppPolicy>
    suspend fun savePolicy(policy: AppPolicy)
    suspend fun savePolicies(policies: List<AppPolicy>)
    suspend fun deletePolicy(packageName: String)
    suspend fun syncPoliciesFromServer(): List<AppPolicy>
    suspend fun syncToServer(policy: AppPolicy)
    suspend fun getUnsyncedPolicies(): List<AppPolicy>
}

