package com.selfcontrol.presentation.accessibility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcontrol.domain.repository.AccessibilityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AccessibilityServicesViewModel @Inject constructor(
    private val accessibilityRepository: AccessibilityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessibilityServicesState())
    val uiState: StateFlow<AccessibilityServicesState> = _uiState.asStateFlow()

    init {
        loadServices()
    }

    fun onEvent(event: AccessibilityEvent) {
        when (event) {
            AccessibilityEvent.Refresh -> loadServices()
            AccessibilityEvent.ScanAndSync -> scanAndSync()
        }
    }

    private fun loadServices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            accessibilityRepository.observeAllServices()
                .catch { e ->
                    Timber.e(e, "[AccessibilityVM] Error loading services")
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { services ->
                    _uiState.update {
                        it.copy(
                            services = services,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    private fun scanAndSync() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isScanning = true) }
                
                Timber.i("[AccessibilityVM] Scanning services...")
                accessibilityRepository.scanAndSyncServices()
                
                Timber.i("[AccessibilityVM] Syncing locked services...")
                accessibilityRepository.syncLockedServicesFromBackend()
                
                Timber.i("[AccessibilityVM] Scan complete")
                _uiState.update { it.copy(isScanning = false) }
                
            } catch (e: Exception) {
                Timber.e(e, "[AccessibilityVM] Scan failed")
                _uiState.update { 
                    it.copy(
                        isScanning = false,
                        error = e.message
                    )
                }
            }
        }
    }
}
