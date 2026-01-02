package com.selfcontrol.presentation.home

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.selfcontrol.presentation.components.LoadingDialog
import com.selfcontrol.presentation.components.SelfControlTopAppBar
import com.selfcontrol.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigationActions: NavigationActions,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            SelfControlTopAppBar(
                title = "Dashboard",
                actions = {
                    IconButton(onClick = { viewModel.onEvent(HomeEvent.Refresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { navigationActions.navigateToSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Device Owner Status Card
                StatusCard(
                    isActive = state.deviceOwnerActive,
                    onClick = { if (!state.deviceOwnerActive) viewModel.onEvent(HomeEvent.GrantDeviceOwner) }
                )

                // VPN Status Card
                VpnStatusCard(
                    isConnected = state.vpnConnected
                )

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Blocked",
                        value = state.blockedAppCount.toString(),
                        icon = Icons.Default.Lock,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        onClick = { navigationActions.navigateToApps() }
                    )
                    StatCard(
                        title = "Total Apps",
                        value = state.totalAppCount.toString(),
                        icon = Icons.Filled.Apps,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { navigationActions.navigateToApps() }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Violations",
                        value = state.activeViolations.toString(),
                        icon = Icons.Default.Warning,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = { navigationActions.navigateToViolations() }
                    )
                    StatCard(
                        title = "Blocked URLs",
                        value = state.blockedUrlCount.toString(),
                        icon = Icons.Default.Block,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        onClick = { navigationActions.navigateToUrls() }
                    )
                }

                // ==================== SYNC ACTIONS ====================
                Spacer(modifier = Modifier.height(8.dp))
                
                SyncButtonGroup(
                    pendingSyncCount = state.pendingSyncCount,
                    isSyncingApps = state.isSyncingApps,
                    isSyncingPolicies = state.isSyncingPolicies,
                    isSyncingUrls = state.isSyncingUrls,
                    appSyncMessage = state.syncStatusMessage,
                    policySyncMessage = state.policySyncStatusMessage,
                    urlSyncMessage = state.urlSyncStatusMessage,
                    onSyncApps = { viewModel.onEvent(HomeEvent.SyncAllApps) },
                    onSyncPolicies = { viewModel.onEvent(HomeEvent.SyncAllPolicies) },
                    onSyncUrls = { viewModel.onEvent(HomeEvent.SyncAllUrls) }
                )

                // EMERGENCY REMOVE BUTTON (Developer Only)
                if (state.deviceOwnerActive) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.onEvent(HomeEvent.RemoveDeviceOwner) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CLEAR DEVICE OWNER (DEV)")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                // Last Sync Info
                if (state.lastSyncTime > 0) {
                    Text(
                        text = "Last synced: ${DateUtils.getRelativeTimeSpanString(state.lastSyncTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LoadingDialog(isLoading = state.isLoading)
        }
    }
}

@Composable
fun SyncButtonGroup(
    pendingSyncCount: Int,
    isSyncingApps: Boolean,
    isSyncingPolicies: Boolean,
    isSyncingUrls: Boolean,
    appSyncMessage: String?,
    policySyncMessage: String?,
    urlSyncMessage: String?,
    onSyncApps: () -> Unit,
    onSyncPolicies: () -> Unit,
    onSyncUrls: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SyncButton(
                text = "Apps",
                isSyncing = isSyncingApps,
                pendingCount = pendingSyncCount,
                onClick = onSyncApps,
                modifier = Modifier.weight(1f),
                containerColor = if (pendingSyncCount > 0) 
                    MaterialTheme.colorScheme.tertiary 
                else 
                    MaterialTheme.colorScheme.primary
            )
            
            SyncButton(
                text = "Policies",
                isSyncing = isSyncingPolicies,
                onClick = onSyncPolicies,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.secondary
            )

            SyncButton(
                text = "URLs",
                isSyncing = isSyncingUrls,
                onClick = onSyncUrls,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.error
            )
        }
        
        // Status Messages
        appSyncMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (it.contains("successful", ignoreCase = true)) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
        }
        
        policySyncMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (it.contains("synced", ignoreCase = true)) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
        }
        
        if (pendingSyncCount > 0 && !isSyncingApps) {
            Text(
                text = "$pendingSyncCount apps pending upload",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun SyncButton(
    text: String,
    isSyncing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pendingCount: Int = 0,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        enabled = !isSyncing,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        if (isSyncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
            
            if (pendingCount > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Badge(containerColor = MaterialTheme.colorScheme.error) {
                    Text(pendingCount.toString())
                }
            }
        }
    }
}

/**
 * Legacy Sync Apps Now Button - kept for compatibility if needed, but replaced by SyncButtonGroup
 */
@Composable
fun SyncAppsButton(
    pendingSyncCount: Int,
    isSyncing: Boolean,
    syncStatusMessage: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onClick,
            enabled = !isSyncing,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (pendingSyncCount > 0) 
                    MaterialTheme.colorScheme.tertiary 
                else 
                    MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Syncing...")
            } else {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Apps Now")
                
                // Show pending count badge if > 0
                if (pendingSyncCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = pendingSyncCount.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        
        // Show sync status message
        if (syncStatusMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = syncStatusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    syncStatusMessage.contains("successful", ignoreCase = true) -> 
                        MaterialTheme.colorScheme.primary
                    syncStatusMessage.contains("failed", ignoreCase = true) -> 
                        MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        
        // Show pending apps indicator
        if (pendingSyncCount > 0 && !isSyncing) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$pendingSyncCount app${if (pendingSyncCount > 1) "s" else ""} pending sync",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun StatusCard(
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Filled.Error,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = if (isActive) "Device Active" else "Device Owner Inactive",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isActive) "Protection is running" else "Tap to setup protection",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VpnStatusCard(
    isConnected: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.Lock else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isConnected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = if (isConnected) "🔒 VPN Protected" else "⚠️ VPN Disconnected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = if (isConnected) 
                        "URL filtering is active" 
                    else 
                        "URL filtering is not active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isConnected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}