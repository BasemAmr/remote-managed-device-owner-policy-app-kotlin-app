package com.selfcontrol.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.selfcontrol.presentation.components.SelfControlTopAppBar
import com.selfcontrol.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigationActions: NavigationActions,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            SelfControlTopAppBar(
                title = "Settings",
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            ListItem(
                headlineContent = { Text("Device ID") },
                supportingContent = { Text(state.deviceId) }
            )
            Divider()
            
            ListItem(
                headlineContent = { Text("Device Owner Status") },
                supportingContent = { Text(if (state.isDeviceOwner) "Active" else "Inactive") },
                trailingContent = {
                    if (state.isDeviceOwner) {
                        Icon(androidx.compose.material.icons.filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            Divider()

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Auto Sync", style = MaterialTheme.typography.titleMedium)
                    Text("Sync policies automatically", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = state.autoSyncEnabled,
                    onCheckedChange = { viewModel.toggleAutoSync(it) }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Notifications", style = MaterialTheme.typography.titleMedium)
                    Text("Receive alerts", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = state.notificationsEnabled,
                    onCheckedChange = { viewModel.toggleNotifications(it) }
                )
            }
        }
    }
}
