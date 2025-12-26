package com.selfcontrol.presentation.requests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.selfcontrol.domain.model.Request
import com.selfcontrol.presentation.components.EmptyState
import com.selfcontrol.presentation.components.ErrorScreen
import com.selfcontrol.presentation.components.LoadingDialog
import com.selfcontrol.presentation.components.SelfControlTopAppBar
import com.selfcontrol.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(
    navigationActions: NavigationActions,
    viewModel: RequestsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            SelfControlTopAppBar(
                title = "Access Requests",
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navigationActions.navigateToCreateRequest() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Request")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading && state.pendingRequests.isEmpty()) {
                LoadingDialog(isLoading = true)
            } else if (state.error != null) {
                ErrorScreen(state.error ?: "Error loading requests") {
                    viewModel.onEvent(RequestsEvent.Refresh)
                }
            } else if (state.pendingRequests.isEmpty()) {
                EmptyState("No pending requests")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.pendingRequests) { request ->
                        RequestItem(
                            request = request,
                            onApprove = { viewModel.onEvent(RequestsEvent.Approve(request.id)) },
                            onDeny = { viewModel.onEvent(RequestsEvent.Deny(request.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestItem(
    request: Request,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = request.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = request.reason, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDeny,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deny")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = onApprove
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve")
                }
            }
        }
    }
}
