package com.selfcontrol.domain.model

/**
 * Domain model representing an installed application
 */
data class App(
    val packageName: String,
    val name: String,
    val iconUrl: String = "",
    val isSystemApp: Boolean = false,
    val version: String = "",
    val installTime: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false
)
