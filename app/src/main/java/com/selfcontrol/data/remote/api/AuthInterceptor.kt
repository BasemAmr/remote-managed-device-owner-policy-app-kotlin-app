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
        val request = chain.request()
        val requestBuilder = request.newBuilder()

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

        // 2. Log detailed information for debugging
        val url = request.url.toString()
        Timber.i("[AuthInterceptor] Request URL: $url")
        Timber.i("[AuthInterceptor] Token present: ${!token.isNullOrBlank()}, Length: ${token?.length ?: 0}")
        
        if (token.isNullOrBlank()) {
            Timber.w("[AuthInterceptor] ⚠️ WARNING: No auth token found! Request will likely fail.")
        } else {
            Timber.d("[AuthInterceptor] Token preview: ${token.take(20)}...")
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        
        val response = chain.proceed(requestBuilder.build())
        Timber.i("[AuthInterceptor] Request sent with auth: ${!token.isNullOrBlank()}, Response code: ${response.code}")

        return response
    }
}
