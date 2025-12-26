package com.selfcontrol.domain.usecase.request

import com.selfcontrol.domain.model.Request
import com.selfcontrol.domain.model.RequestStatus
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.repository.RequestRepository
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Use case to create an access request for a blocked app
 */
class CreateAccessRequestUseCase @Inject constructor(
    private val requestRepository: RequestRepository
) {
    /**
     * Create a new access request
     * @param packageName Package name of the blocked app
     * @param appName Display name of the app
     * @param reason User's reason for requesting access
     */
    suspend operator fun invoke(
        packageName: String,
        appName: String,
        reason: String
    ): Result<Request> {
        Timber.i("[CreateRequest] Creating request for $packageName")
        
        // Validate inputs
        if (reason.isBlank()) {
            return Result.Error("Reason cannot be empty")
        }
        
        if (reason.length < 10) {
            return Result.Error("Reason must be at least 10 characters")
        }
        
        // Create request
        val request = Request(
            id = UUID.randomUUID().toString(),
            packageName = packageName,
            appName = appName,
            reason = reason,
            status = RequestStatus.PENDING,
            requestedAt = System.currentTimeMillis()
        )
        
        // Save and sync
        return when (val result = requestRepository.createRequest(request)) {
            is Result.Success -> {
                Timber.i("[CreateRequest] Request created successfully: ${result.data.id}")
                result
            }
            
            is Result.Error -> {
                Timber.e("[CreateRequest] Failed to create request: ${result.message}")
                result
            }
            
            is Result.Loading -> result
        }
    }
    
    /**
     * Validate if user can create a request (cooldown check)
     * @param packageName Package name to check
     * @param cooldownHours Cooldown period in hours
     */
    suspend fun canCreateRequest(
        packageName: String,
        cooldownHours: Int = 24
    ): Result<Boolean> {
        // Get recent requests for this app
        // Check if any pending or recently rejected requests exist
        // This would need to query the repository
        
        Timber.d("[CreateRequest] Checking if request can be created for $packageName")
        
        // TODO: Implement cooldown logic
        // For now, always allow
        return Result.Success(true)
    }
}
