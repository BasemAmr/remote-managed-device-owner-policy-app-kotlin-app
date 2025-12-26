package com.selfcontrol.presentation.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.usecase.request.GetPendingRequestsUseCase
// import com.selfcontrol.domain.usecase.request.ApproveRequestUseCase // Assuming this exists or using generic ApplyPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RequestsViewModel @Inject constructor(
    private val getPendingRequestsUseCase: GetPendingRequestsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestsState())
    val uiState = _uiState.asStateFlow()

    init {
        loadRequests()
    }
    
    fun onEvent(event: RequestsEvent) {
        when(event) {
            RequestsEvent.Refresh -> loadRequests()
            is RequestsEvent.Approve -> approveRequest(event.requestId)
            is RequestsEvent.Deny -> denyRequest(event.requestId)
        }
    }

    private fun loadRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                getPendingRequestsUseCase()
                    .catch { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                    .collect { requests ->
                        _uiState.update { it.copy(isLoading = false, pendingRequests = requests) }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    private fun approveRequest(requestId: String) {
        // TODO: Implement Approve/Deny use cases
        // For now just removing from local list to simulate UI update
        // In real app, call approveRequestUseCase(requestId)
    }
    
    private fun denyRequest(requestId: String) {
        // TODO: Implement Deny logic
    }
}

sealed class RequestsEvent {
    data object Refresh : RequestsEvent()
    data class Approve(val requestId: String) : RequestsEvent()
    data class Deny(val requestId: String) : RequestsEvent()
}
