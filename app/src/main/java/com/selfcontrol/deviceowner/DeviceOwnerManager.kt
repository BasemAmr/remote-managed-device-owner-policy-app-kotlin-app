package com.selfcontrol.deviceowner

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import android.provider.Settings
import com.selfcontrol.data.local.prefs.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceOwnerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AppPreferences // Inject this to update UI state
) {
    
    private val devicePolicyManager: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    
    private val adminComponent: ComponentName by lazy {
        ComponentName(context, DeviceOwnerReceiver::class.java)
    }

    /**
     * Called on App Startup.
     * 1. Checks if we are actually Device Owner.
     * 2. If yes, LOCKS settings and updates UI.
     */
    fun initialize() {
        val isOwner = devicePolicyManager.isDeviceOwnerApp(context.packageName)
        
        // Update the UI Preference immediately
        CoroutineScope(Dispatchers.IO).launch {
            prefs.setDeviceOwner(isOwner)
        }

        if (!isOwner) {
            Timber.w("[DeviceOwner] Not device owner. Cannot enforce policies.")
            return
        }
        
        Timber.i("[DeviceOwner] Enforcing strict policies...")
        
        try {
            // 1. Prevent App Uninstallation
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_UNINSTALL_APPS)
            
            // 2. Force Accessibility Service (Irrevocable access)
            enforceAccessibilityService()

            // 3. Optional: Prevent clearing app data (keeps your local DB safe)
            // devicePolicyManager.setUninstallBlocked(adminComponent, context.packageName, true)

            Timber.i("[DeviceOwner] Policies enforced successfully")
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwner] Failed to enforce policies")
        }
    }

    private fun enforceAccessibilityService() {
        try {
            val serviceName = ComponentName(context, AccessibilityMonitor::class.java).flattenToString()
            
            // Allow user to use accessibility services (in case it was disabled)
            devicePolicyManager.clearUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_CREDENTIALS)

            // Set our service as ENABLED in Secure Settings
            devicePolicyManager.setSecureSetting(
                adminComponent,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                serviceName
            )
            
            // Enable Accessibility master switch
            devicePolicyManager.setSecureSetting(
                adminComponent,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                "1"
            )
            
            // Lock it so only our service can run (or prevent changing it)
            devicePolicyManager.setPermittedAccessibilityServices(
                adminComponent,
                listOf(context.packageName)
            )
            
            Timber.i("[DeviceOwner] Accessibility Service Locked & Enabled")
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwner] Failed to force accessibility")
        }
    }

    // Wrappers for blocking specific apps
    fun disableApp(packageName: String): Boolean {
        if (!devicePolicyManager.isDeviceOwnerApp(context.packageName)) return false
        return try {
            devicePolicyManager.setApplicationHidden(adminComponent, packageName, true)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun enableApp(packageName: String): Boolean {
        if (!devicePolicyManager.isDeviceOwnerApp(context.packageName)) return false
        return try {
            devicePolicyManager.setApplicationHidden(adminComponent, packageName, false)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun isAppHidden(packageName: String): Boolean {
        if (!devicePolicyManager.isDeviceOwnerApp(context.packageName)) return false
        return devicePolicyManager.isApplicationHidden(adminComponent, packageName)
    }

    /**
     * Check if this app is the device owner
     */
    fun isDeviceOwner(): Boolean {
        return try {
            devicePolicyManager.isDeviceOwnerApp(context.packageName)
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Failed to check device owner status")
            false
        }
    }
    
    /**
     * Set an app as hidden (blocked from use)
     */
    fun setAppHidden(packageName: String, hidden: Boolean): Boolean {
        if (!devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
            Timber.w("[DeviceOwnerManager] Not device owner, cannot hide app")
            return false
        }
        
        return try {
            devicePolicyManager.setApplicationHidden(adminComponent, packageName, hidden)
            Timber.d("[DeviceOwnerManager] Set $packageName hidden=$hidden")
            true
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Failed to set app hidden state")
            false
        }
    }
    
    /**
     * Block or unblock app uninstallation
     */
    fun setAppUninstallBlocked(packageName: String, blocked: Boolean) {
        if (!devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
            Timber.w("[DeviceOwnerManager] Not device owner, cannot block uninstall")
            return
        }
        
        try {
            devicePolicyManager.setUninstallBlocked(adminComponent, packageName, blocked)
            Timber.d("[DeviceOwnerManager] Set $packageName uninstall blocked=$blocked")
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Failed to set uninstall blocked")
        }
    }
    
    /**
     * Check if device admin is active
     */
    fun isAdminActive(): Boolean {
        return try {
            devicePolicyManager.isAdminActive(adminComponent)
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Failed to check admin active status")
            false
        }
    }
    
    /**
     * Cleanup when device owner is disabled
     */
    fun cleanup() {
        Timber.i("[DeviceOwnerManager] Cleaning up device owner resources")
        
        try {
            // Update preferences
            CoroutineScope(Dispatchers.IO).launch {
                prefs.setDeviceOwner(false)
            }
            
            // Remove any restrictions we added
            // Note: Most restrictions will be automatically removed when device owner is removed
            Timber.i("[DeviceOwnerManager] Cleanup complete")
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Failed to cleanup")
        }
    }
}
