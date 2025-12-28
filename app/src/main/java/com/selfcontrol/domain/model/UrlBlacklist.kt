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
     * Check if a URL matches this pattern
     */
    fun matches(targetUrl: String): Boolean {
        val matchPattern = if (pattern.isNotEmpty()) pattern else url
        if (matchPattern.isEmpty()) return false
        
        return when {
            matchPattern.startsWith("*.") -> {
                // Wildcard domain matching (e.g., *.facebook.com)
                val domain = matchPattern.substring(2)
                targetUrl.contains(domain, ignoreCase = true)
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
                    Regex(regexPattern, RegexOption.IGNORE_CASE).containsMatchIn(targetUrl)
                } catch (e: Exception) {
                    false
                }
            }
            else -> {
                // Exact or contains matching
                targetUrl.contains(matchPattern, ignoreCase = true)
            }
        }
    }
}
