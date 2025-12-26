package com.selfcontrol.presentation.requests

data class RequestsState(
    val pendingRequests: List<com.selfcontrol.domain.model.Request> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
