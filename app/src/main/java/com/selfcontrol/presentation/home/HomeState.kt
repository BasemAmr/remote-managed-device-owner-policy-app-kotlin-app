package com.selfcontrol.presentation.home

data class HomeState(
    val blockedAppCount: Int = 0,
    val totalAppCount: Int = 0,
    val deviceOwnerActive: Boolean = false,
    val lastSyncTime: Long = 0,
    val activeViolations: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)
