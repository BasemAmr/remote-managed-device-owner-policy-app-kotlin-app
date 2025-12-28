package com.selfcontrol.domain.model

/**
 * Domain model representing a managed device
 */
data class Device(
    val id: Int = 1,
    val deviceId: String,
    val deviceName: String,
    val isDeviceOwner: Boolean = false
)
