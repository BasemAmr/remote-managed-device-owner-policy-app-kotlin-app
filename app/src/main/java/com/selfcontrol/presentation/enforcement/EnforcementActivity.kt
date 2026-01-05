package com.selfcontrol.presentation.enforcement

import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.selfcontrol.deviceowner.AccessibilityHelpers
import com.selfcontrol.domain.model.AccessibilityService
import com.selfcontrol.presentation.theme.SelfControlTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import timber.log.Timber

@AndroidEntryPoint
class EnforcementActivity : ComponentActivity() {
    
    private val viewModel: EnforcementViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Prevent dismissal
        setFinishOnTouchOutside(false)
        
        // Broadcast to pause accessibility monitoring
        sendBroadcast(android.content.Intent("com.selfcontrol.PAUSE_MONITORING"))
        
        // START LOCK TASK MODE - LOCKS ENTIRE PHONE
        try {
            startLockTask()
            Timber.i("[Enforcement] Lock task mode started - phone is locked")
        } catch (e: Exception) {
            Timber.e(e, "[Enforcement] Failed to start lock task mode")
        }
        
        val disabledServiceIds = intent.getStringArrayExtra("disabled_services") ?: emptyArray()
        viewModel.loadDisabledServices(disabledServiceIds.toList())
        
        setContent {
            SelfControlTheme {
                EnforcementScreen(
                    viewModel = viewModel,
                    onServiceEnabled = { serviceId ->
                        viewModel.checkServiceStatus(serviceId)
                    },
                    onAllServicesEnabled = {
                        // Resume monitoring
                        sendBroadcast(android.content.Intent("com.selfcontrol.RESUME_MONITORING"))
                        
                        // Stop lock task and close
                        try {
                            stopLockTask()
                            Timber.i("[Enforcement] Lock task stopped - phone unlocked")
                        } catch (e: Exception) {
                            Timber.e(e, "[Enforcement] Failed to stop lock task")
                        }
                        finish()
                    }
                )
            }
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent back button dismissal
        Toast.makeText(this, "Please enable the required service", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Ensure lock task is stopped
        try {
            stopLockTask()
        } catch (e: Exception) {
            Timber.e(e, "[Enforcement] Failed to stop lock task on destroy")
        }
    }
}

@Composable
fun EnforcementScreen(
    viewModel: EnforcementViewModel,
    onServiceEnabled: (String) -> Unit,
    onAllServicesEnabled: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Auto-dismiss when all services are enabled (only after loading is complete)
    LaunchedEffect(state.allServicesEnabled, state.isLoading) {
        if (!state.isLoading && state.allServicesEnabled && state.disabledServices.isEmpty()) {
            delay(1000)
            onAllServicesEnabled()
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Accessibility Service Required",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "The following accessibility service(s) must be enabled:",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            state.disabledServices.forEach { service ->
                ServiceCard(
                    service = service,
                    onEnableClick = {
                        // Don't open settings - use Device Owner to force enable
                        viewModel.forceEnableService(service.serviceId)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    state.disabledServices.forEach { service ->
                        onServiceEnabled(service.serviceId)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Check Status")
            }
            
            if (state.allServicesEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "✓ All services enabled. Closing...",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ServiceCard(
    service: AccessibilityService,
    onEnableClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = service.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
            
            Button(
                onClick = onEnableClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Auto-Enable")
            }
        }
    }
}
