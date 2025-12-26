package com.selfcontrol.data.local.dao

import androidx.room.*
import com.selfcontrol.data.local.entity.AppEntity
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
    
    @Query("SELECT * FROM apps WHERE isSystemApp = 0 ORDER BY name ASC")
    fun observeUserApps(): Flow<List<AppEntity>>
    
    @Query("SELECT * FROM apps WHERE isSystemApp = 1 ORDER BY name ASC")
    fun observeSystemApps(): Flow<List<AppEntity>>
    
    @Query("SELECT COUNT(*) FROM apps")
    suspend fun getAppCount(): Int
    
    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)
    
    @Query("DELETE FROM apps")
    suspend fun clearAll()
}
