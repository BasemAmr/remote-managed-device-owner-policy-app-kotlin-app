package com.selfcontrol.deviceowner

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeviceOwnerManager - Wrapper around DevicePolicyManager
 * Provides high-level device management operations
 */
@Singleton
class DeviceOwnerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val devicePolicyManager: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    
    private val adminComponent: ComponentName by lazy {
        ComponentName(context, DeviceOwnerReceiver::class.java)
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
     * Check if device admin is active
     */
    fun isAdminActive(): Boolean {
        return try {
            devicePolicyManager.isAdminActive(adminComponent)
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Failed to check admin status")
            false
        }
    }
    
    /**
     * Initialize device owner features
     */
    fun initialize() {
        if (!isDeviceOwner()) {
            Timber.w("[DeviceOwnerManager] Not a device owner, skipping initialization")
            return
        }
        
        Timber.i("[DeviceOwnerManager] Initializing device owner features")
        
        try {
            // Set user restrictions
            setUserRestrictions()
            
            // Configure system update policy
            configureSystemUpdates()
            
            Timber.i("[DeviceOwnerManager] Initialization complete")
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Initialization failed")
            throw e
        }
    }
    
    /**
     * Cleanup device owner features
     */
    fun cleanup() {
        Timber.i("[DeviceOwnerManager] Cleaning up device owner features")
        
        try {
            // Clear user restrictions
            clearUserRestrictions()
            
            Timber.i("[DeviceOwnerManager] Cleanup complete")
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Cleanup failed")
        }
    }
    
    /**
     * Disable an application
     */
    fun disableApp(packageName: String): Boolean {
        if (!isDeviceOwner()) {
            Timber.w("[DeviceOwnerManager] Cannot disable app - not device owner")
            return false
        }
        
        return try {
            devicePolicyManager.setApplicationHidden(adminComponent, packageName, true)
            Timber.i("[DeviceOwnerManager] Disabled app: $packageName")
            true
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Failed to disable app: $packageName")
            false
        }
    }
    
    /**
     * Enable an application
     */
    fun enableApp(packageName: String): Boolean {
        if (!isDeviceOwner()) {
            Timber.w("[DeviceOwnerManager] Cannot enable app - not device owner")
            return false
        }
        
        return try {
            devicePolicyManager.setApplicationHidden(adminComponent, packageName, false)
            Timber.i("[DeviceOwnerManager] Enabled app: $packageName")
            true
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Failed to enable app: $packageName")
            false
        }
    }
    
    /**
     * Check if an app is hidden
     */
    fun isAppHidden(packageName: String): Boolean {
        if (!isDeviceOwner()) return false
        
        return try {
            devicePolicyManager.isApplicationHidden(adminComponent, packageName)
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Failed to check app visibility: $packageName")
            false
        }
    }
    
    /**
     * Set user restrictions to prevent uninstalling device owner
     */
    private fun setUserRestrictions() {
        try {
            devicePolicyManager.addUserRestriction(
                adminComponent,
                UserManager.DISALLOW_UNINSTALL_APPS
            )
            Timber.d("[DeviceOwnerManager] Set user restrictions")
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Failed to set user restrictions")
        }
    }
    
    /**
     * Clear user restrictions
     */
    private fun clearUserRestrictions() {
        try {
            devicePolicyManager.clearUserRestriction(
                adminComponent,
                UserManager.DISALLOW_UNINSTALL_APPS
            )
            Timber.d("[DeviceOwnerManager] Cleared user restrictions")
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Failed to clear user restrictions")
        }
    }
    
    /**
     * Configure system update policy
     */
    private fun configureSystemUpdates() {
        try {
            // Allow system updates but don't force them
            // This can be customized based on requirements
            Timber.d("[DeviceOwnerManager] Configured system updates")
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Failed to configure system updates")
        }
    }
    
    /**
     * Lock the device
     */
    fun lockDevice() {
        if (!isDeviceOwner()) return
        
        try {
            devicePolicyManager.lockNow()
            Timber.i("[DeviceOwnerManager] Device locked")
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Failed to lock device")
        }
    }
    
    /**
     * Wipe device data (factory reset)
     * Use with extreme caution!
     */
    fun wipeDevice(reason: String = "Remote wipe requested") {
        if (!isDeviceOwner()) return
        
        Timber.w("[DeviceOwnerManager] Wiping device: $reason")
        
        try {
            devicePolicyManager.wipeData(
                DevicePolicyManager.WIPE_EXTERNAL_STORAGE,
                reason
            )
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwnerManager] Failed to wipe device")
        }
    }
}
