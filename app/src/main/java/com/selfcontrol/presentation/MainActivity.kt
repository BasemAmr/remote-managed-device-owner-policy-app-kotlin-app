package com.selfcontrol.presentation

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.selfcontrol.presentation.navigation.NavGraph
import com.selfcontrol.presentation.theme.SelfControlTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    // Permission launcher for POST_NOTIFICATIONS
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Timber.i("[MainActivity] POST_NOTIFICATIONS permission granted")
        } else {
            Timber.w("[MainActivity] POST_NOTIFICATIONS permission denied")
        }
    }
    
    // Receiver to detect when AccessibilityMonitor is destroyed
    private val accessibilityDestroyedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.selfcontrol.ACCESSIBILITY_DESTROYED") {
                Timber.w("[MainActivity] 🚨 Accessibility service destroyed! Triggering enforcement...")
                
                // Launch enforcement screen immediately
                val enforcementIntent = Intent(context, com.selfcontrol.presentation.enforcement.EnforcementActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra("disabled_services", arrayOf("com.selfcontrol/com.selfcontrol.deviceowner.AccessibilityMonitor"))
                }
                context?.startActivity(enforcementIntent)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Register receiver for accessibility service destruction
        val filter = IntentFilter("com.selfcontrol.ACCESSIBILITY_DESTROYED")
        registerReceiver(accessibilityDestroyedReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        Timber.i("[MainActivity] Registered accessibility destruction receiver")
        
        // Request notification permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasNotificationPermission) {
                Timber.i("[MainActivity] Requesting POST_NOTIFICATIONS permission")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Timber.d("[MainActivity] POST_NOTIFICATIONS permission already granted")
            }
        }
        
        setContent {
            SelfControlTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(accessibilityDestroyedReceiver)
        } catch (e: Exception) {
            Timber.e(e, "[MainActivity] Error unregistering receiver")
        }
    }
}
