package com.selfcontrol.domain.model

import java.util.UUID

/**
 * Domain model representing a URL blacklist entry
 */
data class UrlBlacklist(
    val id: String = UUID.randomUUID().toString(),
    val pattern: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val description: String = ""
) {
    /**
     * Check if a URL matches this pattern
     */
    fun matches(url: String): Boolean {
        return when {
            pattern.startsWith("*.") -> {
                // Wildcard domain matching (e.g., *.facebook.com)
                val domain = pattern.substring(2)
                url.contains(domain, ignoreCase = true)
            }
            pattern.startsWith("regex:") -> {
                // Regex pattern matching
                try {
                    val regex = pattern.substring(6).toRegex(RegexOption.IGNORE_CASE)
                    regex.containsMatchIn(url)
                } catch (e: Exception) {
                    false
                }
            }
            pattern.contains("*") -> {
                // Simple wildcard matching (e.g., *porn*, facebook*)
                val regexPattern = pattern
                    .replace(".", "\\.")
                    .replace("*", ".*")
                try {
                    Regex(regexPattern, RegexOption.IGNORE_CASE).containsMatchIn(url)
                } catch (e: Exception) {
                    false
                }
            }
            else -> {
                // Exact or contains matching
                url.contains(pattern, ignoreCase = true)
            }
        }
    }
}

