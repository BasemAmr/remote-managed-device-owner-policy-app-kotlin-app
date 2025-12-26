package com.selfcontrol.presentation.blocked

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcontrol.domain.model.Request
import com.selfcontrol.domain.model.RequestType
import com.selfcontrol.domain.repository.AppRepository
import com.selfcontrol.domain.repository.RequestRepository
import com.selfcontrol.domain.usecase.request.CreateAccessRequestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlockedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appRepository: AppRepository,
    private val requestRepository: RequestRepository,
    private val createAccessRequestUseCase: CreateAccessRequestUseCase
) : ViewModel() {

    private val packageName: String = savedStateHandle.get<String>("packageName") ?: ""

    private val _uiState = MutableStateFlow(BlockedState())
    val uiState: StateFlow<BlockedState> = _uiState.asStateFlow()

    init {
        loadAppInfo()
        checkExistingRequest()
    }

    fun onEvent(event: BlockedEvent) {
        when (event) {
            is BlockedEvent.RequestAccess -> createAccessRequest(event.reason, event.durationHours)
            BlockedEvent.Dismiss -> {
                // Navigation handled in UI
            }
        }
    }

    private fun loadAppInfo() {
        viewModelScope.launch {
            try {
                val app = appRepository.getAppByPackageName(packageName)
                _uiState.update { it.copy(
                    packageName = packageName,
                    appName = app?.name ?: packageName,
                    isLoading = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    packageName = packageName,
                    appName = packageName,
                    isLoading = false,
                    error = e.message
                ) }
            }
        }
    }

    private fun checkExistingRequest() {
        viewModelScope.launch {
            requestRepository.observePendingRequestsForApp(packageName)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { requests ->
                    val pendingRequest = requests.firstOrNull { it.isPending() }
                    _uiState.update { it.copy(
                        hasExistingRequest = pendingRequest != null,
                        existingRequest = pendingRequest
                    ) }
                }
        }
    }

    private fun createAccessRequest(reason: String, durationHours: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingRequest = true, error = null) }

            val request = Request(
                packageName = packageName,
                type = RequestType.APP_ACCESS,
                reason = reason,
                expiresAt = System.currentTimeMillis() + (durationHours * 60 * 60 * 1000)
            )

            when (val result = createAccessRequestUseCase(request)) {
                is com.selfcontrol.domain.model.Result.Success -> {
                    _uiState.update { it.copy(
                        isCreatingRequest = false,
                        requestCreated = true,
                        hasExistingRequest = true
                    ) }
                }
                is com.selfcontrol.domain.model.Result.Error -> {
                    _uiState.update { it.copy(
                        isCreatingRequest = false,
                        error = result.message ?: "Failed to create request"
                    ) }
                }
                else -> {
                    _uiState.update { it.copy(isCreatingRequest = false) }
                }
            }
        }
    }
}

data class BlockedState(
    val packageName: String = "",
    val appName: String = "",
    val isLoading: Boolean = true,
    val isCreatingRequest: Boolean = false,
    val hasExistingRequest: Boolean = false,
    val existingRequest: Request? = null,
    val requestCreated: Boolean = false,
    val error: String? = null
)

sealed class BlockedEvent {
    data class RequestAccess(val reason: String, val durationHours: Int) : BlockedEvent()
    data object Dismiss : BlockedEvent()
}
