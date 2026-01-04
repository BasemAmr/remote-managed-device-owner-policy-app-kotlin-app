package com.selfcontrol.data.remote.api

import com.selfcontrol.data.remote.dto.*
import retrofit2.http.*

interface SelfControlApi {

    // Matches backend: const { device_name, android_id } = req.body;
    @POST("/api/device/register")
    suspend fun registerDevice(@Body body: Map<String, String>): ResponseWrapper<DeviceDto>

    // make it accept a list of policies
    @GET("/api/device/policies")
    suspend fun getPolicies(): ResponseWrapper<List<PolicyDto>>
    
    @POST("/api/device/policies")
    suspend fun applyPolicy(@Body policy: PolicyDto): ResponseWrapper<Unit>

    @POST("/api/device/violations")
    suspend fun logViolation(@Body violation: ViolationDto): ResponseWrapper<Unit>

    @POST("/api/device/violations/batch")
    suspend fun logViolationsBatch(@Body violations: List<ViolationDto>): ResponseWrapper<Unit>

    @POST("/api/device/requests")
    suspend fun createAccessRequest(@Body request: RequestDto): ResponseWrapper<RequestDto>

    @GET("/api/device/requests")
    suspend fun getAccessRequests(): ResponseWrapper<List<RequestDto>>

    @GET("/api/device/requests/{id}")
    suspend fun getAccessRequest(@Path("id") id: String): ResponseWrapper<RequestDto>

    @GET("/api/device/urls")
    suspend fun getUrlBlacklist(): List<UrlDto>
    
    @POST("/api/device/heartbeat")
    suspend fun sendHeartbeat(@Body data: Map<String, String>): ResponseWrapper<Unit>

    @POST("/api/device/apps")
    suspend fun uploadApps(@Body apps: AppUploadRequest): ResponseWrapper<Unit>

    @GET("/api/device/accessibility-services")
    suspend fun getAccessibilityServices(): List<AccessibilityServiceDto>

    @POST("/api/device/accessibility-services")
    suspend fun uploadAccessibilityServices(
        @Body services: Map<String, @JvmSuppressWildcards List<AccessibilityServiceDto>>
    ): ResponseWrapper<Unit>

    @GET("/api/device/accessibility-services/locked")
    suspend fun getLockedAccessibilityServices(): List<AccessibilityServiceDto>

    @POST("/api/device/accessibility-services/status")
    suspend fun reportAccessibilityStatus(
        @Body status: Map<String, @JvmSuppressWildcards Any>
    ): ResponseWrapper<Unit>

    @POST("/api/device/permissions")
    suspend fun uploadPermissions(@Body permissions: Map<String, List<PermissionDto>>): ResponseWrapper<Unit>
}
