package com.selfcontrol.presentation.apps

import com.selfcontrol.domain.model.App

data class AppsState(
    val apps: List<App> = emptyList(),
    val filteredApps: List<App> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
