package com.selfcontrol.domain.repository

import com.selfcontrol.domain.model.AccessibilityService
import kotlinx.coroutines.flow.Flow

interface AccessibilityRepository {
    fun observeAllServices(): Flow<List<AccessibilityService>>
    fun observeLockedServices(): Flow<List<AccessibilityService>>
    suspend fun scanAndSyncServices(): Result<Unit>
    suspend fun syncLockedServicesFromBackend(): Result<Unit>
    suspend fun reportServiceStatus(serviceId: String, isEnabled: Boolean): Result<Unit>
}
