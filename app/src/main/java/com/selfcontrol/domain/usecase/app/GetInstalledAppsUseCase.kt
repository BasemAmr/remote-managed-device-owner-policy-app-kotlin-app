package com.selfcontrol.domain.usecase.app

import com.selfcontrol.domain.model.App
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.repository.AppRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to get all installed apps
 */
class GetInstalledAppsUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    /**
     * Get all installed apps as Flow (reactive)
     */
    operator fun invoke(): Flow<List<App>> {
        Timber.d("[GetInstalledApps] Observing installed apps")
        return appRepository.observeAllApps()
    }
    
    /**
     * Get user-installed apps only (exclude system apps)
     */
    fun getUserApps(): Flow<List<App>> {
        Timber.d("[GetInstalledApps] Observing user apps")
        return appRepository.observeUserApps()
    }
    
    /**
     * Refresh apps from PackageManager
     */
    suspend fun refresh(): Result<List<App>> {
        Timber.i("[GetInstalledApps] Refreshing installed apps")
        return appRepository.refreshInstalledApps()
    }
}
