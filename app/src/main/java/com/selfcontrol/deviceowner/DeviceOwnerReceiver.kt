package com.selfcontrol.deviceowner

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Device Admin Receiver - Entry point for Device Owner capabilities
 * This receiver handles device admin lifecycle events
 */
@AndroidEntryPoint
class DeviceOwnerReceiver : DeviceAdminReceiver() {
    
    @Inject
    lateinit var deviceOwnerManager: DeviceOwnerManager
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Timber.i("[DeviceOwner] Device Owner enabled successfully")
        
        try {
            // Initialize device owner features
            deviceOwnerManager.initialize()
            
            // Notify that device is now managed
            Timber.i("[DeviceOwner] Initialization complete")
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwner] Failed to initialize")
        }
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Timber.w("[DeviceOwner] Device Owner disabled")
        
        try {
            deviceOwnerManager.cleanup()
        } catch (e: Exception) {
            Timber.e(e, "[DeviceOwner] Failed to cleanup")
        }
    }
    
    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Timber.i("[DeviceOwner] Lock task mode entering: $pkg")
    }
    
    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Timber.i("[DeviceOwner] Lock task mode exiting")
    }
    
    override fun onPasswordChanged(context: Context, intent: Intent) {
        super.onPasswordChanged(context, intent)
        Timber.i("[DeviceOwner] Password changed")
    }
    
    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        Timber.w("[DeviceOwner] Password failed")
    }
    
    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        Timber.d("[DeviceOwner] Password succeeded")
    }
}
