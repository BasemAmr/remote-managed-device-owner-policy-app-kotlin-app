package com.selfcontrol.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.selfcontrol.data.local.entity.ApiLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for API log operations
 */
@Dao
interface ApiLogDao {
    
    /**
     * Insert a new API log entry
     */
    @Insert
    suspend fun insertLog(log: ApiLogEntity)
    
    /**
     * Get all API logs, ordered by timestamp (newest first)
     * Limited to 100 most recent logs
     */
    @Query("SELECT * FROM api_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogs(): Flow<List<ApiLogEntity>>
    
    /**
     * Get only error logs
     */
    @Query("SELECT * FROM api_logs WHERE error IS NOT NULL ORDER BY timestamp DESC")
    fun getErrorLogs(): Flow<List<ApiLogEntity>>
    
    /**
     * Delete old logs (older than cutoff timestamp)
     */
    @Query("DELETE FROM api_logs WHERE timestamp < :cutoffTime")
    suspend fun deleteOldLogs(cutoffTime: Long)
    
    /**
     * Delete all logs
     */
    @Query("DELETE FROM api_logs")
    suspend fun deleteAllLogs()
    
    /**
     * Get logs count
     */
    @Query("SELECT COUNT(*) FROM api_logs")
    suspend fun getLogsCount(): Int
}
