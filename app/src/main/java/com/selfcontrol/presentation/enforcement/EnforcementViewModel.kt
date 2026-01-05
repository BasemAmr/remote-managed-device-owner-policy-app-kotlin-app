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
            val services = mutableListOf<AccessibilityService>()
            
            // Try to get from repository first
            val allServices = accessibilityRepository.observeAllServices().first()
            
            for (serviceId in serviceIds) {
                val existing =   allServices.find { it.serviceId == serviceId }
                if (existing != null) {
                    services.add(existing)
                } else {
                    // Create service object directly
                    val componentName = ComponentName.unflattenFromString(serviceId)
                    if (componentName != null) {
                        val packageManager = context.packageManager
                        try {
                            val serviceInfo = packageManager.getServiceInfo(componentName, 0)
                            val label = serviceInfo.loadLabel(packageManager).toString()
                            
                            services.add(
                                AccessibilityService(
                                    serviceId = serviceId,
                                    packageName = componentName.packageName,
                                    serviceName = componentName.className,
                                    label = label,
                                    isEnabled = false,
                                    isLocked = true
                                )
                            )
                        } catch (e: Exception) {
                            // Fallback if service info not found
                            services.add(
                                AccessibilityService(
                                    serviceId = serviceId,
                                    packageName = componentName.packageName,
                                    serviceName = componentName.className,
                                    label = componentName.className.substringAfterLast('.'),
                                    isEnabled = false,
                                    isLocked = true
                                )
                            )
                        }
                    }
                }
            }
            
            _state.update { it.copy(disabledServices = services, isLoading = false) }
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
    
    /**
     * Open accessibility settings while staying in lock task mode
     * User must enable the service, then return to this screen
     */
    fun forceEnableService(serviceId: String) {
        viewModelScope.launch {
            try {
                val componentName = ComponentName.unflattenFromString(serviceId)
                if (componentName != null) {
                    // Open accessibility settings
                    // The user is still in lock task mode, so they can't escape
                    val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    
                    timber.log.Timber.i("[EnforcementVM] Opened accessibility settings")
                }
            } catch (e: Exception) {
                timber.log.Timber.e(e, "[EnforcementVM] Failed to open settings")
            }
        }
    }
}

data class EnforcementState(
    val disabledServices: List<AccessibilityService> = emptyList(),
    val allServicesEnabled: Boolean = false,
    val isLoading: Boolean = true  // Prevent early closure
)
