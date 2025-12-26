package com.selfcontrol.data.remote.api

import com.selfcontrol.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network interceptor for logging, retry logic, and error handling
 */
@Singleton
class NetworkInterceptor @Inject constructor() : Interceptor {
    
    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Log request in debug mode
        if (BuildConfig.DEBUG) {
            Timber.d("[API] → ${request.method} ${request.url}")
            request.body?.let { body ->
                Timber.d("[API] → Body: ${body.contentLength()} bytes")
            }
        }
        
        var response: Response? = null
        var lastException: IOException? = null
        
        // Retry logic for network failures
        for (attempt in 1..MAX_RETRIES) {
            try {
                response = chain.proceed(request)
                
                // Log response
                if (BuildConfig.DEBUG) {
                    Timber.d("[API] ← ${response.code} ${request.url} (attempt $attempt)")
                }
                
                // If successful, return response
                if (response.isSuccessful) {
                    return response
                }
                
                // Handle specific error codes
                when (response.code) {
                    401 -> {
                        Timber.w("[API] Unauthorized - token may be expired")
                        // Don't retry 401s
                        return response
                    }
                    403 -> {
                        Timber.w("[API] Forbidden - insufficient permissions")
                        return response
                    }
                    404 -> {
                        Timber.w("[API] Not found: ${request.url}")
                        return response
                    }
                    429 -> {
                        Timber.w("[API] Rate limited - backing off")
                        Thread.sleep(RETRY_DELAY_MS * attempt)
                        continue
                    }
                    in 500..599 -> {
                        Timber.e("[API] Server error ${response.code} - retrying")
                        Thread.sleep(RETRY_DELAY_MS * attempt)
                        continue
                    }
                    else -> {
                        Timber.w("[API] HTTP ${response.code}: ${request.url}")
                        return response
                    }
                }
                
            } catch (e: IOException) {
                lastException = e
                Timber.e(e, "[API] Network error on attempt $attempt/$MAX_RETRIES")
                
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * attempt)
                }
            }
        }
        
        // All retries failed
        if (lastException != null) {
            Timber.e(lastException, "[API] All retry attempts failed for ${request.url}")
            throw lastException
        }
        
        // Return last response if available
        return response ?: throw IOException("No response after $MAX_RETRIES attempts")
    }
}
