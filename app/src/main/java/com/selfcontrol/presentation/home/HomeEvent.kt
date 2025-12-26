package com.selfcontrol.presentation.home

sealed class HomeEvent {
    data object Refresh : HomeEvent()
    data object GrantDeviceOwner : HomeEvent() // For triggering instructions or checks
}
