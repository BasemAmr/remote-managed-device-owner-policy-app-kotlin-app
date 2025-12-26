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
    
    @SerializedName("pattern")
    val pattern: String, // Regex or domain pattern
    
    @SerializedName("is_active")
    val isActive: Boolean,
    
    @SerializedName("created_at")
    val createdAt: Long,
    
    @SerializedName("updated_at")
    val updatedAt: Long,
    
    @SerializedName("description")
    val description: String? = null
)
