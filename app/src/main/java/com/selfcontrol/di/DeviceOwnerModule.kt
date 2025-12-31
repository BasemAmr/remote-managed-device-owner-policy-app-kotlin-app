package com.selfcontrol.di

import android.content.Context
import com.selfcontrol.data.local.prefs.AppPreferences
import com.selfcontrol.domain.repository.AppRepository
import com.selfcontrol.domain.repository.PolicyRepository
import com.selfcontrol.domain.repository.ViolationRepository
import com.selfcontrol.deviceowner.AppBlockManager
import com.selfcontrol.deviceowner.DeviceOwnerManager
import com.selfcontrol.deviceowner.PackageMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DeviceOwnerModule - Provides Device Owner related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DeviceOwnerModule {
    
    @Provides
    @Singleton
    fun provideDeviceOwnerManager(
        @ApplicationContext context: Context,
        prefs: AppPreferences
    ): DeviceOwnerManager {
        return DeviceOwnerManager(context, prefs)
    }
    
    @Provides
    @Singleton
    fun provideAppBlockManager(
        deviceOwnerManager: DeviceOwnerManager,
        policyRepository: PolicyRepository,
        violationRepository: ViolationRepository
    ): AppBlockManager {
        return AppBlockManager(
            deviceOwnerManager,
            policyRepository,
            violationRepository
        )
    }
    
    @Provides
    @Singleton
    fun providePackageMonitor(
        @ApplicationContext context: Context,
        appBlockManager: AppBlockManager,
        appRepository: AppRepository
    ): PackageMonitor {
        return PackageMonitor(context, appBlockManager, appRepository)
    }
}
