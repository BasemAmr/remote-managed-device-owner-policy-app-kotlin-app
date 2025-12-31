package com.selfcontrol.presentation.home

/**
 * UI state for the Home screen
 */
data class HomeState(
    val blockedAppCount: Int = 0,
    val totalAppCount: Int = 0,
    val deviceOwnerActive: Boolean = false,
    val vpnConnected: Boolean = false,
    val lastSyncTime: Long = 0,
    val activeViolations: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // ==================== Sync Queue Status ====================
    /** Number of apps pending sync */
    val pendingSyncCount: Int = 0,
    /** Whether apps are currently being synced */
    val isSyncingApps: Boolean = false,
    /** Message to show for app sync status */
    val syncStatusMessage: String? = null,
    
    // ==================== Policy Sync Status ====================
    /** Whether policies are currently being synced */
    val isSyncingPolicies: Boolean = false,
    /** Message to show for policy sync status */
    val policySyncStatusMessage: String? = null
)

