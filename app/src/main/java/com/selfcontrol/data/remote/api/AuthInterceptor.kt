package com.selfcontrol.data.remote.api

import com.selfcontrol.data.local.prefs.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val appPreferences: AppPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // 1. FORCE WAIT for the token (Blocking call)
        // This ensures we don't proceed until we check the disk
        val token = runBlocking {
            try {
                appPreferences.authToken.first()
            } catch (e: Exception) {
                Timber.e(e, "[AuthInterceptor] Failed to read token")
                null
            }
        }

        // 2. Log exactly what is happening
        if (token.isNullOrBlank()) {
            Timber.w("[AuthInterceptor] ⚠️ WARNING: No auth token found! Request will likely fail.")
        } else {
            // Timber.d("[AuthInterceptor] Attaching token: ${token.take(10)}...") // Uncomment to debug
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
