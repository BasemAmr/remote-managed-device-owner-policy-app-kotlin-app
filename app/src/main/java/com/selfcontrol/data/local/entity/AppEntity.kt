package com.selfcontrol.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.selfcontrol.domain.model.SyncStatus

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
    val lastUpdated: Long,
    
    // ==================== Sync Queue Fields ====================
    /** Current sync status of this app */
    val syncStatus: String,
    /** Number of times sync has been retried */
    val syncRetryCount: Int,
    /** Timestamp of last sync attempt */
    val lastSyncAttempt: Long,
    /** Whether this app needs immediate sync (e.g., just installed) */
    val needsImmediateSync: Boolean
)
