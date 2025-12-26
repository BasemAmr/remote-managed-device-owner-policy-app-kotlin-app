package com.selfcontrol.presentation.violations

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.selfcontrol.domain.model.Violation
import com.selfcontrol.presentation.components.EmptyState
import com.selfcontrol.presentation.components.ErrorScreen
import com.selfcontrol.presentation.components.LoadingDialog
import com.selfcontrol.presentation.components.SelfControlTopAppBar
import com.selfcontrol.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViolationsScreen(
    navigationActions: NavigationActions,
    viewModel: ViolationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            SelfControlTopAppBar(
                title = "Violations Log",
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading && state.violations.isEmpty()) {
                LoadingDialog(isLoading = true)
            } else if (state.error != null) {
                ErrorScreen(state.error ?: "Error") { viewModel.onEvent(ViolationsEvent.Refresh) }
            } else if (state.violations.isEmpty()) {
                EmptyState("No violations recorded")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.violations) { violation ->
                        ViolationItem(violation)
                    }
                }
            }
        }
    }
}

@Composable
fun ViolationItem(violation: Violation) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = violation.packageName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Type: ${violation.type}", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = DateUtils.getRelativeTimeSpanString(violation.timestamp).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
