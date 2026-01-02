package com.selfcontrol.presentation.urls

import com.selfcontrol.domain.model.UrlBlacklist

/**
 * UI state for the URLs screen
 */
data class UrlsState(
    val urls: List<UrlBlacklist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
