package com.selfcontrol.data.local.dao

import androidx.room.*
import com.selfcontrol.data.local.entity.UrlEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for URL blacklist database operations
 */
@Dao
interface UrlDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUrl(url: UrlEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUrls(urls: List<UrlEntity>)
    
    @Update
    suspend fun updateUrl(url: UrlEntity)
    
    @Delete
    suspend fun deleteUrl(url: UrlEntity)
    
    @Query("SELECT * FROM urls ORDER BY createdAt DESC")
    fun observeAllUrls(): Flow<List<UrlEntity>>
    
    @Query("SELECT * FROM urls WHERE isActive = 1 ORDER BY createdAt DESC")
    fun observeActiveUrls(): Flow<List<UrlEntity>>
    
    @Query("SELECT * FROM urls WHERE isActive = 1")
    suspend fun getActiveUrls(): List<UrlEntity>
    
    @Query("SELECT * FROM urls WHERE id = :urlId LIMIT 1")
    suspend fun getUrl(urlId: String): UrlEntity?
    
    @Query("SELECT * FROM urls WHERE pattern = :pattern LIMIT 1")
    suspend fun getUrlByPattern(pattern: String): UrlEntity?
    
    @Query("SELECT COUNT(*) FROM urls WHERE isActive = 1")
    suspend fun getActiveUrlCount(): Int
    
    @Query("DELETE FROM urls WHERE id = :urlId")
    suspend fun deleteById(urlId: String)
    
    @Query("DELETE FROM urls")
    suspend fun clearAll()
}
