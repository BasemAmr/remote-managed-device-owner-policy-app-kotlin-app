package com.selfcontrol.domain.model

import java.util.UUID

/**
 * Domain model representing a URL blacklist entry
 */
data class UrlBlacklist(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val pattern: String = "",
    val description: String = "",
    val deviceId: String = "",
    val isBlocked: Boolean = true,
    val isActive: Boolean = true,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Normalize a URL to extract just the domain portion
     */
    private fun normalizeDomain(url: String): String {
        var normalized = url.lowercase().trim()
        
        // Strip protocol
        normalized = normalized.removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
        
        // Strip path and query parameters
        val pathIndex = normalized.indexOf('/')
        if (pathIndex != -1) {
            normalized = normalized.substring(0, pathIndex)
        }
        
        val queryIndex = normalized.indexOf('?')
        if (queryIndex != -1) {
            normalized = normalized.substring(0, queryIndex)
        }
        
        return normalized
    }
    
    /**
     * Check if a domain matches another domain or is a subdomain of it
     * e.g., "www.facebook.com" matches "facebook.com"
     * but "notfacebook.com" does NOT match "facebook.com"
     */
    private fun isDomainMatch(targetDomain: String, patternDomain: String): Boolean {
        if (targetDomain == patternDomain) return true
        
        // Check if target is a subdomain of pattern
        // e.g., www.facebook.com should match facebook.com
        if (targetDomain.endsWith(".$patternDomain")) return true
        
        return false
    }
    
    /**
     * Check if a URL matches this pattern
     */
    fun matches(targetUrl: String): Boolean {
        val matchPattern = if (pattern.isNotEmpty()) pattern else url
        if (matchPattern.isEmpty()) return false
        
        // Normalize the target URL to extract domain
        val normalizedTarget = normalizeDomain(targetUrl)
        
        return when {
            matchPattern.startsWith("*.") -> {
                // Wildcard subdomain matching (e.g., *.facebook.com)
                // Should match www.facebook.com, m.facebook.com
                // Should NOT match facebook.com itself
                val domain = matchPattern.substring(2).lowercase()
                
                // Target must end with .domain (note the leading dot)
                // This ensures facebook.com doesn't match *.facebook.com
                normalizedTarget.endsWith(".$domain")
            }
            matchPattern.startsWith("regex:") -> {
                // Regex pattern matching
                try {
                    val regex = matchPattern.substring(6).toRegex(RegexOption.IGNORE_CASE)
                    regex.containsMatchIn(targetUrl)
                } catch (e: Exception) {
                    false
                }
            }
            matchPattern.contains("*") -> {
                // Simple wildcard matching (e.g., *porn*, facebook*)
                val regexPattern = matchPattern
                    .replace(".", "\\.")
                    .replace("*", ".*")
                try {
                    Regex(regexPattern, RegexOption.IGNORE_CASE).containsMatchIn(normalizedTarget)
                } catch (e: Exception) {
                    false
                }
            }
            else -> {
                // Exact domain matching
                // facebook.com should match facebook.com and www.facebook.com
                // but NOT notfacebook.com
                val normalizedPattern = normalizeDomain(matchPattern)
                isDomainMatch(normalizedTarget, normalizedPattern)
            }
        }
    }
}
