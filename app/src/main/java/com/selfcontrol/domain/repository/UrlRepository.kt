package com.selfcontrol.domain.repository

import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.model.UrlBlacklist
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for URL blacklist management
 */
interface UrlRepository {
    fun observeAllUrls(): Flow<List<UrlBlacklist>>
    fun observeBlockedUrls(): Flow<List<UrlBlacklist>>
    suspend fun saveUrl(url: UrlBlacklist): Result<Unit>
    suspend fun saveUrls(urls: List<UrlBlacklist>): Result<Unit>
    suspend fun deleteUrl(id: String): Result<Unit>
    suspend fun syncUrlBlacklist(): Result<List<UrlBlacklist>>
    suspend fun syncUrlsFromServer(deviceId: String): Result<List<UrlBlacklist>>
    suspend fun isUrlBlocked(url: String): Result<Boolean>
    suspend fun getActiveUrlCount(): Result<Int>
}
