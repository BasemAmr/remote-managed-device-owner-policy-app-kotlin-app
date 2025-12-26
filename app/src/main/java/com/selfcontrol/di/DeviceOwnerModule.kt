package com.selfcontrol.di

import android.content.Context
import com.selfcontrol.data.repository.PolicyRepositoryImpl
import com.selfcontrol.data.repository.ViolationRepositoryImpl
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
        @ApplicationContext context: Context
    ): DeviceOwnerManager {
        return DeviceOwnerManager(context)
    }
    
    @Provides
    @Singleton
    fun provideAppBlockManager(
        deviceOwnerManager: DeviceOwnerManager,
        policyRepository: PolicyRepositoryImpl,
        violationRepository: ViolationRepositoryImpl
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
        appBlockManager: AppBlockManager
    ): PackageMonitor {
        return PackageMonitor(context, appBlockManager)
    }
}
