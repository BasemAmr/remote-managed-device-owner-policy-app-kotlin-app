package com.selfcontrol.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.selfcontrol.deviceowner.AppBlockManager
import com.selfcontrol.domain.model.RequestStatus
import com.selfcontrol.domain.repository.PolicyRepository
import com.selfcontrol.domain.repository.RequestRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * RequestCheckWorker - Periodic worker that checks pending access request statuses
 * Runs every 15 minutes to check if any pending requests have been approved/denied
 */
@HiltWorker
class RequestCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val requestRepository: RequestRepository,
    private val policyRepository: PolicyRepository,
    private val appBlockManager: AppBlockManager
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "request_check_worker"
        const val TAG = "RequestCheckWorker"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Timber.i("[$TAG] Starting request status check")
        val startTime = System.currentTimeMillis()
        
        return@withContext try {
            // Step 1: Get all pending requests
            val pendingRequests = requestRepository.observeRequestsByStatus(RequestStatus.PENDING).first()
            
            if (pendingRequests.isEmpty()) {
                Timber.d("[$TAG] No pending requests to check")
                return@withContext Result.success()
            }
            
            Timber.d("[$TAG] Checking ${pendingRequests.size} pending requests")
            
            var approved = 0
            var denied = 0
            var stillPending = 0
            
            // Step 2: Check each request's status from server
            pendingRequests.forEach { request ->
                try {
                    val serverRequest = requestRepository.getRequestById(request.id)
                    
                    if (serverRequest != null) {
                        when (serverRequest.status) {
                            RequestStatus.APPROVED -> {
                                Timber.i("[$TAG] Request ${request.id} approved - unblocking ${request.packageName}")
                                
                                // Unblock the app
                                handleApprovedRequest(request.packageName, serverRequest.expiresAt)
                                
                                // Update local request status
                                requestRepository.updateRequest(serverRequest)
                                approved++
                            }
                            RequestStatus.DENIED -> {
                                Timber.i("[$TAG] Request ${request.id} denied")
                                
                                // Update local request status
                                requestRepository.updateRequest(serverRequest)
                                denied++
                            }
                            RequestStatus.EXPIRED -> {
                                Timber.d("[$TAG] Request ${request.id} expired")
                                
                                // Ensure app is blocked
                                appBlockManager.blockApp(request.packageName)
                                
                                // Update local request status
                                requestRepository.updateRequest(serverRequest)
                            }
                            else -> {
                                stillPending++
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "[$TAG] Failed to check request ${request.id}")
                }
            }
            
            // Step 3: Sync unsynced requests to server
            val unsyncedRequests = requestRepository.getUnsyncedRequests()
            if (unsyncedRequests.isNotEmpty()) {
                Timber.d("[$TAG] Syncing ${unsyncedRequests.size} unsynced requests")
                requestRepository.syncRequestsToServer()
            }
            
            val duration = System.currentTimeMillis() - startTime
            Timber.i("[$TAG] Completed in ${duration}ms. Approved: $approved, Denied: $denied, Pending: $stillPending")
            
            Result.success()
            
        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Request check failed")
            
            if (runAttemptCount < 3) {
                Timber.i("[$TAG] Retrying... Attempt ${runAttemptCount + 1}/3")
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    /**
     * Handle approved access request - temporarily unblock app
     */
    private suspend fun handleApprovedRequest(packageName: String, expiresAt: Long?) {
        try {
            // Create policy to unblock app with optional expiration
            val policy = com.selfcontrol.domain.model.AppPolicy(
                packageName = packageName,
                isBlocked = false,
                expiresAt = expiresAt
            )
            
            appBlockManager.enforcePolicy(policy)
            policyRepository.savePolicy(policy)
            
            Timber.i("[$TAG] Unblocked app $packageName (expires: $expiresAt)")
            
        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Failed to handle approved request for $packageName")
        }
    }
}
