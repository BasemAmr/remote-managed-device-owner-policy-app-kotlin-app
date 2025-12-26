package com.selfcontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Policy DTO for app blocking rules
 */
data class PolicyDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("device_id")
    val deviceId: String,
    
    @SerializedName("package_name")
    val packageName: String,
    
    @SerializedName("is_blocked")
    val isBlocked: Boolean,
    
    @SerializedName("is_locked")
    val isLocked: Boolean,
    
    @SerializedName("expires_at")
    val expiresAt: Long? = null,
    
    @SerializedName("created_at")
    val createdAt: Long,
    
    @SerializedName("updated_at")
    val updatedAt: Long,
    
    @SerializedName("reason")
    val reason: String? = null
)
