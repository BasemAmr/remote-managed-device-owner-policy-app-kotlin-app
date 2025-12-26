package com.selfcontrol.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcontrol.data.local.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.deviceId,
                prefs.isDeviceOwner,
                prefs.autoSyncEnabled,
                prefs.notificationsEnabled
            ) { deviceId, isOwner, autoSync, notifs ->
                SettingsState(deviceId, isOwner, autoSync, notifs)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoSyncEnabled(enabled) }
    }
    
    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch { prefs.setNotificationsEnabled(enabled) }
    }
}
