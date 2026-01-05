package com.selfcontrol.data.remote.api

import com.selfcontrol.BuildConfig
import com.selfcontrol.data.local.dao.ApiLogDao
import com.selfcontrol.data.local.entity.ApiLogEntity
import com.selfcontrol.data.local.prefs.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor that logs all API requests and responses to database for debugging
 * NOTE: Database logging is DISABLED in release builds for performance
 */
@Singleton
class ApiLoggingInterceptor @Inject constructor(
    private val apiLogDao: ApiLogDao,
    private val appPreferences: AppPreferences
) : Interceptor {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun intercept(chain: Interceptor.Chain): Response {
        // In release builds, skip all logging overhead - just pass through
        if (!BuildConfig.DEBUG) {
            return chain.proceed(chain.request())
        }

        val request = chain.request()
        val startTime = System.currentTimeMillis()
        
        // Extract request details
        val url = request.url.toString()
        val method = request.method
        val requestBody = try {
            request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            }
        } catch (e: Exception) {
            null
        }
        
        // Check if token is present (read synchronously in IO context)
        var hasToken = false
        try {
            // We need to check token presence without blocking
            // Use runBlocking here as we're already in an interceptor (synchronous context)
            kotlinx.coroutines.runBlocking {
                hasToken = appPreferences.authToken.first() != null
            }
        } catch (e: Exception) {
            Timber.e(e, "[ApiLoggingInterceptor] Failed to check token presence")
        }
        
        // Proceed with request
        val response: Response
        var error: String? = null
        
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            error = "${e.javaClass.simpleName}: ${e.message}"
            Timber.e(e, "[ApiLoggingInterceptor] Request failed: $url")
            
            // Log the error
            val duration = System.currentTimeMillis() - startTime
            logToDatabase(url, method, requestBody, null, null, hasToken, duration, error)
            
            throw e
        }
        
        // Extract response details
        val duration = System.currentTimeMillis() - startTime
        val responseCode = response.code
        val responseBody = try {
            response.peekBody(Long.MAX_VALUE).string()
        } catch (e: Exception) {
            null
        }
        
        // Log to database asynchronously
        logToDatabase(url, method, requestBody, responseCode, responseBody, hasToken, duration, error)
        
        return response
    }
    
    private fun logToDatabase(
        url: String,
        method: String,
        requestBody: String?,
        responseCode: Int?,
        responseBody: String?,
        hasToken: Boolean,
        duration: Long,
        error: String?
    ) {
        // Skip database logging in release builds for performance
        if (!BuildConfig.DEBUG) return
        
        scope.launch {
            try {
                val log = ApiLogEntity(
                    url = url,
                    method = method,
                    requestBody = requestBody?.take(1000), // Limit to 1000 chars
                    responseCode = responseCode,
                    responseBody = responseBody?.take(1000), // Limit to 1000 chars
                    hasToken = hasToken,
                    duration = duration,
                    error = error
                )
                
                apiLogDao.insertLog(log)
                
                Timber.d("[ApiLoggingInterceptor] Logged: $method $url (${responseCode ?: "error"}) ${duration}ms")
                
            } catch (e: Exception) {
                Timber.e(e, "[ApiLoggingInterceptor] Failed to log API call")
            }
        }
    }
}
