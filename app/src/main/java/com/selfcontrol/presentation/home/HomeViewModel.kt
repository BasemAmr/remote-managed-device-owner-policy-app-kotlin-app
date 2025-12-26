package com.selfcontrol.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcontrol.domain.repository.AppRepository
import com.selfcontrol.domain.repository.ViolationRepository
import com.selfcontrol.data.local.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val violationRepository: ViolationRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.Refresh -> loadData()
            HomeEvent.GrantDeviceOwner -> {
                // Logic to start DPM flow if implemented
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Combine multiple flows
            combine(
                appRepository.observeAllApps(),
                prefs.isDeviceOwner,
                prefs.lastPolicySync,
                violationRepository.observeViolations() // Assuming basic observer exists
            ) { apps, isOwner, lastSync, violations ->
                HomeState(
                    blockedAppCount = apps.count { it.isBlocked },
                    totalAppCount = apps.size,
                    deviceOwnerActive = isOwner,
                    lastSyncTime = lastSync,
                    activeViolations = violations.size, // Simplified
                    isLoading = false
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
