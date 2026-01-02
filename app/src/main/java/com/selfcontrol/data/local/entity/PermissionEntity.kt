package com.selfcontrol.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "device_permissions",
    indices = [Index(value = ["permission_name"], unique = true)]
)
data class PermissionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "permission_name")
    val permissionName: String,
    
    @ColumnInfo(name = "is_granted")
    val isGranted: Boolean = false,
    
    @ColumnInfo(name = "last_checked")
    val lastChecked: Long = System.currentTimeMillis()
)
