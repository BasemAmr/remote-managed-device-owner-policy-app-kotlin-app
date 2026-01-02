package com.selfcontrol.data.local.dao

import androidx.room.*
import com.selfcontrol.data.local.entity.PermissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PermissionDao {
    @Query("SELECT * FROM device_permissions ORDER BY permission_name")
    fun observeAll(): Flow<List<PermissionEntity>>
    
    @Query("SELECT * FROM device_permissions WHERE permission_name = :name")
    suspend fun getByName(name: String): PermissionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(permissions: List<PermissionEntity>)
    
    @Query("DELETE FROM device_permissions")
    suspend fun deleteAll()
}
