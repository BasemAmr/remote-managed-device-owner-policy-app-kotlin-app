package com.selfcontrol.data.local.dao

import androidx.room.*
import com.selfcontrol.data.local.entity.ViolationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for violation-related database operations
 */
@Dao
interface ViolationDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertViolation(violation: ViolationEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertViolations(violations: List<ViolationEntity>)
    
    @Update
    suspend fun updateViolation(violation: ViolationEntity)
    
    @Delete
    suspend fun deleteViolation(violation: ViolationEntity)
    
    @Query("SELECT * FROM violations ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentViolations(limit: Int = 100): Flow<List<ViolationEntity>>
    
    @Query("SELECT * FROM violations ORDER BY timestamp DESC")
    fun observeAllViolations(): Flow<List<ViolationEntity>>
    
    @Query("SELECT * FROM violations WHERE id = :violationId LIMIT 1")
    suspend fun getViolation(violationId: String): ViolationEntity?
    
    @Query("SELECT * FROM violations WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun observeViolationsForApp(packageName: String): Flow<List<ViolationEntity>>
    
    @Query("SELECT * FROM violations WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedViolations(): List<ViolationEntity>
    
    @Query("SELECT * FROM violations WHERE synced = 0")
    fun observeUnsyncedViolations(): Flow<List<ViolationEntity>>
    
    @Query("SELECT * FROM violations WHERE violationType = :type ORDER BY timestamp DESC")
    fun observeViolationsByType(type: String): Flow<List<ViolationEntity>>
    
    @Query("SELECT COUNT(*) FROM violations")
    suspend fun getViolationCount(): Int
    
    @Query("SELECT COUNT(*) FROM violations WHERE synced = 0")
    suspend fun getUnsyncedCount(): Int
    
    @Query("UPDATE violations SET synced = 1 WHERE id IN (:violationIds)")
    suspend fun markAsSynced(violationIds: List<String>)
    
    @Query("DELETE FROM violations WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
    
    @Query("DELETE FROM violations")
    suspend fun clearAll()
}
