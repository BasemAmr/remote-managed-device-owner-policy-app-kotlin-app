package com.selfcontrol.data.local.dao

import androidx.room.*
import com.selfcontrol.data.local.entity.PolicyEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for policy-related database operations
 */
@Dao
interface PolicyDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolicy(policy: PolicyEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolicies(policies: List<PolicyEntity>)
    
    @Update
    suspend fun updatePolicy(policy: PolicyEntity)
    
    @Delete
    suspend fun deletePolicy(policy: PolicyEntity)
    
    @Query("SELECT * FROM policies ORDER BY updatedAt DESC")
    fun observeAllPolicies(): Flow<List<PolicyEntity>>
    
    @Query("SELECT * FROM policies ORDER BY updatedAt DESC")
    suspend fun getAllPolicies(): List<PolicyEntity>
    
    @Query("SELECT * FROM policies WHERE id = :policyId LIMIT 1")
    suspend fun getPolicy(policyId: String): PolicyEntity?
    
    @Query("SELECT * FROM policies WHERE packageName = :packageName LIMIT 1")
    suspend fun getPolicyForApp(packageName: String): PolicyEntity?
    
    @Query("SELECT * FROM policies WHERE packageName = :packageName LIMIT 1")
    fun observePolicyForApp(packageName: String): Flow<PolicyEntity?>
    
    @Query("SELECT * FROM policies WHERE isBlocked = 1 ORDER BY updatedAt DESC")
    fun observeBlockedPolicies(): Flow<List<PolicyEntity>>
    
    @Query("SELECT * FROM policies WHERE isBlocked = 1")
    suspend fun getBlockedPolicies(): List<PolicyEntity>
    
    @Query("SELECT * FROM policies WHERE isLocked = 1 ORDER BY updatedAt DESC")
    fun observeLockedPolicies(): Flow<List<PolicyEntity>>
    
    @Query("SELECT * FROM policies WHERE expiresAt IS NOT NULL AND expiresAt > :currentTime")
    suspend fun getActivePoliciesWithExpiration(currentTime: Long = System.currentTimeMillis()): List<PolicyEntity>
    
    @Query("SELECT COUNT(*) FROM policies WHERE isBlocked = 1")
    suspend fun getBlockedCount(): Int
    
    @Query("SELECT * FROM policies WHERE synced = 0")
    suspend fun getUnsyncedPolicies(): List<PolicyEntity>
    
    @Query("UPDATE policies SET synced = 1 WHERE id IN (:policyIds)")
    suspend fun markAsSynced(policyIds: List<String>)
    
    @Query("DELETE FROM policies WHERE id = :policyId")
    suspend fun deleteById(policyId: String)
    
    @Query("DELETE FROM policies WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)
    
    @Query("DELETE FROM policies")
    suspend fun clearAll()
}
