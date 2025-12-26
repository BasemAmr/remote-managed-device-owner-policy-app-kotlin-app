package com.selfcontrol.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for installed applications
 */
@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey
    val packageName: String,
    val name: String,
    val iconUrl: String? = null,
    val isSystemApp: Boolean,
    val version: String,
    val installTime: Long,
    val lastUpdated: Long = System.currentTimeMillis()
)
