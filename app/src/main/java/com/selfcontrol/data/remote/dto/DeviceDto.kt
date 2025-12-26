package com.selfcontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Device registration and status DTO
 */
data class DeviceDto(
    @SerializedName("device_id")
    val deviceId: String,
    
    @SerializedName("device_name")
    val deviceName: String,
    
    @SerializedName("model")
    val model: String,
    
    @SerializedName("manufacturer")
    val manufacturer: String,
    
    @SerializedName("android_version")
    val androidVersion: String,
    
    @SerializedName("api_level")
    val apiLevel: Int,
    
    @SerializedName("is_device_owner")
    val isDeviceOwner: Boolean,
    
    @SerializedName("last_heartbeat")
    val lastHeartbeat: Long,
    
    @SerializedName("app_version")
    val appVersion: String,
    
    @SerializedName("status")
    val status: String // "active", "inactive", "error"
)
