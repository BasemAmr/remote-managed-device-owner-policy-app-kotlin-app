package com.selfcontrol.data.repository

import com.selfcontrol.data.local.dao.ViolationDao
import com.selfcontrol.data.local.entity.ViolationEntity
import com.selfcontrol.data.local.prefs.AppPreferences
import com.selfcontrol.data.remote.api.SelfControlApi
import com.selfcontrol.data.remote.mapper.ViolationMapper
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
        return violationDao.observeRecentViolations(100)
            .map { entities -> entities.map { entityToDomain(it) } }
    }
    
    override suspend fun logViolation(violation: Violation) {
        try {
            val entity = domainToEntity(violation)
            violationDao.insertViolation(entity)
            
            Timber.i("[ViolationRepo] Logged violation: ${violation.type} for ${violation.packageName}")
            
            // Try to sync immediately (best effort)
            syncViolationToServer(violation)
            
        } catch (e: Exception) {
            Timber.e(e, "[ViolationRepo] Failed to log violation")
            throw e
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
    
    override suspend fun syncViolationsToServer() {
        try {
            val unsyncedEntities = violationDao.getUnsyncedViolations()
            
            if (unsyncedEntities.isEmpty()) {
                Timber.d("[ViolationRepo] No violations to sync")
                return
            }
            
            val deviceId = prefs.deviceId.firstOrNull() ?: throw Exception("No device ID")
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
            } else {
                throw Exception(response.message ?: "Failed to sync violations")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "[ViolationRepo] Failed to sync violations to server")
            throw e
        }
    }
    
    override suspend fun deleteOldSyncedViolations(olderThanMillis: Long) {
        try {
            violationDao.deleteOlderThan(olderThanMillis)
            Timber.d("[ViolationRepo] Deleted old synced violations")
        } catch (e: Exception) {
            Timber.e(e, "[ViolationRepo] Failed to delete old violations")
            throw e
        }
    }
    
    // ==================== Private Methods ====================
    
    private suspend fun syncViolationToServer(violation: Violation) {
        try {
            val deviceId = prefs.deviceId.firstOrNull() ?: return
            val dto = mapper.toDto(violation, deviceId)
            
            val response = api.logViolation(dto)
            
            if (response.success) {
                // Mark as synced
                violationDao.markAsSynced(listOf(violation.id))
                Timber.d("[ViolationRepo] Synced violation ${violation.id} immediately")
            }
        } catch (e: Exception) {
            Timber.w(e, "[ViolationRepo] Immediate sync failed, will retry later")
            // Not a critical error - will be synced in batch later
        }
    }
    
    // ==================== Mappers ====================
    
    private fun entityToDomain(entity: ViolationEntity): Violation {
        return Violation(
            id = entity.id,
            packageName = entity.packageName,
            appName = entity.appName,
            type = try {
                ViolationType.valueOf(entity.violationType.uppercase().replace("-", "_"))
            } catch (e: Exception) {
                ViolationType.UNKNOWN
            },
            message = entity.details,
            timestamp = entity.timestamp,
            details = entity.details,
            synced = entity.synced
        )
    }
    
    private fun domainToEntity(domain: Violation): ViolationEntity {
        return ViolationEntity(
            id = domain.id,
            packageName = domain.packageName,
            appName = domain.appName,
            violationType = domain.type.name.lowercase().replace("_", "-"),
            timestamp = domain.timestamp,
            details = domain.details,
            synced = domain.synced
        )
    }
}
