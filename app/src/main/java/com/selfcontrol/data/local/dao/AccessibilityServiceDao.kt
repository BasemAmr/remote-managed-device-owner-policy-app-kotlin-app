package com.selfcontrol.data.local.dao

import androidx.room.*
import com.selfcontrol.data.local.entity.AccessibilityServiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccessibilityServiceDao {
    @Query("SELECT * FROM accessibility_services ORDER BY label")
    fun observeAll(): Flow<List<AccessibilityServiceEntity>>
    
    @Query("SELECT * FROM accessibility_services WHERE is_locked = 1")
    fun observeLocked(): Flow<List<AccessibilityServiceEntity>>
    
    @Query("SELECT * FROM accessibility_services WHERE service_id = :serviceId")
    suspend fun getByServiceId(serviceId: String): AccessibilityServiceEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(services: List<AccessibilityServiceEntity>)
    
    @Update
    suspend fun update(service: AccessibilityServiceEntity)
    
    @Query("DELETE FROM accessibility_services WHERE service_id NOT IN (:serviceIds)")
    suspend fun deleteNotIn(serviceIds: List<String>)
    
    @Query("DELETE FROM accessibility_services")
    suspend fun deleteAll()
}
