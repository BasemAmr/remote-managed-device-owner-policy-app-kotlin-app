package com.selfcontrol.data.worker

import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.selfcontrol.domain.model.Result as DomainResult
import com.selfcontrol.domain.repository.UrlRepository
import com.selfcontrol.service.UrlFilterVpnService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * UrlBlacklistSyncWorker - Periodic worker that syncs URL blacklist from server
 * Runs every hour to keep URL blocking patterns up to date
 */
@HiltWorker
class UrlBlacklistSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val urlRepository: UrlRepository
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "url_blacklist_sync_worker"
        const val TAG = "UrlBlacklistSyncWorker"
        
        const val ACTION_URL_LIST_UPDATED = "com.selfcontrol.ACTION_URL_LIST_UPDATED"
    }
    
    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        Timber.i("[$TAG] Starting URL blacklist sync")
        val startTime = System.currentTimeMillis()
        
        return@withContext try {
            // Step 1: Fetch URL blacklist from server
            val result = urlRepository.syncUrlsFromServer("")
            
            if (result is DomainResult.Error) {
                Timber.e("[$TAG] Failed to fetch URLs: ${result.message}")
                return@withContext ListenableWorker.Result.retry()
            }

            val urls = (result as? DomainResult.Success)?.data ?: emptyList()
            
            if (urls.isEmpty()) {
                Timber.d("[$TAG] No URLs fetched from server")
                return@withContext ListenableWorker.Result.success()
            }
            
            Timber.d("[$TAG] Fetched ${urls.size} URL patterns from server")
            
            // Step 2: Save URLs locally
            urlRepository.saveUrls(urls)
            
            // Step 3: Notify VPN service to reload blacklist
            notifyVpnService()
            
            val duration = System.currentTimeMillis() - startTime
            Timber.i("[$TAG] Completed in ${duration}ms. Synced: ${urls.size} patterns")
            
            ListenableWorker.Result.success()
            
        } catch (e: Exception) {
            Timber.e(e, "[$TAG] URL blacklist sync failed")
            
            if (runAttemptCount < 3) {
                Timber.i("[$TAG] Retrying... Attempt ${runAttemptCount + 1}/3")
                ListenableWorker.Result.retry()
            } else {
                ListenableWorker.Result.failure()
            }
        }
    }
    
    /**
     * Notify VPN service to reload the URL blacklist
     */
    private fun notifyVpnService() {
        try {
            val intent = Intent(ACTION_URL_LIST_UPDATED).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
            Timber.d("[$TAG] Notified VPN service of URL list update")
        } catch (e: Exception) {
            Timber.w(e, "[$TAG] Failed to notify VPN service")
        }
    }
}
