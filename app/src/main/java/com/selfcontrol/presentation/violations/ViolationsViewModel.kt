package com.selfcontrol.presentation.violations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcontrol.domain.usecase.violation.GetViolationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViolationsViewModel @Inject constructor(
    private val getViolationsUseCase: GetViolationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViolationsState())
    val uiState = _uiState.asStateFlow()

    init {
        loadViolations()
    }

    fun onEvent(event: ViolationsEvent) {
        when(event) {
            ViolationsEvent.Refresh -> loadViolations()
            ViolationsEvent.ClearAll -> { /* Implement clearing logs if needed */ }
        }
    }

    private fun loadViolations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                getViolationsUseCase()
                    .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                    .collect { list ->
                        _uiState.update { it.copy(isLoading = false, violations = list) }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}

sealed class ViolationsEvent {
    data object Refresh : ViolationsEvent()
    data object ClearAll : ViolationsEvent()
}
