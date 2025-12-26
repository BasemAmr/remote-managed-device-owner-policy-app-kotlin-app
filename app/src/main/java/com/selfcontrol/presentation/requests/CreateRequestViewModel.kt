package com.selfcontrol.presentation.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcontrol.domain.model.Request
import com.selfcontrol.domain.model.RequestType
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.usecase.request.CreateAccessRequestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRequestViewModel @Inject constructor(
    private val createAccessRequestUseCase: CreateAccessRequestUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateRequestState())
    val uiState: StateFlow<CreateRequestState> = _uiState.asStateFlow()

    fun onEvent(event: CreateRequestEvent) {
        when (event) {
            is CreateRequestEvent.Submit -> submitRequest(
                type = event.type,
                packageName = event.packageName,
                url = event.url,
                reason = event.reason,
                durationHours = event.durationHours
            )
        }
    }

    private fun submitRequest(
        type: RequestType,
        packageName: String,
        url: String,
        reason: String,
        durationHours: Int
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val request = Request(
                packageName = if (type == RequestType.APP_ACCESS || type == RequestType.TEMPORARY_UNBLOCK) packageName else "",
                url = if (type == RequestType.URL_ACCESS) url else "",
                type = type,
                reason = reason,
                expiresAt = System.currentTimeMillis() + (durationHours * 60 * 60 * 1000)
            )

            when (val result = createAccessRequestUseCase(request)) {
                is Result.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        requestCreated = true
                    ) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = result.message ?: "Failed to create request"
                    ) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}

data class CreateRequestState(
    val isLoading: Boolean = false,
    val requestCreated: Boolean = false,
    val error: String? = null
)

sealed class CreateRequestEvent {
    data class Submit(
        val type: RequestType,
        val packageName: String,
        val url: String,
        val reason: String,
        val durationHours: Int
    ) : CreateRequestEvent()
}
