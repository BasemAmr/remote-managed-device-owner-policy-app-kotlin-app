package com.selfcontrol.data.local.dao

import androidx.room.*
import com.selfcontrol.data.local.entity.AppEntity
import com.selfcontrol.data.local.entity.AppWithPolicy
import kotlinx.coroutines.flow.Flow

/**
 * DAO for app-related database operations
 */
@Dao
interface AppDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppEntity>)
    
    @Update
    suspend fun updateApp(app: AppEntity)
    
    @Delete
    suspend fun deleteApp(app: AppEntity)
    
    @Query("SELECT * FROM apps ORDER BY name ASC")
    fun observeAllApps(): Flow<List<AppEntity>>
    
    @Query("SELECT * FROM apps ORDER BY name ASC")
    suspend fun getAllApps(): List<AppEntity>
    
    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): AppEntity?
    
    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    fun observeApp(packageName: String): Flow<AppEntity?>

    @Transaction
    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    fun observeAppWithPolicy(packageName: String): Flow<AppWithPolicy?>
    
    @Query("SELECT * FROM apps WHERE isSystemApp = 0 ORDER BY name ASC")
    fun observeUserApps(): Flow<List<AppEntity>>

    @Transaction
    @Query("SELECT * FROM apps ORDER BY name ASC")
    fun observeAppsWithPolicy(): Flow<List<AppWithPolicy>>

    @Transaction
    @Query("SELECT * FROM apps WHERE isSystemApp = 0 ORDER BY name ASC")
    fun observeUserAppsWithPolicy(): Flow<List<AppWithPolicy>>
    
    @Query("SELECT * FROM apps WHERE isSystemApp = 1 ORDER BY name ASC")
    fun observeSystemApps(): Flow<List<AppEntity>>
    
    @Query("SELECT COUNT(*) FROM apps")
    suspend fun getAppCount(): Int
    
    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)
    
    @Query("DELETE FROM apps")
    suspend fun clearAll()
    
    // ==================== Sync Queue Methods ====================
    
    /** Get all apps that need to be synced (PENDING or FAILED status) */
    @Query("SELECT * FROM apps WHERE syncStatus IN ('PENDING', 'FAILED') ORDER BY lastSyncAttempt ASC")
    suspend fun getPendingSyncApps(): List<AppEntity>
    
    /** Get all apps marked for immediate sync */
    @Query("SELECT * FROM apps WHERE needsImmediateSync = 1")
    suspend fun getImmediateSyncApps(): List<AppEntity>
    
    /** Observe count of pending sync apps */
    @Query("SELECT COUNT(*) FROM apps WHERE syncStatus IN ('PENDING', 'FAILED')")
    fun observePendingSyncCount(): Flow<Int>
    
    /** Get count of pending sync apps */
    @Query("SELECT COUNT(*) FROM apps WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getPendingSyncCount(): Int
    
    /** Update sync status for a single app */
    @Query("UPDATE apps SET syncStatus = :status, syncRetryCount = :retryCount, lastSyncAttempt = :lastAttempt, needsImmediateSync = :needsImmediate WHERE packageName = :packageName")
    suspend fun updateSyncStatus(packageName: String, status: String, retryCount: Int, lastAttempt: Long, needsImmediate: Boolean)
    
    /** Mark all apps as synced */
    @Query("UPDATE apps SET syncStatus = 'SYNCED', syncRetryCount = 0, needsImmediateSync = 0")
    suspend fun markAllAsSynced()
    
    /** Mark app for immediate sync */
    @Query("UPDATE apps SET needsImmediateSync = 1, syncStatus = 'PENDING' WHERE packageName = :packageName")
    suspend fun markForImmediateSync(packageName: String)
    
    /** Reset sync status for retry */
    @Query("UPDATE apps SET syncStatus = 'PENDING', syncRetryCount = 0 WHERE syncStatus = 'FAILED'")
    suspend fun resetFailedSyncApps()
}

