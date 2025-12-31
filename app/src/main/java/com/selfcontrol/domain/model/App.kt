package com.selfcontrol.domain.model

/**
 * Domain model representing an installed application
 */
data class App(
    val packageName: String,
    val name: String,
    val iconUrl: String? = null,
    val isSystemApp: Boolean = false,
    val version: String = "",
    val installTime: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false,
    val isLocked: Boolean = false,
    /** Sync status with backend */
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    /** Number of sync retry attempts */
    val syncRetryCount: Int = 0
)

