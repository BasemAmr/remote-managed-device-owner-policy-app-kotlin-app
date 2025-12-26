package com.selfcontrol.domain.model

/**
 * Domain model representing a violation event
 */
data class Violation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val packageName: String = "",
    val appName: String = "",
    val url: String = "",
    val type: ViolationType,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val details: String = "",
    val synced: Boolean = false
)

/**
 * Types of violations that can occur
 */
enum class ViolationType {
    APP_LAUNCH_ATTEMPT,
    URL_ACCESS_ATTEMPT,
    POLICY_ENFORCEMENT_FAILED,
    UNAUTHORIZED_UNINSTALL_ATTEMPT,
    DEVICE_OWNER_DISABLED,
    VPN_BYPASS_ATTEMPT,
    BLOCKED_APP_ACCESS_ATTEMPT,
    ACCESSIBILITY_SERVICE_DISABLED,
    UNKNOWN;
    
    fun getDisplayName(): String = when (this) {
        APP_LAUNCH_ATTEMPT -> "App Launch Attempt"
        URL_ACCESS_ATTEMPT -> "URL Access Attempt"
        POLICY_ENFORCEMENT_FAILED -> "Policy Enforcement Failed"
        UNAUTHORIZED_UNINSTALL_ATTEMPT -> "Unauthorized Uninstall Attempt"
        DEVICE_OWNER_DISABLED -> "Device Owner Disabled"
        VPN_BYPASS_ATTEMPT -> "VPN Bypass Attempt"
        BLOCKED_APP_ACCESS_ATTEMPT -> "Blocked App Access Attempt"
        ACCESSIBILITY_SERVICE_DISABLED -> "Accessibility Service Disabled"
        UNKNOWN -> "Unknown Violation"
    }
}

