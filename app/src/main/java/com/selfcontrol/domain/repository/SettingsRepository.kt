package com.selfcontrol.domain.repository

import com.selfcontrol.domain.model.DeviceSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<DeviceSettings?>
    suspend fun getSettings(): DeviceSettings?
    suspend fun saveSettings(settings: DeviceSettings)
    suspend fun updateMasterSwitch(enabled: Boolean)
    suspend fun updateLastSyncTime(timestamp: Long)
}

