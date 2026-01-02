package com.selfcontrol.domain.repository

import com.selfcontrol.domain.model.Permission
import kotlinx.coroutines.flow.Flow

interface PermissionRepository {
    fun observeAllPermissions(): Flow<List<Permission>>
    suspend fun scanAndSyncPermissions(): Result<Unit>
}
