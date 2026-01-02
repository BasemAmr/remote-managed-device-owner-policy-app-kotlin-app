package com.selfcontrol.presentation.enforcement

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcontrol.deviceowner.AccessibilityHelpers
import com.selfcontrol.domain.model.AccessibilityService
import com.selfcontrol.domain.repository.AccessibilityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnforcementViewModel @Inject constructor(
    private val accessibilityRepository: AccessibilityRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _state = MutableStateFlow(EnforcementState())
    val state: StateFlow<EnforcementState> = _state.asStateFlow()
    
    fun loadDisabledServices(serviceIds: List<String>) {
        viewModelScope.launch {
            val services = accessibilityRepository.observeAllServices().first()
            val disabled = services.filter { it.serviceId in serviceIds }
            
            _state.update { it.copy(disabledServices = disabled) }
        }
    }
    
    fun checkServiceStatus(serviceId: String) {
        viewModelScope.launch {
            val componentName = ComponentName.unflattenFromString(serviceId)
            if (componentName != null) {
                val isEnabled = AccessibilityHelpers.isAccessibilityServiceEnabled(context, componentName)
                
                if (isEnabled) {
                    // Remove from disabled list
                    _state.update { state ->
                        val updated = state.disabledServices.filter { it.serviceId != serviceId }
                        state.copy(
                            disabledServices = updated,
                            allServicesEnabled = updated.isEmpty()
                        )
                    }
                    
                    // Report to backend
                    accessibilityRepository.reportServiceStatus(serviceId, true)
                }
            }
        }
    }
}

data class EnforcementState(
    val disabledServices: List<AccessibilityService> = emptyList(),
    val allServicesEnabled: Boolean = false
)
