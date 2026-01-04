package com.selfcontrol.presentation.accessibility

import com.selfcontrol.domain.model.AccessibilityService

data class AccessibilityServicesState(
    val services: List<AccessibilityService> = emptyList(),
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val error: String? = null
)

sealed class AccessibilityEvent {
    data object Refresh : AccessibilityEvent()
    data object ScanAndSync : AccessibilityEvent()
}
