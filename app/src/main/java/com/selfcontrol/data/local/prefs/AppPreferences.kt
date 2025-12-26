package com.selfcontrol.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// Extension property to create DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/**
 * DataStore-based preferences for app settings
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val dataStore = context.dataStore
    
    companion object {
        private val DEVICE_ID = stringPreferencesKey("device_id")
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val IS_DEVICE_OWNER = booleanPreferencesKey("is_device_owner")
        private val COOLDOWN_HOURS = intPreferencesKey("cooldown_hours")
        private val LAST_POLICY_SYNC = longPreferencesKey("last_policy_sync")
        private val LAST_URL_SYNC = longPreferencesKey("last_url_sync")
        private val LAST_VIOLATION_SYNC = longPreferencesKey("last_violation_sync")
        private val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val VPN_FILTER_ENABLED = booleanPreferencesKey("vpn_filter_enabled")
    }
    
    // ==================== Device ID ====================
    
    val deviceId: Flow<String> = dataStore.data
        .catch { handleException(it) }
        .map { prefs ->
            prefs[DEVICE_ID] ?: UUID.randomUUID().toString().also { newId ->
                setDeviceId(newId)
            }
        }
    
    suspend fun setDeviceId(id: String) {
        dataStore.edit { prefs ->
            prefs[DEVICE_ID] = id
        }
    }
    
    // ==================== Auth Token ====================
    
    val authToken: Flow<String?> = dataStore.data
        .catch { handleException(it) }
        .map { prefs -> prefs[AUTH_TOKEN] }
    
    suspend fun setAuthToken(token: String) {
        dataStore.edit { prefs ->
            prefs[AUTH_TOKEN] = token
        }
    }
    
    suspend fun clearAuthToken() {
        dataStore.edit { prefs ->
            prefs.remove(AUTH_TOKEN)
        }
    }
    
    // ==================== Device Owner Status ====================
    
    val isDeviceOwner: Flow<Boolean> = dataStore.data
        .catch { handleException(it) }
        .map { prefs -> prefs[IS_DEVICE_OWNER] ?: false }
    
    suspend fun setDeviceOwner(isOwner: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_DEVICE_OWNER] = isOwner
        }
    }
    
    // ==================== Cooldown Hours ====================
    
    val cooldownHours: Flow<Int> = dataStore.data
        .catch { handleException(it) }
        .map { prefs -> prefs[COOLDOWN_HOURS] ?: 24 }
    
    suspend fun setCooldownHours(hours: Int) {
        dataStore.edit { prefs ->
            prefs[COOLDOWN_HOURS] = hours
        }
    }
    
    // ==================== Sync Timestamps ====================
    
    val lastPolicySync: Flow<Long> = dataStore.data
        .catch { handleException(it) }
        .map { prefs -> prefs[LAST_POLICY_SYNC] ?: 0L }
    
    suspend fun setLastPolicySync(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[LAST_POLICY_SYNC] = timestamp
        }
    }
    
    val lastUrlSync: Flow<Long> = dataStore.data
        .catch { handleException(it) }
        .map { prefs -> prefs[LAST_URL_SYNC] ?: 0L }
    
    suspend fun setLastUrlSync(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[LAST_URL_SYNC] = timestamp
        }
    }
    
    val lastViolationSync: Flow<Long> = dataStore.data
        .catch { handleException(it) }
        .map { prefs -> prefs[LAST_VIOLATION_SYNC] ?: 0L }
    
    suspend fun setLastViolationSync(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[LAST_VIOLATION_SYNC] = timestamp
        }
    }
    
    // ==================== Feature Flags ====================
    
    val autoSyncEnabled: Flow<Boolean> = dataStore.data
        .catch { handleException(it) }
        .map { prefs -> prefs[AUTO_SYNC_ENABLED] ?: true }
    
    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_SYNC_ENABLED] = enabled
        }
    }
    
    val notificationsEnabled: Flow<Boolean> = dataStore.data
        .catch { handleException(it) }
        .map { prefs -> prefs[NOTIFICATIONS_ENABLED] ?: true }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    val vpnFilterEnabled: Flow<Boolean> = dataStore.data
        .catch { handleException(it) }
        .map { prefs -> prefs[VPN_FILTER_ENABLED] ?: false }
    
    suspend fun setVpnFilterEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[VPN_FILTER_ENABLED] = enabled
        }
    }
    
    // ==================== Clear All ====================
    
    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
    
    // ==================== Error Handling ====================
    
    private fun handleException(exception: Throwable): Preferences {
        if (exception is IOException) {
            Timber.e(exception, "[AppPreferences] Error reading preferences")
        } else {
            throw exception
        }
        return emptyPreferences()
    }
}
