package com.selfcontrol.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accessibility_services",
    indices = [Index(value = ["service_id"], unique = true)]
)
data class AccessibilityServiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "service_id")
    val serviceId: String, // ComponentName.flattenToString()
    
    @ColumnInfo(name = "package_name")
    val packageName: String,
    
    @ColumnInfo(name = "service_name")
    val serviceName: String,
    
    val label: String,
    
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = false,
    
    @ColumnInfo(name = "is_locked")
    val isLocked: Boolean = false,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
