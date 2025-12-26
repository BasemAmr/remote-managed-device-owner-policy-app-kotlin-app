package com.selfcontrol.di

import com.selfcontrol.data.repository.*
import com.selfcontrol.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppRepository(
        appRepositoryImpl: AppRepositoryImpl
    ): AppRepository

    @Binds
    @Singleton
    abstract fun bindPolicyRepository(
        policyRepositoryImpl: PolicyRepositoryImpl
    ): PolicyRepository

    @Binds
    @Singleton
    abstract fun bindRequestRepository(
        requestRepositoryImpl: RequestRepositoryImpl
    ): RequestRepository

    @Binds
    @Singleton
    abstract fun bindViolationRepository(
        violationRepositoryImpl: ViolationRepositoryImpl
    ): ViolationRepository

    @Binds
    @Singleton
    abstract fun bindUrlRepository(
        urlRepositoryImpl: UrlRepositoryImpl
    ): UrlRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}
