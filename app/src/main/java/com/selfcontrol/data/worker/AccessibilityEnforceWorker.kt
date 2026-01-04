package com.selfcontrol.data.worker

import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.selfcontrol.domain.model.Violation
import com.selfcontrol.domain.model.ViolationType
import com.selfcontrol.domain.repository.AccessibilityRepository
import com.selfcontrol.domain.repository.ViolationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Worker that periodically checks if locked accessibility services are disabled
 * and triggers enforcement UI if violations are detected.
 */
@HiltWorker
class AccessibilityEnforceWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val accessibilityRepository: AccessibilityRepository,
    private val violationRepository: ViolationRepository
) : CoroutineWorker(appContext, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            Timber.i("[AccessibilityEnforceWorker] Checking locked services")
            
            val lockedServices = accessibilityRepository.observeLockedServices().first()
            val disabledLockedServices = lockedServices.filter { !it.isEnabled }
            
            if (disabledLockedServices.isNotEmpty()) {
                Timber.w("[AccessibilityEnforceWorker] Found ${disabledLockedServices.size} disabled locked services")
                
                // Log violations
                for (service in disabledLockedServices) {
                    violationRepository.logViolation(
                        Violation(
                            appPackage = service.packageName,
                            type = ViolationType.ACCESSIBILITY_SERVICE_DISABLED,
                            message = "Locked accessibility service disabled: ${service.label}",
                            details = "service_id: ${service.serviceId}"
                        )
                    )
                }
                
                // Trigger enforcement UI
                showEnforcementNotification(disabledLockedServices.map { it.serviceId })
            }
            
            Result.success()
            
        } catch (e: Exception) {
            Timber.e(e, "[AccessibilityEnforceWorker] Enforcement check failed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
    
    private fun showEnforcementNotification(serviceIds: List<String>) {
        try {
            // Create intent to launch EnforcementActivity
            val intent = Intent().apply {
                setClassName(
                    appContext.packageName,
                    "com.selfcontrol.presentation.enforcement.EnforcementActivity"
                )
                putExtra("disabled_services", serviceIds.toTypedArray())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            appContext.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "[AccessibilityEnforceWorker] Failed to launch enforcement activity")
        }
    }
    
    companion object {
        const val WORK_NAME = "accessibility_enforce_worker"
    }
}
