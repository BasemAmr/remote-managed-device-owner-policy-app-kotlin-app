package com.selfcontrol.presentation.requests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.selfcontrol.domain.model.RequestType
import com.selfcontrol.presentation.components.LoadingDialog
import com.selfcontrol.presentation.components.SelfControlTopAppBar
import com.selfcontrol.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestScreen(
    navigationActions: NavigationActions,
    viewModel: CreateRequestViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedType by remember { mutableStateOf(RequestType.APP_ACCESS) }
    var packageName by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf(1) }

    val durations = listOf(1, 2, 4, 8, 12, 24, 48)

    // Navigate back on success
    LaunchedEffect(state.requestCreated) {
        if (state.requestCreated) {
            navigationActions.navigateBack()
        }
    }

    Scaffold(
        topBar = {
            SelfControlTopAppBar(
                title = "Create Request",
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Request Type Selection
                Text(
                    text = "Request Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                RequestType.values().forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.getDisplayName()) },
                        leadingIcon = {
                            if (selectedType == type) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    )
                }

                Divider()

                // Package Name (for APP_ACCESS)
                if (selectedType == RequestType.APP_ACCESS || selectedType == RequestType.TEMPORARY_UNBLOCK) {
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Package Name") },
                        placeholder = { Text("com.example.app") },
                        leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // URL (for URL_ACCESS)
                if (selectedType == RequestType.URL_ACCESS) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        placeholder = { Text("https://example.com") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Reason
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    placeholder = { Text("Why do you need access?") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    minLines = 3
                )

                // Duration Selection
                Text(
                    text = "Duration (hours)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    durations.take(4).forEach { duration ->
                        FilterChip(
                            selected = selectedDuration == duration,
                            onClick = { selectedDuration = duration },
                            label = { Text("${duration}h") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    durations.drop(4).forEach { duration ->
                        FilterChip(
                            selected = selectedDuration == duration,
                            onClick = { selectedDuration = duration },
                            label = { Text("${duration}h") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Submit Button
                Button(
                    onClick = {
                        viewModel.onEvent(
                            CreateRequestEvent.Submit(
                                type = selectedType,
                                packageName = packageName,
                                url = url,
                                reason = reason,
                                durationHours = selectedDuration
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading && isFormValid(selectedType, packageName, url, reason)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Request")
                }

                // Error Message
                state.error?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Info Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Your request will be sent for approval. You'll be notified when it's reviewed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            LoadingDialog(isLoading = state.isLoading)
        }
    }
}

private fun isFormValid(
    type: RequestType,
    packageName: String,
    url: String,
    reason: String
): Boolean {
    return when (type) {
        RequestType.APP_ACCESS, RequestType.TEMPORARY_UNBLOCK -> packageName.isNotBlank()
        RequestType.URL_ACCESS -> url.isNotBlank()
        RequestType.POLICY_OVERRIDE -> reason.isNotBlank()
    }
}
