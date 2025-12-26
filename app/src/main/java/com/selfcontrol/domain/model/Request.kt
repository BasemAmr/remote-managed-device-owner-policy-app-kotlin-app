package com.selfcontrol.domain.model

/**
 * Domain model representing an access request
 */
data class Request(
    val id: String = java.util.UUID.randomUUID().toString(),
    val packageName: String = "",
    val url: String = "",
    val type: RequestType,
    val reason: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val approvedAt: Long? = null,
    val deniedAt: Long? = null,
    val isSynced: Boolean = false
) {
    /**
     * Check if request is still pending
     */
    fun isPending(): Boolean = status == RequestStatus.PENDING && !isExpired()
    
    /**
     * Check if request has expired
     */
    fun isExpired(): Boolean {
        return expiresAt?.let { it <= System.currentTimeMillis() } ?: false
    }
    
    /**
     * Get time remaining until expiration (in milliseconds)
     */
    fun getTimeRemaining(): Long? {
        return expiresAt?.let { maxOf(0, it - System.currentTimeMillis()) }
    }
}

/**
 * Types of access requests
 */
enum class RequestType {
    APP_ACCESS,
    URL_ACCESS,
    TEMPORARY_UNBLOCK,
    POLICY_OVERRIDE;
    
    fun getDisplayName(): String = when (this) {
        APP_ACCESS -> "App Access"
        URL_ACCESS -> "URL Access"
        TEMPORARY_UNBLOCK -> "Temporary Unblock"
        POLICY_OVERRIDE -> "Policy Override"
    }
}

/**
 * Status of an access request
 */
enum class RequestStatus {
    PENDING,
    APPROVED,
    DENIED,
    EXPIRED,
    CANCELLED;
    
    fun getDisplayName(): String = when (this) {
        PENDING -> "Pending"
        APPROVED -> "Approved"
        DENIED -> "Denied"
        EXPIRED -> "Expired"
        CANCELLED -> "Cancelled"
    }
    
    fun isTerminal(): Boolean = this in listOf(APPROVED, DENIED, EXPIRED, CANCELLED)
}
