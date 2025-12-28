package com.selfcontrol.domain.model

/**
 * Domain model representing device settings
 */
data class DeviceSettings(
    val deviceId: String,
    val masterSwitchEnabled: Boolean = true,
    val lastSyncTime: Long = 0,
    val lastAppSyncTime: Long = 0,
    val cooldownHours: Int = 24,
    val updatedLocallyAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if enough time has passed since last sync
     */
    fun shouldSync(intervalMillis: Long): Boolean {
        return System.currentTimeMillis() - lastSyncTime >= intervalMillis
    }
    
    /**
     * Check if cooldown period has passed
     */
    fun isCooldownActive(lastActionTime: Long): Boolean {
        val cooldownMillis = cooldownHours * 60 * 60 * 1000L
        return System.currentTimeMillis() - lastActionTime < cooldownMillis
    }
}
