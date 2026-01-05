package com.selfcontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AccessibilityServiceDto(
    @SerializedName("service_id")
    val serviceId: String,
    
    @SerializedName("package_name")
    val packageName: String? = null,
    
    @SerializedName("service_name")
    val serviceName: String? = null,
    
    val label: String? = null,
    
    @SerializedName("is_enabled")
    val isEnabled: Boolean? = null,
    
    @SerializedName("is_locked")
    val isLocked: Boolean? = null
)

data class PermissionDto(
    @SerializedName("permission_name")
    val permissionName: String,
    
    @SerializedName("is_granted")
    val isGranted: Boolean
)
