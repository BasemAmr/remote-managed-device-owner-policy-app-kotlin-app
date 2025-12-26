package com.selfcontrol.data.local.dao

import androidx.room.*
import com.selfcontrol.data.local.entity.RequestEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for request-related database operations
 */
@Dao
interface RequestDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: RequestEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequests(requests: List<RequestEntity>)
    
    @Update
    suspend fun updateRequest(request: RequestEntity)
    
    @Delete
    suspend fun deleteRequest(request: RequestEntity)
    
    @Query("SELECT * FROM requests ORDER BY requestedAt DESC")
    fun observeAllRequests(): Flow<List<RequestEntity>>
    
    @Query("SELECT * FROM requests WHERE id = :requestId LIMIT 1")
    suspend fun getRequest(requestId: String): RequestEntity?
    
    @Query("SELECT * FROM requests WHERE id = :requestId LIMIT 1")
    fun observeRequest(requestId: String): Flow<RequestEntity?>
    
    @Query("SELECT * FROM requests WHERE status = :status ORDER BY requestedAt DESC")
    fun observeRequestsByStatus(status: String): Flow<List<RequestEntity>>
    
    @Query("SELECT * FROM requests WHERE status = 'pending' ORDER BY requestedAt DESC")
    fun observePendingRequests(): Flow<List<RequestEntity>>
    
    @Query("SELECT * FROM requests WHERE status = 'pending'")
    suspend fun getPendingRequests(): List<RequestEntity>
    
    @Query("SELECT * FROM requests WHERE packageName = :packageName ORDER BY requestedAt DESC")
    fun observeRequestsForApp(packageName: String): Flow<List<RequestEntity>>
    
    @Query("SELECT COUNT(*) FROM requests WHERE status = 'pending'")
    suspend fun getPendingCount(): Int
    
    @Query("DELETE FROM requests WHERE id = :requestId")
    suspend fun deleteById(requestId: String)
    
    @Query("DELETE FROM requests")
    suspend fun clearAll()
}
