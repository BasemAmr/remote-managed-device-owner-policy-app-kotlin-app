package com.selfcontrol.presentation.home

sealed class HomeEvent {
    data object Refresh : HomeEvent()
    data object GrantDeviceOwner : HomeEvent()
    data object RemoveDeviceOwner : HomeEvent()
    
    /** Manual "Sync All Apps" button clicked */
    data object SyncAllApps : HomeEvent()
    
    /** Manual "Sync All Policies" button clicked */
    data object SyncAllPolicies : HomeEvent()
    
    /** Manual "Sync URLs" button clicked */
    data object SyncAllUrls : HomeEvent()
    
    /** Manual "Sync Accessibility Services" button clicked */
    data object SyncAccessibilityServices : HomeEvent()
}