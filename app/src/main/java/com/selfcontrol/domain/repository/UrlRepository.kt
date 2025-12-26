package com.selfcontrol.domain.repository

import com.selfcontrol.domain.model.UrlBlacklist
import kotlinx.coroutines.flow.Flow

interface UrlRepository {
    fun observeAllUrls(): Flow<List<UrlBlacklist>>
    fun observeBlockedUrls(): Flow<List<UrlBlacklist>>
    suspend fun saveUrl(url: UrlBlacklist)
    suspend fun saveUrls(urls: List<UrlBlacklist>)
    suspend fun deleteUrl(id: String)
    suspend fun syncUrlsFromServer(deviceId: String): List<UrlBlacklist>
    suspend fun isUrlBlocked(url: String): Boolean
}

