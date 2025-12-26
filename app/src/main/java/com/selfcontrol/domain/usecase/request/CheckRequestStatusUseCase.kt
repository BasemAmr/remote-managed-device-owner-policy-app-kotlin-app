package com.selfcontrol.domain.usecase.request

import com.selfcontrol.domain.model.Request
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.repository.RequestRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to check the status of an access request
 */
class CheckRequestStatusUseCase @Inject constructor(
    private val requestRepository: RequestRepository
) {
    /**
     * Check request status from server
     * @param requestId ID of the request to check
     */
    suspend operator fun invoke(requestId: String): Result<Request> {
        Timber.d("[CheckRequestStatus] Checking status for request: $requestId")
        
        return when (val result = requestRepository.checkRequestStatus(requestId)) {
            is Result.Success -> {
                Timber.i("[CheckRequestStatus] Status: ${result.data.status}")
                result
            }
            
            is Result.Error -> {
                Timber.e("[CheckRequestStatus] Error: ${result.message}")
                result
            }
            
            is Result.Loading -> result
        }
    }
    
    /**
     * Sync all pending requests from server
     */
    suspend fun syncAllRequests(): Result<List<Request>> {
        Timber.i("[CheckRequestStatus] Syncing all requests from server")
        
        return when (val result = requestRepository.fetchLatestRequests()) {
            is Result.Success -> {
                Timber.i("[CheckRequestStatus] Synced ${result.data.size} requests")
                result
            }
            
            is Result.Error -> {
                Timber.e("[CheckRequestStatus] Sync failed: ${result.message}")
                result
            }
            
            is Result.Loading -> result
        }
    }
}
