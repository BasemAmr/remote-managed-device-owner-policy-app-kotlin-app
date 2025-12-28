package com.selfcontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Violation log DTO
 */
data class ViolationDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("device_id")
    val deviceId: String,
    
    @SerializedName("package_name")
    val packageName: String,
    
    @SerializedName("app_name")
    val appName: String,
    
    @SerializedName("violation_type")
    val violationType: String, // "app_launch_attempt", "url_access_attempt", "policy_bypass_attempt"
    
    @SerializedName("message")
    val message: String = "", // Added this
    
    @SerializedName("timestamp")
    val timestamp: Long,
    
    @SerializedName("details")
    val details: String? = null,
    
    @SerializedName("synced")
    val synced: Boolean = false
)
