package com.selfcontrol.domain.usecase.url

import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.model.UrlBlacklist
import com.selfcontrol.domain.repository.UrlRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to sync URL blacklist from server
 */
class SyncUrlBlacklistUseCase @Inject constructor(
    private val urlRepository: UrlRepository
) {
    /**
     * Sync URL blacklist from server
     */
    suspend operator fun invoke(): Result<List<UrlBlacklist>> {
        Timber.i("[SyncUrlBlacklist] Starting URL blacklist sync")
        
        return when (val result = urlRepository.syncUrlBlacklist()) {
            is Result.Success -> {
                Timber.i("[SyncUrlBlacklist] Successfully synced ${result.data.size} URL patterns")
                result
            }
            
            is Result.Error -> {
                Timber.e("[SyncUrlBlacklist] Sync failed: ${result.message}")
                result
            }
            
            is Result.Loading -> result
        }
    }
    
    /**
     * Get current active URL count
     */
    suspend fun getActiveCount(): Result<Int> {
        return urlRepository.getActiveUrlCount()
    }
}
