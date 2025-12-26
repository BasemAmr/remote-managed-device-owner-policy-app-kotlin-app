package com.selfcontrol.data.local.dao

import androidx.room.*
import com.selfcontrol.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for settings database operations (single row table)
 */
@Dao
interface SettingsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity)
    
    @Update
    suspend fun updateSettings(settings: SettingsEntity)
    
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): SettingsEntity?
    
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<SettingsEntity?>
    
    @Query("UPDATE settings SET lastPolicySync = :timestamp WHERE id = 1")
    suspend fun updateLastPolicySync(timestamp: Long)
    
    @Query("UPDATE settings SET lastUrlSync = :timestamp WHERE id = 1")
    suspend fun updateLastUrlSync(timestamp: Long)
    
    @Query("UPDATE settings SET lastViolationSync = :timestamp WHERE id = 1")
    suspend fun updateLastViolationSync(timestamp: Long)
    
    @Query("UPDATE settings SET isDeviceOwner = :isOwner WHERE id = 1")
    suspend fun updateDeviceOwnerStatus(isOwner: Boolean)
    
    @Query("UPDATE settings SET cooldownHours = :hours WHERE id = 1")
    suspend fun updateCooldownHours(hours: Int)
    
    @Query("UPDATE settings SET vpnFilterEnabled = :enabled WHERE id = 1")
    suspend fun updateVpnFilterEnabled(enabled: Boolean)
    
    @Query("UPDATE settings SET autoSyncEnabled = :enabled WHERE id = 1")
    suspend fun updateAutoSyncEnabled(enabled: Boolean)
    
    @Query("DELETE FROM settings")
    suspend fun clearAll()
}
