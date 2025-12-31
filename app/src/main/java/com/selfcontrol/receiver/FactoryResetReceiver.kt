package com.selfcontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.selfcontrol.data.local.prefs.AppPreferences
import com.selfcontrol.deviceowner.DeviceOwnerManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * FactoryResetReceiver - Intercepts factory reset attempts
 * Logs factory reset attempts and attempts to abort the broadcast
 */
@AndroidEntryPoint
class FactoryResetReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var deviceOwnerManager: DeviceOwnerManager
    
    @Inject
    lateinit var appPreferences: AppPreferences
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        
        when (intent.action) {
            "android.intent.action.FACTORY_RESET",
            "android.intent.action.MASTER_CLEAR" -> {
                handleFactoryResetAttempt(context)
            }
        }
    }
    
    private fun handleFactoryResetAttempt(context: Context?) {
        scope.launch {
            try {
                val deviceId = appPreferences.deviceId.first()
                val timestamp = System.currentTimeMillis()
                
                Timber.w("[FactoryResetReceiver] ⚠️ Factory reset attempt detected!")
                Timber.w("[FactoryResetReceiver] Device ID: $deviceId")
                Timber.w("[FactoryResetReceiver] Timestamp: $timestamp")
                
                // Attempt to abort the broadcast (may not work on all devices)
                try {
                    abortBroadcast()
                    Timber.i("[FactoryResetReceiver] Factory reset broadcast aborted")
                } catch (e: Exception) {
                    Timber.e(e, "[FactoryResetReceiver] Failed to abort factory reset broadcast")
                }
                
                Timber.i("[FactoryResetReceiver] Factory reset attempt detected and logged")
                
            } catch (e: Exception) {
                Timber.e(e, "[FactoryResetReceiver] Error handling factory reset attempt")
            }
        }
    }
}
