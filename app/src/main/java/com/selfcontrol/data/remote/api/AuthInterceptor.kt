package com.selfcontrol.data.remote.api

import com.selfcontrol.data.local.prefs.AppPreferences
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intercepts all API requests to inject JWT authentication token
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val appPreferences: AppPreferences
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip auth for public endpoints
        val url = originalRequest.url.toString()
        if (url.contains("/register") || url.contains("/public/")) {
            return chain.proceed(originalRequest)
        }
        
        // Get auth token from DataStore (blocking call in interceptor)
        val token = runBlocking {
            appPreferences.authToken.firstOrNull()
        }
        
        // Add Authorization header if token exists
        val authorizedRequest = if (!token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build()
        } else {
            Timber.w("[AuthInterceptor] No auth token available for ${originalRequest.url}")
            originalRequest.newBuilder()
                .header("Content-Type", "application/json")
                .build()
        }
        
        return chain.proceed(authorizedRequest)
    }
}
