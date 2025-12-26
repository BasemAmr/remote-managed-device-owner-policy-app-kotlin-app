package com.selfcontrol.domain.usecase.url

import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.repository.UrlRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to check if a URL is blocked
 */
class CheckUrlBlockedUseCase @Inject constructor(
    private val urlRepository: UrlRepository
) {
    /**
     * Check if a URL matches any blacklist pattern
     * @param url URL to check
     * @return true if URL is blocked, false otherwise
     */
    suspend operator fun invoke(url: String): Result<Boolean> {
        Timber.d("[CheckUrlBlocked] Checking URL: $url")
        
        // Normalize URL
        val normalizedUrl = normalizeUrl(url)
        
        return when (val result = urlRepository.isUrlBlocked(normalizedUrl)) {
            is Result.Success -> {
                if (result.data) {
                    Timber.i("[CheckUrlBlocked] URL is blocked: $url")
                } else {
                    Timber.d("[CheckUrlBlocked] URL is allowed: $url")
                }
                result
            }
            
            is Result.Error -> {
                Timber.e("[CheckUrlBlocked] Error checking URL: ${result.message}")
                // On error, allow by default (fail-open)
                Result.Success(false)
            }
            
            is Result.Loading -> result
        }
    }
    
    /**
     * Batch check multiple URLs
     */
    suspend fun checkMultiple(urls: List<String>): Result<Map<String, Boolean>> {
        Timber.d("[CheckUrlBlocked] Batch checking ${urls.size} URLs")
        
        val results = mutableMapOf<String, Boolean>()
        
        for (url in urls) {
            when (val result = invoke(url)) {
                is Result.Success -> results[url] = result.data
                is Result.Error -> results[url] = false // Fail-open
                is Result.Loading -> {} // Skip
            }
        }
        
        return Result.Success(results)
    }
    
    /**
     * Normalize URL for consistent checking
     */
    private fun normalizeUrl(url: String): String {
        var normalized = url.trim().lowercase()
        
        // Remove protocol
        normalized = normalized
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")
        
        // Remove trailing slash
        normalized = normalized.trimEnd('/')
        
        return normalized
    }
}
