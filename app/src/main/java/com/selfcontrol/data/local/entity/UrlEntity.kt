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
    indices = [Index("pattern", unique = true)]
)
data class UrlEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val pattern: String, // Regex or domain pattern
    val isActive: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val description: String? = null
)
