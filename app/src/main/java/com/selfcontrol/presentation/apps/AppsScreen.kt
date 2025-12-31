package com.selfcontrol.presentation.apps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.selfcontrol.presentation.components.AppCard
import com.selfcontrol.presentation.components.EmptyState
import com.selfcontrol.presentation.components.ErrorScreen
import com.selfcontrol.presentation.components.LoadingDialog
import com.selfcontrol.presentation.components.SelfControlTopAppBar
import com.selfcontrol.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    navigationActions: NavigationActions,
    viewModel: AppsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            SelfControlTopAppBar(
                title = "Manage Apps",
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
                .fillMaxSize()
        ) {
            // Search Bar
            TextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onEvent(AppsEvent.Search(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search apps...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // Content
            Box(modifier = Modifier.weight(1f)) {
                if (state.isLoading && state.apps.isEmpty()) {
                    LoadingDialog(isLoading = true)
                } else if (state.error != null) {
                    ErrorScreen(
                        message = state.error ?: "Unknown error",
                        onRetry = { viewModel.onEvent(AppsEvent.Refresh) }
                    )
                } else if (state.filteredApps.isEmpty()) {
                    EmptyState("No apps found matching your search.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.filteredApps, key = { it.packageName }) { app ->
                            AppItem(
                                app = app,
                                onToggleBlock = { 
                                    viewModel.onEvent(AppsEvent.ToggleBlock(app.packageName, app.isBlocked))
                                },
                                onClick = { navigationActions.navigateToAppDetails(app.packageName) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppItem(
    app: com.selfcontrol.domain.model.App,
    onToggleBlock: () -> Unit,
    onClick: () -> Unit
) {
    // Custom wrapper around AppCard to include the Switch/Checkbox interaction
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (app.isBlocked) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = app.name, style = MaterialTheme.typography.titleMedium)
                    if (app.isLocked) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                if (app.isLocked) {
                    Text(
                        text = "Managed by Administrator",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (app.isBlocked) {
                    Text(
                        text = "Blocked by User",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    Text(text = app.packageName, style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Switch(
                checked = app.isBlocked,
                onCheckedChange = { onToggleBlock() },
                enabled = !app.isLocked,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.error,
                    checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                )
            )
        }
    }
}
