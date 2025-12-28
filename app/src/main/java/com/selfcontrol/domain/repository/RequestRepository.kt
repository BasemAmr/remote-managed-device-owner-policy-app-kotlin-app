package com.selfcontrol.domain.repository

import com.selfcontrol.domain.model.Request
import com.selfcontrol.domain.model.RequestStatus
import com.selfcontrol.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing access requests
 */
interface RequestRepository {
    fun observeAllRequests(): Flow<List<Request>>
    fun observeRequestsByStatus(status: RequestStatus): Flow<List<Request>>
    fun observePendingRequests(): Flow<List<Request>>
    fun observeRequestsForApp(packageName: String): Flow<List<Request>>
    fun observeRequest(requestId: String): Flow<Request?>
    
    suspend fun getRequest(requestId: String): Result<Request?>
    suspend fun createRequest(request: Request)
    suspend fun updateRequestStatus(requestId: String, status: RequestStatus, reviewerNote: String? = null): Result<Unit>
    suspend fun fetchLatestRequests(): Result<List<Request>>
    suspend fun checkRequestStatus(requestId: String): Result<Request>
    suspend fun getPendingCount(): Result<Int>
    suspend fun deleteRequest(requestId: String): Result<Unit>
}
