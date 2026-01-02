package com.selfcontrol.data.repository

import android.content.Context
import com.selfcontrol.data.local.dao.AccessibilityServiceDao
import com.selfcontrol.data.mapper.toDomain
import com.selfcontrol.data.mapper.toDto
import com.selfcontrol.data.mapper.toEntity
import com.selfcontrol.data.remote.api.SelfControlApi
import com.selfcontrol.deviceowner.AccessibilityHelpers
import com.selfcontrol.domain.model.AccessibilityService
import com.selfcontrol.domain.repository.AccessibilityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityRepositoryImpl @Inject constructor(
    private val accessibilityServiceDao: AccessibilityServiceDao,
    private val api: SelfControlApi,
    @ApplicationContext private val context: Context
) : AccessibilityRepository {
    
    override fun observeAllServices(): Flow<List<AccessibilityService>> {
        return accessibilityServiceDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun observeLockedServices(): Flow<List<AccessibilityService>> {
        return accessibilityServiceDao.observeLocked().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun scanAndSyncServices(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Scan device for accessibility services
                val scannedServices = AccessibilityHelpers.scanAccessibilityServices(context)
                
                // Save to local database
                val entities = scannedServices.map { it.toEntity() }
                accessibilityServiceDao.insertAll(entities)
                
                // Upload to backend
                val dtos = scannedServices.map { it.toDto() }
                api.uploadAccessibilityServices(mapOf("services" to dtos))
                
                Timber.i("[AccessibilityRepo] Scanned and synced ${scannedServices.size} services")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Timber.e(e, "[AccessibilityRepo] Failed to scan and sync services")
                Result.failure(e)
            }
        }
    }
    
    override suspend fun syncLockedServicesFromBackend(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val lockedServices = api.getLockedAccessibilityServices()
                
                // Update local database with lock status
                for (dto in lockedServices) {
                    val entity = accessibilityServiceDao.getByServiceId(dto.serviceId)
                    if (entity != null) {
                        accessibilityServiceDao.update(
                            entity.copy(isLocked = dto.isLocked ?: false)
                        )
                    }
                }
                
                Timber.i("[AccessibilityRepo] Synced ${lockedServices.size} locked services")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Timber.e(e, "[AccessibilityRepo] Failed to sync locked services")
                Result.failure(e)
            }
        }
    }
    
    override suspend fun reportServiceStatus(serviceId: String, isEnabled: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                api.reportAccessibilityStatus(
                    mapOf("service_id" to serviceId, "is_enabled" to isEnabled)
                )
                
                // Update local database
                val entity = accessibilityServiceDao.getByServiceId(serviceId)
                if (entity != null) {
                    accessibilityServiceDao.update(entity.copy(isEnabled = isEnabled))
                }
                
                Result.success(Unit)
                
            } catch (e: Exception) {
                Timber.e(e, "[AccessibilityRepo] Failed to report service status")
                Result.failure(e)
            }
        }
    }
}
