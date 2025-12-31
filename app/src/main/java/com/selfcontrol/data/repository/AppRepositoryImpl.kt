package com.selfcontrol.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.selfcontrol.data.local.dao.AppDao
import com.selfcontrol.data.local.entity.AppEntity
import com.selfcontrol.data.local.entity.AppWithPolicy
import com.selfcontrol.domain.model.App
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.model.SyncStatus
import com.selfcontrol.domain.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AppRepository
 * Handles installed app information with offline-first sync queue
 */
@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao
) : AppRepository {
    
    private val packageManager: PackageManager = context.packageManager
    
    private val MATCH_FLAGS = PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES
    
    override fun observeAllApps(): Flow<List<App>> {
        return appDao.observeAppsWithPolicy()
            .map { entities -> entities.map { mapWithPolicyToDomain(it) } }
    }
    
    override fun observeUserApps(): Flow<List<App>> {
        return appDao.observeUserAppsWithPolicy()
            .map { entities -> entities.map { mapWithPolicyToDomain(it) } }
    }
    
    override fun observeApp(packageName: String): Flow<App?> {
        return appDao.observeAppWithPolicy(packageName)
            .map { entity -> entity?.let { mapWithPolicyToDomain(it) } }
    }
    
    override fun observePendingSyncCount(): Flow<Int> {
        return appDao.observePendingSyncCount()
    }
    
    override suspend fun getApp(packageName: String): Result<App?> {
        return try {
            val entity = appDao.getApp(packageName)
            Result.Success(entity?.let { entityToDomain(it) })
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to get app $packageName")
            Result.Error(e)
        }
    }
    
    override suspend fun getAppByPackageName(packageName: String): Result<App?> {
        return getApp(packageName)
    }
    
    override suspend fun refreshInstalledApps(): Result<List<App>> = withContext(Dispatchers.IO) {
        try {
            val installedApps = packageManager.getInstalledApplications(MATCH_FLAGS)
            
            val appEntities = installedApps.map { appInfo ->
                // Check if app already exists to preserve sync status
                val existingApp = appDao.getApp(appInfo.packageName)
                
                AppEntity(
                    packageName = appInfo.packageName,
                    name = getAppName(appInfo),
                    iconUrl = null, // Icons are loaded on-demand
                    isSystemApp = isSystemApp(appInfo),
                    version = getAppVersion(appInfo.packageName),
                    installTime = getInstallTime(appInfo.packageName),
                    lastUpdated = System.currentTimeMillis(),
                    syncStatus = existingApp?.syncStatus ?: SyncStatus.PENDING.name,
                    syncRetryCount = existingApp?.syncRetryCount ?: 0,
                    lastSyncAttempt = existingApp?.lastSyncAttempt ?: 0L,
                    needsImmediateSync = existingApp?.needsImmediateSync ?: true
                )
            }
            
            // Save to database
            appDao.insertApps(appEntities)
            
            val apps = appEntities.map { entityToDomain(it) }
            
            Timber.i("[AppRepo] Refreshed ${apps.size} installed apps")
            Result.Success(apps)
            
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to refresh installed apps")
            Result.Error(e)
        }
    }
    
    override suspend fun saveApp(app: App): Result<Unit> {
        return try {
            val entity = domainToEntity(app)
            appDao.insertApp(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to save app")
            Result.Error(e)
        }
    }
    
    override suspend fun deleteApp(packageName: String): Result<Unit> {
        return try {
            appDao.deleteByPackageName(packageName)
            Timber.d("[AppRepo] Deleted app $packageName")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to delete app")
            Result.Error(e)
        }
    }
    
    override suspend fun getAppCount(): Result<Int> {
        return try {
            val count = appDao.getAppCount()
            Result.Success(count)
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to get app count")
            Result.Error(e)
        }
    }
    
    override suspend fun getInstalledAppsForUpload(): Result<List<App>> = withContext(Dispatchers.IO) {
        try {
            val installedApps = packageManager.getInstalledApplications(MATCH_FLAGS)
            
            val apps = installedApps.map { appInfo ->
                App(
                    packageName = appInfo.packageName,
                    name = getAppName(appInfo),
                    version = getAppVersion(appInfo.packageName),
                    installTime = getInstallTime(appInfo.packageName),
                    isSystemApp = isSystemApp(appInfo)
                )
            }
            
            Timber.d("[AppRepo] Retrieved ${apps.size} installed apps for upload")
            Result.Success(apps)
            
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to get installed apps for upload")
            Result.Error(e)
        }
    }
    
    // ==================== Sync Queue Operations ====================
    
    override suspend fun getPendingSyncCount(): Result<Int> {
        return try {
            val count = appDao.getPendingSyncCount()
            Result.Success(count)
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to get pending sync count")
            Result.Error(e)
        }
    }
    
    override suspend fun getPendingSyncApps(): Result<List<App>> {
        return try {
            val entities = appDao.getPendingSyncApps()
            val apps = entities.map { entityToDomain(it) }
            Timber.d("[AppRepo] Retrieved ${apps.size} pending sync apps")
            Result.Success(apps)
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to get pending sync apps")
            Result.Error(e)
        }
    }
    
    override suspend fun updateAppSyncStatus(
        packageName: String, 
        status: SyncStatus, 
        retryCount: Int
    ): Result<Unit> {
        return try {
            appDao.updateSyncStatus(
                packageName = packageName,
                status = status.name,
                retryCount = retryCount,
                lastAttempt = System.currentTimeMillis(),
                needsImmediate = false
            )
            Timber.d("[AppRepo] Updated sync status for $packageName to $status (retry: $retryCount)")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to update sync status for $packageName")
            Result.Error(e)
        }
    }
    
    override suspend fun markAppForImmediateSync(packageName: String): Result<Unit> {
        return try {
            appDao.markForImmediateSync(packageName)
            Timber.d("[AppRepo] Marked $packageName for immediate sync")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to mark $packageName for immediate sync")
            Result.Error(e)
        }
    }
    
    override suspend fun markAllAppsSynced(): Result<Unit> {
        return try {
            appDao.markAllAsSynced()
            Timber.i("[AppRepo] Marked all apps as synced")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to mark all apps as synced")
            Result.Error(e)
        }
    }
    
    override suspend fun resetFailedSyncApps(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            appDao.resetFailedSyncApps()
            Timber.i("[AppRepo] Reset failed sync apps for retry")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to reset failed sync apps")
            Result.Error(e)
        }
    }
    
    // ==================== Helper Methods ====================
    
    private fun getAppName(appInfo: ApplicationInfo): String {
        return try {
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            appInfo.packageName
        }
    }
    
    private fun getAppVersion(packageName: String): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, MATCH_FLAGS)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun getInstallTime(packageName: String): Long {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, MATCH_FLAGS)
            packageInfo.firstInstallTime
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }
    
    // ==================== Mappers ====================
    
    private fun entityToDomain(entity: AppEntity): App {
        return App(
            packageName = entity.packageName,
            name = entity.name,
            iconUrl = entity.iconUrl.orEmpty(),
            isSystemApp = entity.isSystemApp,
            version = entity.version,
            installTime = entity.installTime,
            syncStatus = try { SyncStatus.valueOf(entity.syncStatus) } catch (e: Exception) { SyncStatus.SYNCED },
            syncRetryCount = entity.syncRetryCount,
            isBlocked = false,
            isLocked = false
        )
    }
    
    private fun mapWithPolicyToDomain(input: AppWithPolicy): App {
        return entityToDomain(input.app).copy(
            isBlocked = input.policy?.isBlocked ?: false,
            isLocked = input.policy?.isLocked ?: false
        )
    }
    
    private fun domainToEntity(domain: App): AppEntity {
        return AppEntity(
            packageName = domain.packageName,
            name = domain.name,
            iconUrl = domain.iconUrl,
            isSystemApp = domain.isSystemApp,
            version = domain.version,
            installTime = domain.installTime,
            lastUpdated = System.currentTimeMillis(),
            syncStatus = domain.syncStatus.name,
            syncRetryCount = domain.syncRetryCount,
            lastSyncAttempt = 0L,
            needsImmediateSync = false
        )
    }
}

