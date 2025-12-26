package com.selfcontrol.data.repository

import com.selfcontrol.data.local.dao.SettingsDao
import com.selfcontrol.data.local.entity.SettingsEntity
import com.selfcontrol.domain.model.DeviceSettings
import com.selfcontrol.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SettingsRepository
 * Handles device settings (single row table)
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao
) : SettingsRepository {
    
    override fun observeSettings(): Flow<DeviceSettings?> {
        return settingsDao.observeSettings()
            .map { entity -> entity?.let { entityToDomain(it) } }
    }
    
    override suspend fun getSettings(): DeviceSettings? {
        return try {
            val entity = settingsDao.getSettings()
            entity?.let { entityToDomain(it) }
        } catch (e: Exception) {
            Timber.e(e, "[SettingsRepo] Failed to get settings")
            null
        }
    }
    
    override suspend fun saveSettings(settings: DeviceSettings) {
        try {
            val entity = domainToEntity(settings)
            settingsDao.insertSettings(entity)
            Timber.d("[SettingsRepo] Saved settings")
        } catch (e: Exception) {
            Timber.e(e, "[SettingsRepo] Failed to save settings")
            throw e
        }
    }
    
    override suspend fun updateMasterSwitch(enabled: Boolean) {
        try {
            settingsDao.updateAutoSyncEnabled(enabled)
            Timber.i("[SettingsRepo] Updated master switch: $enabled")
        } catch (e: Exception) {
            Timber.e(e, "[SettingsRepo] Failed to update master switch")
            throw e
        }
    }
    
    override suspend fun updateLastSyncTime(timestamp: Long) {
        try {
            settingsDao.updateLastPolicySync(timestamp)
            Timber.d("[SettingsRepo] Updated last sync time: $timestamp")
        } catch (e: Exception) {
            Timber.e(e, "[SettingsRepo] Failed to update sync time")
            throw e
        }
    }
    
    // ==================== Mappers ====================
    
    private fun entityToDomain(entity: SettingsEntity): DeviceSettings {
        return DeviceSettings(
            deviceId = entity.deviceId,
            masterSwitchEnabled = entity.autoSyncEnabled,
            lastSyncTime = entity.lastPolicySync,
            cooldownHours = entity.cooldownHours
        )
    }
    
    private fun domainToEntity(domain: DeviceSettings): SettingsEntity {
        return SettingsEntity(
            id = 1, // Single row table
            deviceId = domain.deviceId,
            deviceName = "",
            isDeviceOwner = false,
            cooldownHours = domain.cooldownHours,
            autoSyncEnabled = domain.masterSwitchEnabled,
            notificationsEnabled = true,
            vpnFilterEnabled = false,
            lastPolicySync = domain.lastSyncTime,
            lastUrlSync = 0,
            lastViolationSync = 0,
            updatedAt = domain.updatedLocallyAt
        )
    }
}
