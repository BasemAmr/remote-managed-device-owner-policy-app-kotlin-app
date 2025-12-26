package com.selfcontrol.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity for violation logs
 */
@Entity(
    tableName = "violations",
    indices = [Index("packageName"), Index("timestamp"), Index("synced")]
)
data class ViolationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appName: String,
    val violationType: String, // "app_launch_attempt", "url_access_attempt", etc.
    val timestamp: Long = System.currentTimeMillis(),
    val details: String? = null,
    val synced: Boolean = false
)
