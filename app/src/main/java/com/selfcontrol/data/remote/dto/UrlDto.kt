package com.selfcontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * URL blacklist entry DTO
 */
data class UrlDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("device_id")
    val deviceId: String,
    
    @SerializedName("url")
    val url: String? = null,
    
    @SerializedName("pattern")
    val pattern: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    // Make these nullable - backend doesn't always send them
    // Gson will set null when missing, we handle defaults in mapper
    @SerializedName("is_blocked")
    val isBlocked: Boolean? = null,
    
    @SerializedName("is_active")
    val isActive: Boolean? = null,
    
    @SerializedName("created_at")
    val createdAt: Long,
    
    @SerializedName("updated_at")
    val updatedAt: Long
)
