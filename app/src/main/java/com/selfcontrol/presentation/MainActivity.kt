package com.selfcontrol.presentation

import android.Manifest
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
}
