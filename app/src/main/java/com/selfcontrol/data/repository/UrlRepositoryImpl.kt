package com.selfcontrol.data.repository

import com.selfcontrol.data.local.dao.UrlDao
import com.selfcontrol.data.local.entity.UrlEntity
import com.selfcontrol.data.local.prefs.AppPreferences
import com.selfcontrol.data.remote.api.SelfControlApi
import com.selfcontrol.data.remote.mapper.UrlMapper
import com.selfcontrol.domain.model.UrlBlacklist
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.repository.UrlRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UrlRepository
 * Handles URL blacklist for VPN filtering
 */
@Singleton
class UrlRepositoryImpl @Inject constructor(
    private val urlDao: UrlDao,
    private val api: SelfControlApi,
    private val mapper: UrlMapper,
    private val prefs: AppPreferences
) : UrlRepository {
    
    override fun observeAllUrls(): Flow<List<UrlBlacklist>> {
        return urlDao.observeAllUrls()
            .map { entities -> entities.map { mapper.entityToDomain(it) } }
    }
    
    override fun observeBlockedUrls(): Flow<List<UrlBlacklist>> {
        return urlDao.observeActiveUrls()
            .map { entities -> entities.map { mapper.entityToDomain(it) } }
    }
    
    override suspend fun saveUrl(url: UrlBlacklist): Result<Unit> {
        return try {
            val entity = mapper.domainToEntity(url)
            urlDao.insertUrl(entity)
            Timber.i("[UrlRepo] Added URL pattern: ${url.url}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[UrlRepo] Failed to add URL")
            Result.Error(e)
        }
    }
    
    override suspend fun saveUrls(urls: List<UrlBlacklist>): Result<Unit> {
        return try {
            val entities = urls.map { mapper.domainToEntity(it) }
            urlDao.insertUrls(entities)
            Timber.d("[UrlRepo] Saved ${urls.size} URL patterns")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[UrlRepo] Failed to save URLs")
            Result.Error(e)
        }
    }
    
    override suspend fun deleteUrl(id: String): Result<Unit> {
        return try {
            urlDao.deleteById(id)
            Timber.d("[UrlRepo] Removed URL $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[UrlRepo] Failed to remove URL")
            Result.Error(e)
        }
    }

    override suspend fun syncUrlBlacklist(): Result<List<UrlBlacklist>> {
        return syncUrlsFromServer("")
    }
    
    override suspend fun syncUrlsFromServer(deviceId: String): Result<List<UrlBlacklist>> {
        return try {
            val actualDeviceId = deviceId.ifEmpty { 
                prefs.deviceId.firstOrNull() ?: return Result.Error(Exception("No device ID"))
            }
            
            val response = api.getUrlBlacklist()
            
            if (response.success && response.data != null) {
                val urls = response.data.map { dto -> mapper.dtoToDomain(dto) }
                
                // Save to local database
                saveUrls(urls)
                
                // Update sync timestamp
                prefs.setLastUrlSync(System.currentTimeMillis())
                
                Timber.i("[UrlRepo] Synced ${urls.size} URL patterns from server")
                Result.Success(urls)
            } else {
                Result.Error(Exception(response.message ?: "Failed to sync URLs"))
            }
            
        } catch (e: Exception) {
            Timber.e(e, "[UrlRepo] Failed to sync URL blacklist")
            Result.Error(e)
        }
    }
    
    override suspend fun isUrlBlocked(url: String): Result<Boolean> {
        return try {
            val activeUrls = urlDao.getActiveUrls()
            
            // Check if URL matches any pattern
            val isBlocked = activeUrls.any { entity ->
                matchesPattern(url, entity.pattern)
            }
            Result.Success(isBlocked)
        } catch (e: Exception) {
            Timber.e(e, "[UrlRepo] Failed to check URL")
            Result.Error(e)
        }
    }

    override suspend fun getActiveUrlCount(): Result<Int> {
        return try {
            val count = urlDao.getActiveUrlCount()
            Result.Success(count)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    // ==================== Helper Methods ====================
    
    private fun matchesPattern(urlToCheck: String, pattern: String): Boolean {
        return try {
            // Create UrlBlacklist to use its matches() method
            val blacklistItem = UrlBlacklist(url = pattern, pattern = pattern)
            blacklistItem.matches(urlToCheck)
        } catch (e: Exception) {
            Timber.w(e, "[UrlRepo] Pattern matching error for: $pattern")
            false
        }
    }
}
