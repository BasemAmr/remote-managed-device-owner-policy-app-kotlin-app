package com.selfcontrol.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity for URL blacklist entries
 */
@Entity(
    tableName = "urls",
    indices = [Index("pattern", unique = true), Index("url")]
)
data class UrlEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val url: String = "", // Added this
    val pattern: String, // Regex or domain pattern
    val description: String? = null,
    val deviceId: String = "", // Added this
    val isBlocked: Boolean = true, // Added this
    val isActive: Boolean = true,
    val isSynced: Boolean = false, // Added this
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
