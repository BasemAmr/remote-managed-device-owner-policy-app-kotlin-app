package com.selfcontrol.domain.model

data class AccessibilityService(
    val serviceId: String,
    val packageName: String,
    val serviceName: String,
    val label: String,
    val isEnabled: Boolean,
    val isLocked: Boolean
)

data class Permission(
    val permissionName: String,
    val isGranted: Boolean,
    val severity: PermissionSeverity
)

enum class PermissionSeverity {
    CRITICAL,
    HIGH,
    LOW
}
