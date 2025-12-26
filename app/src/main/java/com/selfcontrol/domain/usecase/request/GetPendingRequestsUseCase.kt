package com.selfcontrol.domain.usecase.request

import com.selfcontrol.domain.model.Request
import com.selfcontrol.domain.repository.RequestRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to get pending access requests
 */
class GetPendingRequestsUseCase @Inject constructor(
    private val requestRepository: RequestRepository
) {
    /**
     * Get all pending requests as Flow (reactive)
     */
    operator fun invoke(): Flow<List<Request>> {
        Timber.d("[GetPendingRequests] Observing pending requests")
        return requestRepository.observePendingRequests()
    }
    
    /**
     * Get all requests (any status)
     */
    fun getAllRequests(): Flow<List<Request>> {
        Timber.d("[GetPendingRequests] Observing all requests")
        return requestRepository.observeAllRequests()
    }
    
    /**
     * Get requests for a specific app
     */
    fun getRequestsForApp(packageName: String): Flow<List<Request>> {
        Timber.d("[GetPendingRequests] Observing requests for $packageName")
        return requestRepository.observeRequestsForApp(packageName)
    }
}
