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
            // 1. Self-Protect Device Owner App (Prevent uninstallation of this app)
            devicePolicyManager.setUninstallBlocked(adminComponent, context.packageName, true)
            Timber.i("[DeviceOwner] Self-protection enabled - cannot uninstall this app")
            
            // 2. Force Accessibility Service (Irrevocable access)
            enforceAccessibilityService()

            // 3. Enforce VPN Always-On (Requires Android 7.0+)
            enforceVpnAlwaysOn()

            // Start VPN service content
            try {
                UrlFilterVpnService.start(context)
                Timber.i("[DeviceOwner] VPN service started")
            } catch (e: Exception) {
                Timber.e(e, "[DeviceOwner] Failed to start VPN service")
            }

            // 4. Prevent Factory Reset from Settings
//            try {
//                devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
//                Timber.i("[DeviceOwner] Factory reset prevention enabled")
//            } catch (e: Exception) {
//                Timber.w(e, "[DeviceOwner] Failed to enable factory reset prevention")
//            }

            // 5. Optional: Prevent clearing app data (keeps your local DB safe)
            // devicePolicyManager.setUninstallBlocked(adminComponent, context.packageName, true)

            Timber.i("[DeviceOwner] Policies enforced successfully")
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwner] Failed to enforce policies")
        }
    }

    /**
     * Enforce accessibility service to be enabled and locked
     * Made public so it can be called by AccessibilityEnforceWorker
     */
    fun enforceAccessibilityService() {
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
            
            Timber.i("[DeviceOwner] Accessibility Service Locked & Enabled at ${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwner] Failed to force accessibility")
        }
    }
    
    /**
     * Check if accessibility service is currently active
     */
    fun isAccessibilityServiceActive(): Boolean {
        return try {
            // Read the enabled accessibility services from system settings
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            
            val serviceName = ComponentName(context, AccessibilityMonitor::class.java).flattenToString()
            val isEnabled = enabledServices?.contains(serviceName) == true
            
            Timber.d("[DeviceOwner] Accessibility service active: $isEnabled")
            isEnabled
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwner] Failed to check accessibility service status")
            false
        }
    }
    
    /**
     * Enforce accessibility service for a specific app (per-app locking)
     * Note: Android doesn't provide direct API to enable another app's accessibility service
     * This method stores the intent and monitors via AccessibilityMonitor
     */
    fun enforceAppAccessibilityService(packageName: String) {
        try {
            // Check if app has accessibility service declared
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                android.content.pm.PackageManager.GET_SERVICES
            )
            
            val hasAccessibilityService = packageInfo.services?.any { serviceInfo ->
                serviceInfo.permission == android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE
            } ?: false
            
            if (hasAccessibilityService) {
                Timber.i("[DeviceOwner] App $packageName has accessibility service - monitoring enabled")
                // The actual enforcement is handled by AccessibilityMonitor tampering detection
            } else {
                Timber.w("[DeviceOwner] App $packageName does not have accessibility service")
            }
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwner] Failed to enforce app accessibility service for $packageName")
        }
    }

    /**
     * Enforce VPN Always-On to ensure URL filtering cannot be disabled
     * Requires Android 7.0+ (API 24)
     */
    private fun enforceVpnAlwaysOn() {
        try {
            // Check Android version
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
                Timber.w("[DeviceOwner] VPN Always-On requires Android 7.0+ (API 24)")
                return
            }
            
            // Set this app's VPN as always-on
            // Parameters:
            // - adminComponent: Device owner component
            // - packageName: This app's package (contains VPN service)
            // - lockdownEnabled: false = allow traffic even if VPN is temporarily down
            //                    (true would block ALL traffic if VPN isn't routing, causing ERR_NETWORK_ACCESS_DENIED)
            // - lockdownWhitelist: null = no apps are whitelisted
            devicePolicyManager.setAlwaysOnVpnPackage(
                adminComponent,
                context.packageName,
                false,  // lockdown mode DISABLED - allows normal internet while VPN is passive
                null    // no whitelist needed since lockdown is off
            )
            
            Timber.i("[DeviceOwner] VPN Always-On enabled (lockdown disabled for passthrough mode)")
            
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwner] Failed to enforce VPN always-on")
        }
    }
    
    /**
     * Check if VPN always-on is configured for this app
     */
    fun isVpnAlwaysOn(): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
                return false
            }
            
            val alwaysOnVpnPackage = devicePolicyManager.getAlwaysOnVpnPackage(adminComponent)
            alwaysOnVpnPackage == context.packageName
            
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwner] Failed to check VPN always-on status")
            false
        }
    }
    
    /**
     * Immediately lock the device screen
     * Used for remote lock functionality via heartbeat
     */
    fun lockDevice(): Boolean {
        return try {
            if (!devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
                Timber.w("[DeviceOwner] Not device owner, cannot lock device")
                return false
            }
            
            devicePolicyManager.lockNow()
            Timber.i("[DeviceOwner] Device locked remotely")
            true
            
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwner] Failed to lock device")
            false
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
     * Clears the device owner and removes all restrictions.
     */
    fun clearDeviceOwner() {
        try {
            if (devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
                devicePolicyManager.clearDeviceOwnerApp(context.packageName)
                Timber.i("[DeviceOwnerManager] Device owner cleared successfully")
                cleanup()
            } else {
                Timber.w("[DeviceOwnerManager] Not device owner, cannot clear")
            }
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Failed to clear device owner")
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
