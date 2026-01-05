package com.selfcontrol.data.repository

import android.content.Context
import com.selfcontrol.data.local.dao.AccessibilityServiceDao
import com.selfcontrol.data.local.entity.AccessibilityServiceEntity
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
    
    override fun getLockedServices(): Flow<List<AccessibilityService>> {
        // Same as observeLockedServices but for one-time checks
        return observeLockedServices()
    }
    
    override suspend fun scanAndSyncServices(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Scan device for accessibility services
                val scannedServices = AccessibilityHelpers.scanAccessibilityServices(context)
                
                // Save to local database, preserving isLocked status
                val entitiesToInsert = mutableListOf<AccessibilityServiceEntity>()
                for (service in scannedServices) {
                    val existing = accessibilityServiceDao.getByServiceId(service.serviceId)
                    val entity = if (existing != null) {
                        // Update existing, preserving isLocked status
                        service.toEntity().copy(isLocked = existing.isLocked)
                    } else {
                        // New service
                        service.toEntity()
                    }
                    entitiesToInsert.add(entity)
                }
                accessibilityServiceDao.insertAll(entitiesToInsert)
                
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
                Timber.d("[AccessibilityRepo] Received ${lockedServices.size} locked services from backend")
                
                // Update local database with lock status
                for (dto in lockedServices) {
                    // Check actual enabled status from device
                    val componentName = android.content.ComponentName.unflattenFromString(dto.serviceId)
                    val actuallyEnabled = if (componentName != null) {
                        AccessibilityHelpers.isAccessibilityServiceEnabled(context, componentName)
                    } else {
                        false
                    }
                    
                    Timber.d("[AccessibilityRepo] Locked service: ${dto.serviceId}, enabled on device: $actuallyEnabled")
                    
                    val entity = accessibilityServiceDao.getByServiceId(dto.serviceId)
                    if (entity != null) {
                        // Update existing entity with locked status AND actual enabled status
                        accessibilityServiceDao.update(
                            entity.copy(
                                isLocked = dto.isLocked ?: true,
                                isEnabled = actuallyEnabled  // Use actual device status!
                            )
                        )
                    } else {
                        // Create new entity for locked service that doesn't exist locally
                        val newEntity = AccessibilityServiceEntity(
                            serviceId = dto.serviceId,
                            packageName = dto.packageName ?: dto.serviceId.substringBefore("/"),
                            serviceName = dto.serviceName ?: dto.serviceId.substringAfter("/"),
                            label = dto.label ?: "Unknown Service",
                            isEnabled = actuallyEnabled,  // Use actual device status!
                            isLocked = dto.isLocked ?: true
                        )
                        accessibilityServiceDao.insertAll(listOf(newEntity))
                        Timber.d("[AccessibilityRepo] Created new entity for locked service: ${dto.serviceId}")
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
    
    override suspend fun markServiceAsLocked(serviceId: String, isLocked: Boolean, isEnabled: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = accessibilityServiceDao.getByServiceId(serviceId)
                if (entity != null) {
                    // Update existing
                    accessibilityServiceDao.update(entity.copy(isLocked = isLocked, isEnabled = isEnabled))
                } else {
                    // Create new - extract package and service name from serviceId
                    val parts = serviceId.split("/")
                    val packageName = parts.getOrNull(0) ?: serviceId
                    val serviceName = parts.getOrNull(1) ?: "AccessibilityMonitor"
                    
                    val newEntity = AccessibilityServiceEntity(
                        serviceId = serviceId,
                        packageName = packageName,
                        serviceName = serviceName,
                        label = "SelfControl Accessibility Service",
                        isEnabled = isEnabled,
                        isLocked = isLocked
                    )
                    accessibilityServiceDao.insertAll(listOf(newEntity))
                }
                
                Timber.i("[AccessibilityRepo] Marked service as locked=$isLocked, enabled=$isEnabled: $serviceId")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Timber.e(e, "[AccessibilityRepo] Failed to mark service as locked")
                Result.failure(e)
            }
        }
    }
}
