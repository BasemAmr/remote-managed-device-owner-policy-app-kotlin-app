package com.selfcontrol.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.selfcontrol.data.worker.AppSyncWorker
import com.selfcontrol.data.worker.PolicySyncWorker
import com.selfcontrol.data.worker.UrlBlacklistSyncWorker
import com.selfcontrol.domain.repository.AppRepository
import com.selfcontrol.domain.repository.ViolationRepository
import com.selfcontrol.data.local.prefs.AppPreferences
import com.selfcontrol.deviceowner.DeviceOwnerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val violationRepository: ViolationRepository,
    private val urlRepository: com.selfcontrol.domain.repository.UrlRepository,
    private val prefs: AppPreferences,
    private val deviceOwnerManager: DeviceOwnerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()
    
    private val workManager = WorkManager.getInstance(context)

    init {
        loadData()
        observeSyncWorkerStatus()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.Refresh -> loadData()
            HomeEvent.GrantDeviceOwner -> {
                // Logic to start DPM flow if implemented
            }
            HomeEvent.RemoveDeviceOwner -> {
                deviceOwnerManager.clearDeviceOwner()
                loadData()
            }
            HomeEvent.SyncAllApps -> {
                triggerManualAppSync()
            }
            HomeEvent.SyncAllPolicies -> {
                triggerManualPolicySync()
            }
            HomeEvent.SyncAllUrls -> {
                triggerManualUrlSync()
            }
        }
    }
    
    /**
     * Trigger manual app sync - "Sync Apps Now" button
     */
    private fun triggerManualAppSync() {
        viewModelScope.launch {
            try {
                // Update UI to show syncing status
                _uiState.update { 
                    it.copy(
                        isSyncingApps = true, 
                        syncStatusMessage = "Syncing..."
                    ) 
                }
                
                // Reset any failed sync apps so they can be retried
                appRepository.resetFailedSyncApps()
                
                // Create input data for manual sync
                val inputData = Data.Builder()
                    .putBoolean(AppSyncWorker.KEY_IS_MANUAL_SYNC, true)
                    .putBoolean(AppSyncWorker.KEY_IS_IMMEDIATE_SYNC, true)
                    .build()
                
                val appSyncWork = OneTimeWorkRequestBuilder<AppSyncWorker>()
                    .setInputData(inputData)
                    .addTag("manual_app_sync")
                    .build()
                
                workManager.enqueueUniqueWork(
                    "manual_app_sync",
                    ExistingWorkPolicy.REPLACE,
                    appSyncWork
                )
                
                Timber.i("[HomeViewModel] ⚡ Manual app sync triggered")
                
            } catch (e: Exception) {
                Timber.e(e, "[HomeViewModel] Failed to trigger manual app sync")
                _uiState.update { 
                    it.copy(
                        isSyncingApps = false, 
                        syncStatusMessage = "Failed to start sync",
                        error = e.message
                    ) 
                }
            }
        }
    }
    
    /**
     * Observe sync worker status to update UI
     */
    private fun observeSyncWorkerStatus() {
        viewModelScope.launch {
            // Observe App Sync
            launch {
                workManager.getWorkInfosForUniqueWorkFlow("manual_app_sync")
                    .collect { workInfos ->
                        val workInfo = workInfos.firstOrNull()
                        when (workInfo?.state) {
                            WorkInfo.State.RUNNING -> {
                                _uiState.update { 
                                    it.copy(
                                        isSyncingApps = true, 
                                        syncStatusMessage = "Syncing Apps..."
                                    ) 
                                }
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                _uiState.update { 
                                    it.copy(
                                        isSyncingApps = false, 
                                        syncStatusMessage = "App sync successful!",
                                        pendingSyncCount = 0
                                    ) 
                                }
                                delay(3000)
                                _uiState.update { it.copy(syncStatusMessage = null) }
                            }
                            WorkInfo.State.FAILED -> {
                                _uiState.update { 
                                    it.copy(
                                        isSyncingApps = false, 
                                        syncStatusMessage = "App sync failed"
                                    ) 
                                }
                            }
                            else -> { /* No action needed */ }
                        }
                    }
            }

            // Observe Policy Sync
            launch {
                workManager.getWorkInfosForUniqueWorkFlow("manual_policy_sync")
                    .collect { workInfos ->
                        val workInfo = workInfos.firstOrNull()
                        when (workInfo?.state) {
                            WorkInfo.State.RUNNING -> {
                                _uiState.update { 
                                    it.copy(
                                        isSyncingPolicies = true, 
                                        policySyncStatusMessage = "Syncing Policies..."
                                    ) 
                                }
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                _uiState.update { 
                                    it.copy(
                                        isSyncingPolicies = false, 
                                        policySyncStatusMessage = "Policies synced!"
                                    ) 
                                }
                                delay(3000)
                                _uiState.update { it.copy(policySyncStatusMessage = null) }
                            }
                            WorkInfo.State.FAILED -> {
                                _uiState.update { 
                                    it.copy(
                                        isSyncingPolicies = false, 
                                        policySyncStatusMessage = "Policy sync failed"
                                    ) 
                                }
                            }
                            else -> { /* No action needed */ }
                        }
                    }
            }

            // Observe URL Sync
            launch {
                workManager.getWorkInfosForUniqueWorkFlow("manual_url_sync")
                    .collect { workInfos ->
                        val workInfo = workInfos.firstOrNull()
                        when (workInfo?.state) {
                            WorkInfo.State.RUNNING -> {
                                _uiState.update { 
                                    it.copy(
                                        isSyncingUrls = true, 
                                        urlSyncStatusMessage = "Syncing URLs..."
                                    ) 
                                }
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                _uiState.update { 
                                    it.copy(
                                        isSyncingUrls = false, 
                                        urlSyncStatusMessage = "URLs synced!"
                                    ) 
                                }
                                delay(3000)
                                _uiState.update { it.copy(urlSyncStatusMessage = null) }
                            }
                            WorkInfo.State.FAILED -> {
                                _uiState.update { 
                                    it.copy(
                                        isSyncingUrls = false, 
                                        urlSyncStatusMessage = "URL sync failed"
                                    ) 
                                }
                            }
                            else -> { /* No action needed */ }
                        }
                    }
            }
        }
    }

    private fun triggerManualPolicySync() {
        viewModelScope.launch {
            try {
                _uiState.update { 
                    it.copy(
                        isSyncingPolicies = true, 
                        policySyncStatusMessage = "Syncing Policies..."
                    ) 
                }

                val policySyncWork = OneTimeWorkRequestBuilder<PolicySyncWorker>()
                    .addTag("manual_policy_sync")
                    .build()

                workManager.enqueueUniqueWork(
                    "manual_policy_sync",
                    ExistingWorkPolicy.REPLACE,
                    policySyncWork
                )

                Timber.i("[HomeViewModel] ⚡ Manual policy sync triggered")
            } catch (e: Exception) {
                Timber.e(e, "Failed to trigger manual policy sync")
                _uiState.update { 
                    it.copy(
                        isSyncingPolicies = false,
                        policySyncStatusMessage = "Sync failed"
                    )
                }
            }
        }
    }

    private fun triggerManualUrlSync() {
        viewModelScope.launch {
            try {
                _uiState.update { 
                    it.copy(
                        isSyncingUrls = true, 
                        urlSyncStatusMessage = "Syncing URLs..."
                    ) 
                }

                val urlSyncWork = OneTimeWorkRequestBuilder<UrlBlacklistSyncWorker>()
                    .addTag("manual_url_sync")
                    .build()

                workManager.enqueueUniqueWork(
                    "manual_url_sync",
                    ExistingWorkPolicy.REPLACE,
                    urlSyncWork
                )

                Timber.i("[HomeViewModel] ⚡ Manual URL sync triggered")
            } catch (e: Exception) {
                Timber.e(e, "Failed to trigger manual URL sync")
                _uiState.update { 
                    it.copy(
                        isSyncingUrls = false,
                        urlSyncStatusMessage = "Sync failed"
                    )
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Combine first 5 flows (standard combine supports up to 5)
            val baseFlow = combine(
                appRepository.observeAllApps(),
                prefs.isDeviceOwner,
                prefs.vpnConnected,
                prefs.lastPolicySync,
                violationRepository.observeViolations()
            ) { apps, isOwner, vpnConnected, lastSync, violations ->
                HomeState(
                    blockedAppCount = apps.count { it.isBlocked },
                    totalAppCount = apps.size,
                    deviceOwnerActive = isOwner,
                    vpnConnected = vpnConnected,
                    lastSyncTime = lastSync,
                    activeViolations = violations.size
                )
            }

            // Combine with pending sync count and blocked URL count
            combine(
                baseFlow,
                appRepository.observePendingSyncCount(),
                urlRepository.observeBlockedUrls()
            ) { baseState, pendingCount, blockedUrls ->
                baseState.copy(
                    pendingSyncCount = pendingCount,
                    blockedUrlCount = blockedUrls.size,
                    isLoading = false
                )
            }.catch { e ->
                Timber.e(e, "[HomeViewModel] Error loading data")
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}