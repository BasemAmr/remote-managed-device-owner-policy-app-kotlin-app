package com.selfcontrol.data.remote.api

import com.selfcontrol.data.remote.dto.*
import retrofit2.http.*

interface SelfControlApi {

    // Matches backend: const { device_name, android_id } = req.body;
    @POST("/api/device/register")
    suspend fun registerDevice(@Body body: Map<String, String>): ResponseWrapper<DeviceDto>

    @GET("/api/device/policies")
    suspend fun getPolicies(): ResponseWrapper<PolicyResponseDto>
    
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
    suspend fun getUrlBlacklist(): ResponseWrapper<List<UrlDto>>
    
    @POST("/api/device/heartbeat")
    suspend fun sendHeartbeat(@Body data: Map<String, String>): ResponseWrapper<Unit>

    @POST("/api/device/apps")
    suspend fun uploadApps(@Body apps: AppUploadRequest): ResponseWrapper<Unit>
}
