package com.selfcontrol.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity for API request/response logging
 * Used for debugging and monitoring API interactions
 */
@Entity(
    tableName = "api_logs",
    indices = [Index("timestamp")]
)
data class ApiLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val method: String,
    val requestBody: String? = null,
    val responseCode: Int? = null,
    val responseBody: String? = null,
    val hasToken: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long, // in milliseconds
    val error: String? = null
)
