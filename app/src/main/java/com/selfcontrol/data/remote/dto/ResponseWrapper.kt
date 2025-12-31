package com.selfcontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Generic API response wrapper
 */
data class ResponseWrapper<T>(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: T? = null,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("error")
    val error: ErrorDto? = null,
    
    @SerializedName("remote_lock")
    val remoteLock: Boolean = false,
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Error details
 */
data class ErrorDto(
    @SerializedName("code")
    val code: String,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("details")
    val details: Map<String, Any>? = null
)
