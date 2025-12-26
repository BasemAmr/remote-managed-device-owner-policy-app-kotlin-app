package com.selfcontrol.presentation.settings

data class SettingsState(
    val deviceId: String = "",
    val isDeviceOwner: Boolean = false,
    val autoSyncEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true
)
