package com.selfcontrol.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity for app blocking policies
 */
@Entity(
    tableName = "policies",
    indices = [Index(value = ["packageName"], unique = true)]
)
data class PolicyEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val isBlocked: Boolean,
    val isLocked: Boolean,
    val lockAccessibility: Boolean = false,
    val expiresAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val reason: String? = null,
    val synced: Boolean = false
)
