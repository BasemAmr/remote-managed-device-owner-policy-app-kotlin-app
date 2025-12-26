package com.selfcontrol.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for device settings
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = 1, // Single row table
    val deviceId: String,
    val deviceName: String,
    val isDeviceOwner: Boolean,
    val cooldownHours: Int = 24,
    val autoSyncEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val vpnFilterEnabled: Boolean = false,
    val lastPolicySync: Long = 0,
    val lastUrlSync: Long = 0,
    val lastViolationSync: Long = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
