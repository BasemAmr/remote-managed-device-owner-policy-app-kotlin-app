package com.selfcontrol.di

import com.selfcontrol.BuildConfig
import com.selfcontrol.data.local.prefs.AppPreferences
import com.selfcontrol.data.remote.api.AuthInterceptor
import com.selfcontrol.data.remote.api.SelfControlApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideAuthInterceptor(prefs: AppPreferences): AuthInterceptor {
        return AuthInterceptor(prefs)
    }

    @Singleton
    @Provides
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        networkInterceptor: com.selfcontrol.data.remote.api.NetworkInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(networkInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Singleton
    @Provides
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        // Use a placeholder URL if not defined in BuildConfig yet
        val baseUrl = try {
            BuildConfig.API_URL
        } catch (e: Exception) {
            "http://localhost:3000/" // Fallback/Placeholder
        }
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun provideSelfControlApi(retrofit: Retrofit): SelfControlApi {
        return retrofit.create(SelfControlApi::class.java)
    }
}
