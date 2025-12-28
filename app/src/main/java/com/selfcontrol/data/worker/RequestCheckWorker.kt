package com.selfcontrol.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.selfcontrol.deviceowner.AppBlockManager
import com.selfcontrol.domain.model.RequestStatus
import com.selfcontrol.domain.model.Result as DomainResult
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

    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        Timber.i("[$TAG] Starting request status check")
        val startTime = System.currentTimeMillis()

        try {
            // Step 1: Get all pending requests
            val pendingRequests = requestRepository.observeRequestsByStatus(RequestStatus.PENDING).first()

            if (pendingRequests.isEmpty()) {
                Timber.d("[$TAG] No pending requests to check")
                return@withContext ListenableWorker.Result.success()
            }

            Timber.d("[$TAG] Checking ${pendingRequests.size} pending requests")

            var approved = 0
            var denied = 0
            var stillPending = 0

            // Step 2: Check each request's status from server
            pendingRequests.forEach { request ->
                try {
                    val result = requestRepository.checkRequestStatus(request.id)

                    when (result) {
                        is DomainResult.Success -> {
                            val serverRequest = result.data
                            when (serverRequest.status) {
                                RequestStatus.APPROVED -> {
                                    Timber.i("[$TAG] Request ${request.id} approved - unblocking ${request.packageName}")
                                    handleApprovedRequest(request.packageName, serverRequest.reviewedAt ?: System.currentTimeMillis())
                                    approved++
                                }
                                RequestStatus.REJECTED, RequestStatus.DENIED -> {
                                    Timber.i("[$TAG] Request ${request.id} rejected/denied")
                                    denied++
                                }
                                RequestStatus.PENDING -> {
                                    Timber.d("[$TAG] Request ${request.id} still pending")
                                    stillPending++
                                }
                                RequestStatus.EXPIRED, RequestStatus.CANCELLED -> {
                                    Timber.d("[$TAG] Request ${request.id} expired or cancelled")
                                }
                            }
                        }
                        is DomainResult.Error -> {
                            Timber.e(result.exception, "[$TAG] Failed to check request ${request.id}")
                        }
                        is DomainResult.Loading -> {
                            Timber.d("[$TAG] Loading request status for ${request.id}")
                            stillPending++
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "[$TAG] Exception while checking request ${request.id}")
                }
            }

            val duration = System.currentTimeMillis() - startTime
            Timber.i("[$TAG] Completed in ${duration}ms. Approved: $approved, Denied: $denied, Pending: $stillPending")

            return@withContext ListenableWorker.Result.success()

        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Request check failed")
            return@withContext when {
                runAttemptCount < 3 -> {
                    Timber.i("[$TAG] Retrying... Attempt ${runAttemptCount + 1}/3")
                    ListenableWorker.Result.retry()
                }
                else -> ListenableWorker.Result.failure()
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
