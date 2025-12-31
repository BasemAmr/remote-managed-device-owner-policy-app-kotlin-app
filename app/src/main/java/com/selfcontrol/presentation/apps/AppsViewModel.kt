package com.selfcontrol.presentation.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcontrol.domain.model.AppPolicy
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.usecase.app.GetInstalledAppsUseCase
import com.selfcontrol.domain.usecase.policy.ApplyPolicyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val applyPolicyUseCase: ApplyPolicyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsState())
    val uiState = _uiState.asStateFlow()

    init {
        loadApps()
    }

    fun onEvent(event: AppsEvent) {
        when (event) {
            is AppsEvent.Refresh -> loadApps()
            is AppsEvent.ToggleBlock -> toggleAppBlock(event.packageName, event.currentBlockStatus)
            is AppsEvent.Search -> filterApps(event.query)
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                getInstalledAppsUseCase()
                    .catch { e -> 
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                    .collect { apps ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                apps = apps,
                                filteredApps = if (it.searchQuery.isBlank()) apps else filterList(apps, it.searchQuery)
                            ) 
                        }
                    }
            } catch (e: Exception) {
               _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun toggleAppBlock(packageName: String, isBlocked: Boolean) {
        val app = _uiState.value.apps.find { it.packageName == packageName }
        if (app?.isLocked == true) {
            // Cannot toggle locked apps
            return
        }

        viewModelScope.launch {
            // Optimistic update
            val updatedApps = _uiState.value.apps.map {
                if (it.packageName == packageName) it.copy(isBlocked = !isBlocked) else it
            }
            _uiState.update { 
                it.copy(
                    apps = updatedApps,
                    filteredApps = filterList(updatedApps, it.searchQuery)
                ) 
            }

            val newPolicy = AppPolicy(
                packageName = packageName,
                isBlocked = !isBlocked, // Toggle
                isLocked = app?.isLocked ?: false // Preserve existing lock state
            )
            
            when (val result = applyPolicyUseCase(newPolicy)) {
                 is Result.Success -> {
                     // Success, the flow from loadApps will naturally update the UI when DB changes
                 }
                 is Result.Error -> {
                     // Revert on failure
                    val revertedApps = _uiState.value.apps.map {
                        if (it.packageName == packageName) it.copy(isBlocked = isBlocked) else it
                    }
                     _uiState.update { 
                        it.copy(
                            apps = revertedApps,
                            filteredApps = filterList(revertedApps, it.searchQuery),
                            error = "Failed to update policy: ${result.message}" 
                        ) 
                    }
                 }
                 else -> {}
            }
        }
    }
    
    private fun filterApps(query: String) {
        _uiState.update { 
            it.copy(
                searchQuery = query,
                filteredApps = filterList(it.apps, query)
            ) 
        }
    }
    
    private fun filterList(apps: List<com.selfcontrol.domain.model.App>, query: String): List<com.selfcontrol.domain.model.App> {
        if (query.isBlank()) return apps
        return apps.filter { app ->
            app.name.contains(query, ignoreCase = true) || app.packageName.contains(query, ignoreCase = true)
        }
    }
}

sealed class AppsEvent {
    data object Refresh : AppsEvent()
    data class ToggleBlock(val packageName: String, val currentBlockStatus: Boolean) : AppsEvent()
    data class Search(val query: String) : AppsEvent()
}
