package com.selfcontrol.data.repository

import com.selfcontrol.data.local.dao.ViolationDao
import com.selfcontrol.data.local.entity.ViolationEntity
import com.selfcontrol.data.local.prefs.AppPreferences
import com.selfcontrol.data.remote.api.SelfControlApi
import com.selfcontrol.data.remote.mapper.ViolationMapper
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.model.Violation
import com.selfcontrol.domain.model.ViolationType
import com.selfcontrol.domain.repository.ViolationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ViolationRepository
 * Handles violation logging and syncing
 */
@Singleton
class ViolationRepositoryImpl @Inject constructor(
    private val violationDao: ViolationDao,
    private val api: SelfControlApi,
    private val mapper: ViolationMapper,
    private val prefs: AppPreferences
) : ViolationRepository {
    
    override fun observeViolations(): Flow<List<Violation>> {
        return observeRecentViolations(100)
    }

    override fun observeRecentViolations(limit: Int): Flow<List<Violation>> {
        return violationDao.observeRecentViolations(limit)
            .map { entities -> entities.map { entityToDomain(it) } }
    }

    override fun observeViolationsForApp(packageName: String): Flow<List<Violation>> {
        return violationDao.observeViolationsForApp(packageName)
            .map { entities -> entities.map { entityToDomain(it) } }
    }

    override fun observeUnsyncedViolations(): Flow<List<Violation>> {
        return violationDao.observeUnsyncedViolations()
            .map { entities -> entities.map { entityToDomain(it) } }
    }
    
    // Custom mapper for entity
    private fun entityToDomain(entity: ViolationEntity): Violation {
        return Violation(
            id = entity.id,
            appPackage = entity.appPackage.ifEmpty { entity.packageName },
            packageName = entity.packageName,
            appName = entity.appName,
            type = try {
               ViolationType.valueOf(entity.violationType.uppercase())
            } catch (e: Exception) {
               ViolationType.UNKNOWN
            },
            message = entity.message,
            timestamp = entity.timestamp,
            details = entity.details ?: "",
            synced = entity.synced
        )
    }

    private fun domainToEntity(domain: Violation): ViolationEntity {
        return ViolationEntity(
            id = domain.id,
            appPackage = domain.appPackage,
            packageName = domain.packageName,
            appName = domain.appName,
            violationType = domain.type.name.lowercase(),
            message = domain.message,
            timestamp = domain.timestamp,
            details = domain.details,
            synced = domain.synced
        )
    }

    override suspend fun logViolation(violation: Violation): Result<Unit> {
        return try {
            val entity = domainToEntity(violation)
            violationDao.insertViolation(entity)
            
            Timber.i("[ViolationRepo] Logged violation: ${violation.type} for ${violation.appPackage}")
            
            // Try to sync immediately (best effort)
            val deviceId = prefs.deviceId.firstOrNull()
            if (deviceId != null) {
                val dto = mapper.toDto(violation, deviceId)
                api.logViolation(dto)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[ViolationRepo] Failed to log violation")
            Result.Error(e)
        }
    }
    
    override suspend fun getViolations(): Result<List<Violation>> {
        return try {
            val entities = violationDao.getRecentViolations(100)
            Result.Success(entities.map { entityToDomain(it) })
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getUnsyncedViolations(): List<Violation> {
        return try {
            val entities = violationDao.getUnsyncedViolations()
            entities.map { entityToDomain(it) }
        } catch (e: Exception) {
            Timber.e(e, "[ViolationRepo] Failed to get unsynced violations")
            emptyList()
        }
    }
    
    override suspend fun syncViolationsToServer(): Result<Unit> {
        return try {
            val unsyncedEntities = violationDao.getUnsyncedViolations()
            
            if (unsyncedEntities.isEmpty()) {
                Timber.d("[ViolationRepo] No violations to sync")
                return Result.Success(Unit)
            }
            
            val deviceId = prefs.deviceId.firstOrNull() ?: return Result.Error(Exception("No device ID"))
            val violations = unsyncedEntities.map { entityToDomain(it) }
            val dtos = mapper.toDtoList(violations, deviceId)
            
            val response = api.logViolationsBatch(dtos)
            
            if (response.success) {
                // Mark as synced
                val violationIds = unsyncedEntities.map { it.id }
                violationDao.markAsSynced(violationIds)
                
                // Update sync timestamp
                prefs.setLastViolationSync(System.currentTimeMillis())
                
                Timber.i("[ViolationRepo] Synced ${violations.size} violations to server")
                Result.Success(Unit)
            } else {
                Result.Error(Exception(response.message ?: "Failed to sync violations"))
            }
        } catch (e: Exception) {
            Timber.e(e, "[ViolationRepo] Failed to sync violations to server")
            Result.Error(e)
        }
    }
    
    override suspend fun deleteOldSyncedViolations(olderThanMillis: Long): Result<Unit> {
        return try {
            violationDao.deleteOlderThan(olderThanMillis)
            Timber.d("[ViolationRepo] Deleted old synced violations")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[ViolationRepo] Failed to delete old violations")
            Result.Error(e)
        }
    }

    override suspend fun getViolationCount(): Result<Int> {
        return try {
            val count = violationDao.getViolationCount()
            Result.Success(count)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
