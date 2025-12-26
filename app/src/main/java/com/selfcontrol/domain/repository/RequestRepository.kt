package com.selfcontrol.domain.repository

import com.selfcontrol.domain.model.Request
import com.selfcontrol.domain.model.RequestStatus
import kotlinx.coroutines.flow.Flow

interface RequestRepository {
    fun observeAllRequests(): Flow<List<Request>>
    fun observeRequestsByStatus(status: RequestStatus): Flow<List<Request>>
    fun observePendingRequestsForApp(packageName: String): Flow<List<Request>>
    suspend fun getRequestById(id: String): Request?
    suspend fun createRequest(request: Request)
    suspend fun updateRequest(request: Request)
    suspend fun getUnsyncedRequests(): List<Request>
    suspend fun syncRequestsToServer()
}

