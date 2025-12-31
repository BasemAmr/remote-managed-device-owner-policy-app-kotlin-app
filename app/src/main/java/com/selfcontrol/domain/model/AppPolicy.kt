package com.selfcontrol.domain.model

/**
 * Domain model representing an app blocking policy
 */
data class AppPolicy(
    val id: String = java.util.UUID.randomUUID().toString(),
    val packageName: String,
    val isBlocked: Boolean = false,
    val isLocked: Boolean = false,
    val lockAccessibility: Boolean = false,
    val reason: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
) {
    /**
     * Check if policy is currently active
     */
    fun isActive(): Boolean {
        return expiresAt?.let { it > System.currentTimeMillis() } ?: true
    }
    
    /**
     * Check if policy has expired
     */
    fun isExpired(): Boolean {
        return expiresAt?.let { it <= System.currentTimeMillis() } ?: false
    }
    
    /**
     * Check if app should be blocked based on this policy
     */
    fun shouldBlock(): Boolean {
        return isActive() && (isBlocked || isLocked)
    }
}
