package com.selfcontrol.data.repository

import com.selfcontrol.data.local.dao.RequestDao
import com.selfcontrol.data.local.entity.RequestEntity
import com.selfcontrol.data.local.prefs.AppPreferences
import com.selfcontrol.data.remote.api.SelfControlApi
import com.selfcontrol.data.remote.mapper.RequestMapper
import com.selfcontrol.domain.model.Request
import com.selfcontrol.domain.model.RequestStatus
import com.selfcontrol.domain.model.RequestType
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.repository.RequestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of RequestRepository
 * Handles access approval requests
 */
@Singleton
class RequestRepositoryImpl @Inject constructor(
    private val requestDao: RequestDao,
    private val api: SelfControlApi,
    private val mapper: RequestMapper,
    private val prefs: AppPreferences
) : RequestRepository {
    
    override fun observeAllRequests(): Flow<List<Request>> {
        return requestDao.observeAllRequests()
            .map { entities -> entities.map { entityToDomain(it) } }
    }
    
    override fun observeRequestsByStatus(status: RequestStatus): Flow<List<Request>> {
        return requestDao.observeRequestsByStatus(status.name.lowercase())
            .map { entities -> entities.map { entityToDomain(it) } }
    }
    
    override fun observePendingRequests(): Flow<List<Request>> {
        return requestDao.observePendingRequests()
            .map { entities -> entities.map { entityToDomain(it) } }
    }
    
    override fun observeRequestsForApp(packageName: String): Flow<List<Request>> {
        return requestDao.observeRequestsForApp(packageName)
            .map { entities -> entities.map { entityToDomain(it) } }
    }
    
    override fun observeRequest(requestId: String): Flow<Request?> {
        return requestDao.observeRequest(requestId)
            .map { entity -> entity?.let { entityToDomain(it) } }
    }
    
    override suspend fun getRequest(requestId: String): Result<Request?> {
        return try {
            val entity = requestDao.getRequest(requestId)
            Result.Success(entity?.let { entityToDomain(it) })
        } catch (e: Exception) {
            Timber.e(e, "[RequestRepo] Failed to get request $requestId")
            Result.Error(e)
        }
    }
    
    override suspend fun createRequest(request: Request) {
        try {
            // Save locally first
            val entity = domainToEntity(request)
            requestDao.insertRequest(entity)
            
            // Sync to server (fire and forget for now, or use background worker)
            val deviceId = prefs.deviceId.firstOrNull()
            if (deviceId != null) {
                val dto = mapper.toDto(request, deviceId)
                val response = api.createAccessRequest(dto)
                if (response.success && response.data != null) {
                    val serverRequest = mapper.toDomain(response.data)
                    requestDao.insertRequest(domainToEntity(serverRequest))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "[RequestRepo] Failed to create request")
        }
    }
    
    override suspend fun updateRequestStatus(
        requestId: String,
        status: RequestStatus,
        reviewerNote: String?
    ): Result<Unit> {
        return try {
            val entity = requestDao.getRequest(requestId)
                ?: return Result.Error(Exception("Request not found"))
            
            val updatedEntity = entity.copy(
                status = status.name.lowercase(),
                reviewedAt = System.currentTimeMillis(),
                reviewerNote = reviewerNote
            )
            
            requestDao.updateRequest(updatedEntity)
            
            Timber.i("[RequestRepo] Updated request $requestId to $status")
            Result.Success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "[RequestRepo] Failed to update request status")
            Result.Error(e)
        }
    }
    
    override suspend fun fetchLatestRequests(): Result<List<Request>> {
        return try {
            val deviceId = prefs.deviceId.firstOrNull() ?: return Result.Error(Exception("No device ID"))
            
            val response = api.getAccessRequests()
            
            if (response.success && response.data != null) {
                val requests = mapper.toDomainList(response.data)
                
                // Save to local database
                val entities = requests.map { domainToEntity(it) }
                requestDao.insertRequests(entities)
                
                Timber.i("[RequestRepo] Fetched ${requests.size} requests from server")
                Result.Success(requests)
            } else {
                Result.Error(Exception(response.message ?: "Failed to fetch requests"))
            }
        } catch (e: Exception) {
            Timber.e(e, "[RequestRepo] Failed to fetch requests from server")
            Result.Error(e)
        }
    }
    
    override suspend fun checkRequestStatus(requestId: String): Result<Request> {
        return try {
            val response = api.getAccessRequest(requestId)
            
            if (response.success && response.data != null) {
                val request = mapper.toDomain(response.data)
                
                // Update local database
                requestDao.insertRequest(domainToEntity(request))
                
                Timber.d("[RequestRepo] Checked status for request $requestId: ${request.status}")
                Result.Success(request)
            } else {
                Result.Error(Exception(response.message ?: "Failed to check status"))
            }
        } catch (e: Exception) {
            Timber.e(e, "[RequestRepo] Failed to check request status")
            Result.Error(e)
        }
    }
    
    override suspend fun getPendingCount(): Result<Int> {
        return try {
            val count = requestDao.getPendingCount()
            Result.Success(count)
        } catch (e: Exception) {
            Timber.e(e, "[RequestRepo] Failed to get pending count")
            Result.Error(e)
        }
    }
    
    override suspend fun deleteRequest(requestId: String): Result<Unit> {
        return try {
            requestDao.deleteById(requestId)
            Timber.d("[RequestRepo] Deleted request $requestId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[RequestRepo] Failed to delete request")
            Result.Error(e)
        }
    }
    
    
    // ==================== Mappers ====================
    
    private fun entityToDomain(entity: RequestEntity): Request {
        return Request(
            id = entity.id,
            packageName = entity.packageName,
            appName = entity.appName,
            type = parseType(entity.type),
            reason = entity.reason,
            status = parseStatus(entity.status),
            requestedAt = entity.requestedAt,
            reviewedAt = entity.reviewedAt,
            expiresAt = entity.expiresAt,
            reviewerNote = entity.reviewerNote
        )
    }
    
    private fun domainToEntity(domain: Request): RequestEntity {
        return RequestEntity(
            id = domain.id,
            packageName = domain.packageName,
            appName = domain.appName,
            type = domain.type.name.lowercase(),
            reason = domain.reason,
            status = domain.status.name.lowercase(),
            requestedAt = domain.requestedAt,
            reviewedAt = domain.reviewedAt,
            expiresAt = domain.expiresAt,
            reviewerNote = domain.reviewerNote
        )
    }
    
    private fun parseStatus(status: String): RequestStatus {
        return when (status.lowercase()) {
            "pending" -> RequestStatus.PENDING
            "approved" -> RequestStatus.APPROVED
            "rejected" -> RequestStatus.REJECTED
            "denied" -> RequestStatus.REJECTED
            "expired" -> RequestStatus.EXPIRED
            "cancelled" -> RequestStatus.CANCELLED
            else -> RequestStatus.PENDING
        }
    }
    
    private fun parseType(type: String): RequestType {
        return try {
            RequestType.valueOf(type.uppercase())
        } catch (e: Exception) {
            RequestType.APP_ACCESS
        }
    }
}
