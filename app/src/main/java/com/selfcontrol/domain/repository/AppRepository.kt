package com.selfcontrol.domain.repository

import com.selfcontrol.domain.model.App
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app management
 */
interface AppRepository {
    // Observe operations (Flow-based)
    fun observeAllApps(): Flow<List<App>>
    fun observeUserApps(): Flow<List<App>>
    fun observeApp(packageName: String): Flow<App?>
    
    /** Observe count of apps pending sync */
    fun observePendingSyncCount(): Flow<Int>
    
    // One-time operations
    suspend fun getApp(packageName: String): Result<App?>
    suspend fun getAppByPackageName(packageName: String): Result<App?>
    suspend fun refreshInstalledApps(): Result<List<App>>
    suspend fun saveApp(app: App): Result<Unit>
    suspend fun deleteApp(packageName: String): Result<Unit>
    suspend fun getAppCount(): Result<Int>
    suspend fun getInstalledAppsForUpload(): Result<List<App>>
    
    // ==================== Sync Queue Operations ====================
    
    /** Get current count of apps pending sync */
    suspend fun getPendingSyncCount(): Result<Int>
    
    /** Get all apps that need to be synced */
    suspend fun getPendingSyncApps(): Result<List<App>>
    
    /** Update sync status for an app */
    suspend fun updateAppSyncStatus(packageName: String, status: SyncStatus, retryCount: Int = 0): Result<Unit>
    
    /** Mark an app for immediate sync (after install/uninstall) */
    suspend fun markAppForImmediateSync(packageName: String): Result<Unit>
    
    /** Mark all apps as synced after successful bulk sync */
    suspend fun markAllAppsSynced(): Result<Unit>
    
    /** Reset failed sync apps for retry */
    suspend fun resetFailedSyncApps(): Result<Unit>
}

