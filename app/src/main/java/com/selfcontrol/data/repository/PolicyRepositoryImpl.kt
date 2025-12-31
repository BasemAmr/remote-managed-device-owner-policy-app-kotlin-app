package com.selfcontrol.data.repository

import com.selfcontrol.data.local.dao.PolicyDao
import com.selfcontrol.data.local.entity.PolicyEntity
import com.selfcontrol.data.local.prefs.AppPreferences
import com.selfcontrol.data.remote.api.SelfControlApi
import com.selfcontrol.data.remote.mapper.PolicyMapper
import com.selfcontrol.domain.model.AppPolicy
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.repository.PolicyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PolicyRepository
 * Handles app blocking policies with offline-first architecture
 */
@Singleton
class PolicyRepositoryImpl @Inject constructor(
    private val policyDao: PolicyDao,
    private val api: SelfControlApi,
    private val mapper: PolicyMapper,
    private val prefs: AppPreferences
) : PolicyRepository {
    
    override fun observeAllPolicies(): Flow<List<AppPolicy>> {
        return policyDao.observeAllPolicies()
            .map { entities -> entities.map { entityToDomain(it) } }
    }
    
    override fun observeBlockedPolicies(): Flow<List<AppPolicy>> {
        return policyDao.observeBlockedPolicies()
            .map { entities -> entities.map { entityToDomain(it) } }
    }
    
    override fun observePolicyForApp(packageName: String): Flow<AppPolicy?> {
        return policyDao.observePolicyForApp(packageName)
            .map { entity -> entity?.let { entityToDomain(it) } }
    }
    
    override suspend fun getPolicyForApp(packageName: String): AppPolicy? {
        return try {
            val entity = policyDao.getPolicyForApp(packageName)
            entity?.let { entityToDomain(it) }
        } catch (e: Exception) {
            Timber.e(e, "[PolicyRepo] Failed to get policy for $packageName")
            null
        }
    }
    
    override suspend fun getAllPolicies(): List<AppPolicy> {
        return try {
            val entities = policyDao.getAllPolicies()
            entities.map { entityToDomain(it) }
        } catch (e: Exception) {
            Timber.e(e, "[PolicyRepo] Failed to get all policies")
            emptyList()
        }
    }
    
    override suspend fun getActivePolicies(): List<AppPolicy> {
        return getAllPolicies().filter { !it.isExpired() }
    }
    
    override suspend fun getBlockedCount(): Result<Int> {
        return try {
            val count = policyDao.getBlockedCount()
            Result.Success(count)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun savePolicy(policy: AppPolicy): Result<Unit> {
        return try {
            // Check if a policy already exists for this package
            val existingPolicy = policyDao.getPolicyForApp(policy.packageName)
            
            // If exists, preserve the ID and createdAt timestamp
            val policyToSave = if (existingPolicy != null) {
                policy.copy(
                    id = existingPolicy.id,
                    createdAt = existingPolicy.createdAt
                )
            } else {
                policy
            }
            
            val entity = domainToEntity(policyToSave)
            policyDao.insertPolicy(entity)
            Timber.d("[PolicyRepo] Saved policy for ${policy.packageName} (ID: ${policyToSave.id})")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[PolicyRepo] Failed to save policy")
            Result.Error(e)
        }
    }
    
    override suspend fun savePolicies(policies: List<AppPolicy>): Result<Unit> {
        return try {
            val entities = policies.map { domainToEntity(it) }
            policyDao.insertPolicies(entities)
            Timber.d("[PolicyRepo] Saved ${policies.size} policies")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[PolicyRepo] Failed to save policies")
            Result.Error(e)
        }
    }
    
    override suspend fun deletePolicy(packageName: String): Result<Unit> {
        return try {
            policyDao.deleteByPackageName(packageName)
            Timber.d("[PolicyRepo] Deleted policy for $packageName")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[PolicyRepo] Failed to delete policy")
            Result.Error(e)
        }
    }
    
    override suspend fun syncPoliciesFromServer(): Result<List<AppPolicy>> {
        return try {
            val deviceId = prefs.deviceId.firstOrNull() ?: return Result.Error(Exception("No device ID"))
            
            val response = api.getPolicies()
            
            if (response.success && response.data != null) {
                // Get the list of policies directly from response data
                val policies = mapper.toDomainList(response.data)
                
                // Save to local database (offline-first)
                savePolicies(policies)
                
                // Update sync timestamp
                prefs.setLastPolicySync(System.currentTimeMillis())
                
                Timber.i("[PolicyRepo] Fetched ${policies.size} policies from server")
                Result.Success(policies)
            } else {
                Result.Error(Exception(response.message ?: "Failed to fetch policies"))
            }
        } catch (e: Exception) {
            Timber.e(e, "[PolicyRepo] Failed to fetch policies from server")
            Result.Error(e)
        }
    }
    
    override suspend fun syncToServer(policy: AppPolicy): Result<Unit> {
        return try {
            val deviceId = prefs.deviceId.firstOrNull() ?: return Result.Error(Exception("No device ID"))
            val dto = mapper.toDto(policy, deviceId)
            
            val response = api.applyPolicy(dto)
            
            if (response.success) {
                Timber.i("[PolicyRepo] Synced policy ${policy.packageName} to server")
                Result.Success(Unit)
            } else {
                Result.Error(Exception(response.message ?: "Failed to sync policy"))
            }
        } catch (e: Exception) {
            Timber.e(e, "[PolicyRepo] Failed to sync policy to server")
            Result.Error(e)
        }
    }
    
    override suspend fun getUnsyncedPolicies(): List<AppPolicy> {
        return try {
            val entities = policyDao.getUnsyncedPolicies()
            entities.map { entityToDomain(it) }
        } catch (e: Exception) {
            Timber.e(e, "[PolicyRepo] Failed to get unsynced policies")
            emptyList()
        }
    }
    
    // ==================== Mappers ====================
    
    private fun entityToDomain(entity: PolicyEntity): AppPolicy {
        return AppPolicy(
            id = entity.id,
            packageName = entity.packageName,
            isBlocked = entity.isBlocked,
            isLocked = entity.isLocked,
            expiresAt = entity.expiresAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            reason = entity.reason.orEmpty()
        )
    }
    
    private fun domainToEntity(domain: AppPolicy): PolicyEntity {
        return PolicyEntity(
            id = domain.id,
            packageName = domain.packageName,
            isBlocked = domain.isBlocked,
            isLocked = domain.isLocked,
            expiresAt = domain.expiresAt,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            reason = domain.reason
        )
    }
}
