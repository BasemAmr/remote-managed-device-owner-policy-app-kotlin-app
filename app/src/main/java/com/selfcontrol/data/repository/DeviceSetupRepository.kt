package com.selfcontrol.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.selfcontrol.data.local.prefs.AppPreferences
import com.selfcontrol.data.remote.api.SelfControlApi
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceSetupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: SelfControlApi,
    private val prefs: AppPreferences,
    private val appRepository: AppRepository,
    private val policyRepository: com.selfcontrol.domain.repository.PolicyRepository,
    private val accessibilityRepository: com.selfcontrol.domain.repository.AccessibilityRepository
) {
    
    companion object {
        private const val MAX_SYNC_RETRIES = 3
        private const val RETRY_DELAY_MS = 5000L
        private const val TOKEN_SAVE_DELAY_MS = 500L
    }

    @SuppressLint("HardwareIds")
    suspend fun performStartupChecks() {
        // 1. Always refresh installed apps list on startup so UI isn't empty
        Timber.i("[Startup] Scanning installed apps...")
        appRepository.refreshInstalledApps()

        // 2. Check if registered
        val currentId = prefs.deviceId.first()
        val currentToken = prefs.authToken.first()
        
        // CHECK: If ID is missing OR Token is missing -> REGISTER
        // This handles the case where app data was partially cleared
        if (currentId.isNullOrEmpty() || currentToken.isNullOrEmpty()) {
            Timber.w("[Startup] Missing ID or Token. Triggering Registration...")
            Timber.w("[Startup] Current ID: ${currentId?.take(10)}..., Has Token: ${!currentToken.isNullOrEmpty()}")
            registerDeviceWithRetry()
        } else {
            Timber.i("[Startup] Device registered & Authenticated. ID: ${currentId.take(10)}...")
            // If registered, sync policies with retry - MUST succeed before app opens
            syncPoliciesWithRetry()
        }
        
        // 3. Scan and sync accessibility services
        Timber.i("[Startup] Scanning accessibility services...")
        try {
            accessibilityRepository.scanAndSyncServices()
            accessibilityRepository.syncLockedServicesFromBackend()
            Timber.i("[Startup] ✅ Accessibility services synced")
        } catch (e: Exception) {
            Timber.e(e, "[Startup] ⚠️ Failed to sync accessibility services (non-critical)")
        }
        
        // 4. CRITICAL: Check if OUR OWN accessibility service is enabled
        // This is the DeviceOwner's accessibility service - it MUST always be enabled
        val ourServiceId = "${context.packageName}/${context.packageName}.deviceowner.AccessibilityMonitor"
        Timber.i("[Startup] Checking our accessibility service: $ourServiceId")
        
        val ourServiceComponent = android.content.ComponentName(
            context.packageName,
            "${context.packageName}.deviceowner.AccessibilityMonitor"
        )
        val ourServiceEnabled = com.selfcontrol.deviceowner.AccessibilityHelpers.isAccessibilityServiceEnabled(context, ourServiceComponent)
        Timber.i("[Startup] Our accessibility service enabled: $ourServiceEnabled")
        
        // 5. Lock our own service in the database (always)
        try {
            accessibilityRepository.markServiceAsLocked(ourServiceId, isLocked = true, isEnabled = ourServiceEnabled)
        } catch (e: Exception) {
            Timber.e(e, "[Startup] Failed to mark our service as locked")
        }
        
        // 6. Check ALL locked accessibility services (including our own)
        Timber.i("[Startup] Checking locked accessibility services...")
        try {
            // Build list of disabled services to enforce
            val disabledServiceIds = mutableListOf<String>()
            
            // First, check if our own service is disabled (highest priority)
            if (!ourServiceEnabled) {
                Timber.w("[Startup] ⚠️ OUR accessibility service is DISABLED! Enforcing...")
                disabledServiceIds.add(ourServiceId)
            }
            
            // Then check other locked services from backend
            val lockedServices = accessibilityRepository.getLockedServices().first()
            val otherDisabledLocked = lockedServices.filter { !it.isEnabled && it.serviceId != ourServiceId }
            disabledServiceIds.addAll(otherDisabledLocked.map { it.serviceId })
            
            if (disabledServiceIds.isNotEmpty()) {
                Timber.w("[Startup] ⚠️ ${disabledServiceIds.size} locked services are disabled! Launching enforcement...")
                
                // Trigger enforcement screen for ALL disabled locked services
                val intent = android.content.Intent(context, com.selfcontrol.presentation.enforcement.EnforcementActivity::class.java).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra("disabled_services", disabledServiceIds.toTypedArray())
                }
                context.startActivity(intent)
                Timber.i("[Startup] Enforcement screen launched for ${disabledServiceIds.size} services")
            } else {
                Timber.i("[Startup] ✅ All locked accessibility services are enabled")
            }
        } catch (e: Exception) {
            Timber.e(e, "[Startup] Failed to check locked services")
        }
    }
    
    /**
     * Register device with the backend and sync policies
     * Retries until successful or max attempts reached
     */
    private suspend fun registerDeviceWithRetry() {
        var attempt = 0
        var registered = false
        
        while (attempt < MAX_SYNC_RETRIES && !registered) {
            attempt++
            Timber.i("[Startup] Registration attempt $attempt/$MAX_SYNC_RETRIES")
            
            registered = registerDevice()
            
            if (registered) {
                // Wait for token to be saved to DataStore
                delay(TOKEN_SAVE_DELAY_MS)
                
                // Now sync policies with retry
                syncPoliciesWithRetry()
            } else if (attempt < MAX_SYNC_RETRIES) {
                Timber.w("[Startup] Registration failed, retrying in ${RETRY_DELAY_MS}ms...")
                delay(RETRY_DELAY_MS)
            }
        }
        
        if (!registered) {
            Timber.e("[Startup] ❌ Failed to register after $MAX_SYNC_RETRIES attempts")
            throw Exception("Failed to register device with server")
        }
    }
    
    /**
     * Sync policies with retry mechanism
     * App will not proceed until this succeeds
     */
    private suspend fun syncPoliciesWithRetry() {
        var attempt = 0
        var synced = false
        
        while (attempt < MAX_SYNC_RETRIES && !synced) {
            attempt++
            Timber.i("[Startup] Policy sync attempt $attempt/$MAX_SYNC_RETRIES")
            
            try {
                val result = policyRepository.syncPoliciesFromServer()
                
                when (result) {
                    is Result.Success -> {
                        Timber.i("[Startup] ✅ Policies synced successfully: ${result.data.size} policies")
                        synced = true
                    }
                    is Result.Error -> {
                        Timber.e(result.exception, "[Startup] Policy sync failed: ${result.message}")
                        if (attempt < MAX_SYNC_RETRIES) {
                            Timber.w("[Startup] Retrying policy sync in ${RETRY_DELAY_MS}ms...")
                            delay(RETRY_DELAY_MS)
                        }
                    }
                    is Result.Loading -> {
                        // Should not happen, but handle it
                        Timber.w("[Startup] Unexpected Loading state")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "[Startup] Exception during policy sync")
                if (attempt < MAX_SYNC_RETRIES) {
                    delay(RETRY_DELAY_MS)
                }
            }
        }
        
        if (!synced) {
            Timber.e("[Startup] ❌ Failed to sync policies after $MAX_SYNC_RETRIES attempts")
            throw Exception("Failed to sync policies from server")
        }
    }

    @SuppressLint("HardwareIds")
    private suspend fun registerDevice(): Boolean {
        Timber.i("[Startup] Registering with Backend...")
        return try {
            // Get unique Android ID
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_id"
            val model = android.os.Build.MODEL ?: "Unknown Device"

            // Call Backend
            val response = api.registerDevice(mapOf(
                "device_name" to model,
                "android_id" to androidId
            ))

            if (response.success && response.data != null) {
                val deviceIdStr = response.data.deviceId.toString() // Convert Int to String for storage
                // Get token from either field (authToken or token)
                val token = response.data.authToken ?: response.data.token

                // Save to Preferences
                prefs.setDeviceId(deviceIdStr)
                if (token != null) {
                    prefs.setAuthToken(token)
                }

                Timber.i("[Startup] ✅ Registration Successful! ID: $deviceIdStr")
                true
            } else {
                Timber.e("[Startup] ❌ Registration Failed: ${response.message}")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "[Startup] ❌ Registration Error (Check Internet/Render URL)")
            false
        }
    }
}
