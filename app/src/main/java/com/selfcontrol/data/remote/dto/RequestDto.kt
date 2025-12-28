package com.selfcontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Access request DTO
 */
data class RequestDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("device_id")
    val deviceId: String,
    
    @SerializedName("package_name")
    val packageName: String,
    
    @SerializedName("app_name")
    val appName: String,
    
    @SerializedName("type")
    val type: String, // "app_access", "url_access", etc.
    
    @SerializedName("reason")
    val reason: String,
    
    @SerializedName("status")
    val status: String, // "pending", "approved", "rejected", "expired"
    
    @SerializedName("requested_at")
    val requestedAt: Long,
    
    @SerializedName("reviewed_at")
    val reviewedAt: Long? = null,
    
    @SerializedName("expires_at")
    val expiresAt: Long? = null,
    
    @SerializedName("reviewer_note")
    val reviewerNote: String? = null
)
