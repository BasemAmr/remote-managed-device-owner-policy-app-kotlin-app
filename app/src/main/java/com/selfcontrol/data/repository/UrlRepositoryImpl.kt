package com.selfcontrol.data.repository

import com.selfcontrol.data.local.dao.UrlDao
import com.selfcontrol.data.local.entity.UrlEntity
import com.selfcontrol.data.local.prefs.AppPreferences
import com.selfcontrol.data.remote.api.SelfControlApi
import com.selfcontrol.domain.model.UrlBlacklist
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
    private val prefs: AppPreferences
) : UrlRepository {
    
    override fun observeAllUrls(): Flow<List<UrlBlacklist>> {
        return urlDao.observeAllUrls()
            .map { entities -> entities.map { entityToDomain(it) } }
    }
    
    override fun observeBlockedUrls(): Flow<List<UrlBlacklist>> {
        return urlDao.observeActiveUrls()
            .map { entities -> entities.map { entityToDomain(it) } }
    }
    
    override suspend fun saveUrl(url: UrlBlacklist) {
        try {
            val entity = domainToEntity(url)
            urlDao.insertUrl(entity)
            Timber.i("[UrlRepo] Added URL pattern: ${url.pattern}")
        } catch (e: Exception) {
            Timber.e(e, "[UrlRepo] Failed to add URL")
            throw e
        }
    }
    
    override suspend fun saveUrls(urls: List<UrlBlacklist>) {
        try {
            val entities = urls.map { domainToEntity(it) }
            urlDao.insertUrls(entities)
            Timber.d("[UrlRepo] Saved ${urls.size} URL patterns")
        } catch (e: Exception) {
            Timber.e(e, "[UrlRepo] Failed to save URLs")
            throw e
        }
    }
    
    override suspend fun deleteUrl(id: String) {
        try {
            urlDao.deleteById(id)
            Timber.d("[UrlRepo] Removed URL $id")
        } catch (e: Exception) {
            Timber.e(e, "[UrlRepo] Failed to remove URL")
            throw e
        }
    }
    
    override suspend fun syncUrlsFromServer(deviceId: String): List<UrlBlacklist> {
        return try {
            val actualDeviceId = deviceId.ifEmpty { 
                prefs.deviceId.firstOrNull() ?: throw Exception("No device ID")
            }
            
            val response = api.getUrlBlacklist(actualDeviceId)
            
            if (response.success && response.data != null) {
                val urls = response.data.map { dto ->
                    UrlBlacklist(
                        id = dto.id,
                        pattern = dto.pattern,
                        isActive = dto.isActive,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        description = dto.description
                    )
                }
                
                // Save to local database
                saveUrls(urls)
                
                // Update sync timestamp
                prefs.setLastUrlSync(System.currentTimeMillis())
                
                Timber.i("[UrlRepo] Synced ${urls.size} URL patterns from server")
                urls
            } else {
                throw Exception(response.message ?: "Failed to sync URLs")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "[UrlRepo] Failed to sync URL blacklist")
            throw e
        }
    }
    
    override suspend fun isUrlBlocked(url: String): Boolean {
        return try {
            val activeUrls = urlDao.getActiveUrls()
            
            // Check if URL matches any pattern
            activeUrls.any { entity ->
                matchesPattern(url, entity.pattern)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "[UrlRepo] Failed to check URL")
            false
        }
    }
    
    // ==================== Helper Methods ====================
    
    private fun matchesPattern(url: String, pattern: String): Boolean {
        return try {
            // Create UrlBlacklist to use its matches() method
            val blacklistItem = UrlBlacklist(pattern = pattern)
            blacklistItem.matches(url)
        } catch (e: Exception) {
            Timber.w(e, "[UrlRepo] Pattern matching error for: $pattern")
            false
        }
    }
    
    // ==================== Mappers ====================
    
    private fun entityToDomain(entity: UrlEntity): UrlBlacklist {
        return UrlBlacklist(
            id = entity.id,
            pattern = entity.pattern,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            description = entity.description
        )
    }
    
    private fun domainToEntity(domain: UrlBlacklist): UrlEntity {
        return UrlEntity(
            id = domain.id,
            pattern = domain.pattern,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            description = domain.description
        )
    }
}
