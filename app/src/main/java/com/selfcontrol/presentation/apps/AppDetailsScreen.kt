package com.selfcontrol.presentation.apps

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.selfcontrol.domain.model.App
import com.selfcontrol.domain.model.AppPolicy
import com.selfcontrol.domain.repository.AppRepository
import com.selfcontrol.domain.repository.PolicyRepository
import com.selfcontrol.domain.usecase.policy.ApplyPolicyUseCase
import com.selfcontrol.presentation.components.ConfirmDialog
import com.selfcontrol.presentation.components.LoadingDialog
import com.selfcontrol.presentation.components.SelfControlTopAppBar
import com.selfcontrol.presentation.navigation.NavigationActions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(
    packageName: String,
    navigationActions: NavigationActions,
    viewModel: AppDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showUnblockConfirm by remember { mutableStateOf(false) }
    var showLockConfirm by remember { mutableStateOf(false) }
    var showUnlockConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SelfControlTopAppBar(
                title = "App Details",
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(AppDetailsEvent.Refresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading && state.app == null) {
                LoadingDialog(isLoading = true)
            } else if (state.app != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val app = state.app!!

                    // App Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (app.isBlocked) 
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (app.isBlocked) Icons.Filled.Block else Icons.Filled.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = if (app.isBlocked) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            AssistChip(
                                onClick = { },
                                label = { 
                                    Text(if (app.isBlocked) "BLOCKED" else "ALLOWED")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (app.isBlocked) Icons.Default.Lock else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (app.isBlocked) 
                                        MaterialTheme.colorScheme.errorContainer 
                                    else 
                                        MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }

                    // Details Section
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            DetailRow("Version", app.version.ifBlank { "Unknown" })
                            DetailRow("Type", if (app.isSystemApp) "System App" else "User App")
                            DetailRow(
                                "Installed", 
                                DateUtils.getRelativeTimeSpanString(app.installTime).toString()
                            )
                        }
                    }

                    // Policy Section
                    state.policy?.let { policy ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Policy",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                DetailRow("Status", if (policy.isBlocked) "Blocked" else "Allowed")
                                DetailRow("Locked", if (policy.isLocked) "Yes" else "No")
                                
                                policy.expiresAt?.let { expiresAt ->
                                    if (expiresAt > System.currentTimeMillis()) {
                                        DetailRow(
                                            "Expires", 
                                            DateUtils.getRelativeTimeSpanString(expiresAt).toString()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Block/Unblock Actions
                    if (app.isLocked) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                             Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                 Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                 Spacer(modifier = Modifier.width(8.dp))
                                 Text(
                                    "This app is locked by your administrator. You cannot unblock it locally.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                 )
                             }
                        }
                    } else if (app.isBlocked) {
                        Button(
                            onClick = { showUnblockConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Filled.LockOpen, contentDescription = null)

                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unblock App")
                        }
                    } else {
                        Button(
                            onClick = { showBlockConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.Block, contentDescription = null)

                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Block App")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Lock/Unlock Actions
                    val isLocked = state.policy?.isLocked ?: false
                    if (isLocked) {
                        OutlinedButton(
                            onClick = { showUnlockConfirm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.LockOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock App (Allow Uninstall)")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showLockConfirm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lock App (Prevent Uninstall)")
                        }
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
                                Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)

                                Spacer(modifier = Modifier.width(8.dp))
                                Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            } else {
                // Error state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("App not found", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    // Confirmation Dialogs
    if (showBlockConfirm) {
        ConfirmDialog(
            title = "Block App?",
            message = "Are you sure you want to block ${state.app?.name}? You won't be able to open it until unblocked.",
            onConfirm = {
                viewModel.onEvent(AppDetailsEvent.ToggleBlock)
                showBlockConfirm = false
            },
            onDismiss = { showBlockConfirm = false }
        )
    }

    if (showUnblockConfirm) {
        ConfirmDialog(
            title = "Unblock App?",
            message = "Are you sure you want to unblock ${state.app?.name}?",
            onConfirm = {
                viewModel.onEvent(AppDetailsEvent.ToggleBlock)
                showUnblockConfirm = false
            },
            onDismiss = { showUnblockConfirm = false }
        )
    }

    if (showLockConfirm) {
        ConfirmDialog(
            title = "Lock App?",
            message = "Are you sure you want to lock ${state.app?.name}? This will prevent uninstallation.",
            onConfirm = {
                viewModel.onEvent(AppDetailsEvent.ToggleLock)
                showLockConfirm = false
            },
            onDismiss = { showLockConfirm = false }
        )
    }

    if (showUnlockConfirm) {
        ConfirmDialog(
            title = "Unlock App?",
            message = "Are you sure you want to unlock ${state.app?.name}? This will allow uninstallation.",
            onConfirm = {
                viewModel.onEvent(AppDetailsEvent.ToggleLock)
                showUnlockConfirm = false
            },
            onDismiss = { showUnlockConfirm = false }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ViewModel
@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appRepository: AppRepository,
    private val policyRepository: PolicyRepository,
    private val applyPolicyUseCase: ApplyPolicyUseCase
) : ViewModel() {

    private val packageName: String = savedStateHandle.get<String>("packageName") ?: ""

    private val _uiState = MutableStateFlow(AppDetailsState())
    val uiState: StateFlow<AppDetailsState> = _uiState.asStateFlow()

    init {
        loadAppDetails()
    }

    fun onEvent(event: AppDetailsEvent) {
        when (event) {
            AppDetailsEvent.Refresh -> loadAppDetails()
            AppDetailsEvent.ToggleBlock -> toggleBlock()
            AppDetailsEvent.ToggleLock -> toggleLock()
        }
    }

    private fun loadAppDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                combine(
                    appRepository.observeApp(packageName),
                    policyRepository.observePolicyForApp(packageName)
                ) { app, policy ->
                    AppDetailsState(
                        app = app,
                        policy = policy,
                        isLoading = false
                    )
                }.catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun toggleBlock() {
        viewModelScope.launch {
            val currentApp = _uiState.value.app ?: return@launch
            val currentPolicy = _uiState.value.policy

            // Preserve the existing lock state when toggling block
            // IMPORTANT: Reuse existing policy ID to update, not create new
            val newPolicy = AppPolicy(
                id = currentPolicy?.id ?: java.util.UUID.randomUUID().toString(),
                packageName = packageName,
                isBlocked = !currentApp.isBlocked,
                isLocked = currentPolicy?.isLocked ?: false,
                lockAccessibility = currentPolicy?.lockAccessibility ?: false,
                reason = currentPolicy?.reason ?: "",
                createdAt = currentPolicy?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                expiresAt = currentPolicy?.expiresAt
            )

            when (val result = applyPolicyUseCase(newPolicy)) {
                is com.selfcontrol.domain.model.Result.Success -> {
                    // Success - flow will update UI
                }
                is com.selfcontrol.domain.model.Result.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> {}
            }
        }
    }

    private fun toggleLock() {
        viewModelScope.launch {
            val currentApp = _uiState.value.app ?: return@launch
            val currentPolicy = _uiState.value.policy

            // Preserve the existing block state when toggling lock
            // IMPORTANT: Reuse existing policy ID to update, not create new
            val newPolicy = AppPolicy(
                id = currentPolicy?.id ?: java.util.UUID.randomUUID().toString(),
                packageName = packageName,
                isBlocked = currentPolicy?.isBlocked ?: false,
                isLocked = !(currentPolicy?.isLocked ?: false),
                lockAccessibility = currentPolicy?.lockAccessibility ?: false,
                reason = currentPolicy?.reason ?: "",
                createdAt = currentPolicy?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                expiresAt = currentPolicy?.expiresAt
            )

            when (val result = applyPolicyUseCase(newPolicy)) {
                is com.selfcontrol.domain.model.Result.Success -> {
                    // Success - flow will update UI
                }
                is com.selfcontrol.domain.model.Result.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> {}
            }
        }
    }
}

data class AppDetailsState(
    val app: App? = null,
    val policy: AppPolicy? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class AppDetailsEvent {
    data object Refresh : AppDetailsEvent()
    data object ToggleBlock : AppDetailsEvent()
    data object ToggleLock : AppDetailsEvent()
}
