package com.selfcontrol.domain.repository

import com.selfcontrol.domain.model.App
import com.selfcontrol.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app management
 */
interface AppRepository {
    // Observe operations (Flow-based)
    fun observeAllApps(): Flow<List<App>>
    fun observeUserApps(): Flow<List<App>>
    fun observeApp(packageName: String): Flow<App?>
    
    // One-time operations
    suspend fun getApp(packageName: String): Result<App?>
    suspend fun getAppByPackageName(packageName: String): App?
    suspend fun refreshInstalledApps(): Result<List<App>>
    suspend fun saveApp(app: App): Result<Unit>
    suspend fun deleteApp(packageName: String): Result<Unit>
    suspend fun getAppCount(): Result<Int>
}
