package com.selfcontrol.data.remote.api

import com.selfcontrol.data.remote.dto.*
import retrofit2.http.*

/**
 * Retrofit API interface for Self-Control backend
 */
interface SelfControlApi {
    
    // ==================== Device Management ====================
    
    @POST("api/management/device/register")
    suspend fun registerDevice(
        @Body device: DeviceDto
    ): ResponseWrapper<DeviceDto>
    
    @POST("api/management/device/heartbeat")
    suspend fun sendHeartbeat(
        @Body deviceId: Map<String, String>
    ): ResponseWrapper<Unit>
    
    @GET("api/management/device/{deviceId}/status")
    suspend fun getDeviceStatus(
        @Path("deviceId") deviceId: String
    ): ResponseWrapper<DeviceDto>
    
    // ==================== Policy Management ====================
    
    @GET("api/management/policies")
    suspend fun getPolicies(
        @Query("device_id") deviceId: String
    ): ResponseWrapper<List<PolicyDto>>
    
    @POST("api/management/policy/apply")
    suspend fun applyPolicy(
        @Body policy: PolicyDto
    ): ResponseWrapper<PolicyDto>
    
    @PUT("api/management/policy/{policyId}")
    suspend fun updatePolicy(
        @Path("policyId") policyId: String,
        @Body policy: PolicyDto
    ): ResponseWrapper<PolicyDto>
    
    @DELETE("api/management/policy/{policyId}")
    suspend fun deletePolicy(
        @Path("policyId") policyId: String
    ): ResponseWrapper<Unit>
    
    // ==================== URL Blacklist ====================
    
    @GET("api/management/urls/{deviceId}")
    suspend fun getUrlBlacklist(
        @Path("deviceId") deviceId: String
    ): ResponseWrapper<List<UrlDto>>
    
    @POST("api/management/url")
    suspend fun addUrlToBlacklist(
        @Body url: UrlDto
    ): ResponseWrapper<UrlDto>
    
    @DELETE("api/management/url/{urlId}")
    suspend fun removeUrlFromBlacklist(
        @Path("urlId") urlId: String
    ): ResponseWrapper<Unit>
    
    // ==================== Access Requests ====================
    
    @POST("api/management/request")
    suspend fun createAccessRequest(
        @Body request: RequestDto
    ): ResponseWrapper<RequestDto>
    
    @GET("api/management/requests")
    suspend fun getAccessRequests(
        @Query("device_id") deviceId: String,
        @Query("status") status: String? = null
    ): ResponseWrapper<List<RequestDto>>
    
    @GET("api/management/request/{requestId}")
    suspend fun getAccessRequest(
        @Path("requestId") requestId: String
    ): ResponseWrapper<RequestDto>
    
    @PUT("api/management/request/{requestId}/status")
    suspend fun updateRequestStatus(
        @Path("requestId") requestId: String,
        @Body statusUpdate: Map<String, String>
    ): ResponseWrapper<RequestDto>
    
    // ==================== Violations ====================
    
    @POST("api/management/violation")
    suspend fun logViolation(
        @Body violation: ViolationDto
    ): ResponseWrapper<ViolationDto>
    
    @POST("api/management/violations/batch")
    suspend fun logViolationsBatch(
        @Body violations: List<ViolationDto>
    ): ResponseWrapper<List<ViolationDto>>
    
    @GET("api/management/violations")
    suspend fun getViolations(
        @Query("device_id") deviceId: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): ResponseWrapper<List<ViolationDto>>
}
