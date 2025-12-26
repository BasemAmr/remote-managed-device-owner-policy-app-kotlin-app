package com.selfcontrol.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity for access requests
 */
@Entity(
    tableName = "requests",
    indices = [Index("packageName"), Index("status")]
)
data class RequestEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appName: String,
    val reason: String,
    val status: String, // "pending", "approved", "rejected", "expired"
    val requestedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val expiresAt: Long? = null,
    val reviewerNote: String? = null
)
