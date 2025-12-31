package com.selfcontrol.domain.model

/**
 * Enum representing the sync status of an app
 */
enum class SyncStatus {
    /** App is synced with backend */
    SYNCED,
    
    /** App is pending sync (waiting to be synced) */
    PENDING,
    
    /** Sync failed after all retries */
    FAILED,
    
    /** App is currently being synced */
    SYNCING
}
