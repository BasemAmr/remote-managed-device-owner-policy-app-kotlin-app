package com.selfcontrol.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.selfcontrol.data.local.dao.AppDao
import com.selfcontrol.data.local.entity.AppEntity
import com.selfcontrol.domain.model.App
import com.selfcontrol.domain.model.Result
import com.selfcontrol.domain.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AppRepository
 * Handles installed app information
 */
@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao
) : AppRepository {
    
    private val packageManager: PackageManager = context.packageManager
    
    override fun observeAllApps(): Flow<List<App>> {
        return appDao.observeAllApps()
            .map { entities -> entities.map { entityToDomain(it) } }
    }
    
    override fun observeUserApps(): Flow<List<App>> {
        return appDao.observeUserApps()
            .map { entities -> entities.map { entityToDomain(it) } }
    }
    
    override fun observeApp(packageName: String): Flow<App?> {
        return appDao.observeApp(packageName)
            .map { entity -> entity?.let { entityToDomain(it) } }
    }
    
    override suspend fun getApp(packageName: String): Result<App?> {
        return try {
            val entity = appDao.getApp(packageName)
            Result.Success(entity?.let { entityToDomain(it) })
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to get app $packageName")
            Result.Error(e.message ?: "Failed to get app")
        }
    }
    
    override suspend fun refreshInstalledApps(): Result<List<App>> {
        return try {
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            
            val appEntities = installedApps.map { appInfo ->
                AppEntity(
                    packageName = appInfo.packageName,
                    name = getAppName(appInfo),
                    iconUrl = null, // Icons are loaded on-demand
                    isSystemApp = isSystemApp(appInfo),
                    version = getAppVersion(appInfo.packageName),
                    installTime = getInstallTime(appInfo.packageName)
                )
            }
            
            // Save to database
            appDao.insertApps(appEntities)
            
            val apps = appEntities.map { entityToDomain(it) }
            
            Timber.i("[AppRepo] Refreshed ${apps.size} installed apps")
            Result.Success(apps)
            
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to refresh installed apps")
            Result.Error(e.message ?: "Failed to refresh apps")
        }
    }
    
    override suspend fun saveApp(app: App): Result<Unit> {
        return try {
            val entity = domainToEntity(app)
            appDao.insertApp(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to save app")
            Result.Error(e.message ?: "Failed to save app")
        }
    }
    
    override suspend fun deleteApp(packageName: String): Result<Unit> {
        return try {
            appDao.deleteByPackageName(packageName)
            Timber.d("[AppRepo] Deleted app $packageName")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to delete app")
            Result.Error(e.message ?: "Failed to delete app")
        }
    }
    
    override suspend fun getAppCount(): Result<Int> {
        return try {
            val count = appDao.getAppCount()
            Result.Success(count)
        } catch (e: Exception) {
            Timber.e(e, "[AppRepo] Failed to get app count")
            Result.Error(e.message ?: "Failed to get count")
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
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun getInstallTime(packageName: String): Long {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
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
            iconUrl = entity.iconUrl,
            isSystemApp = entity.isSystemApp,
            version = entity.version,
            installTime = entity.installTime
        )
    }
    
    private fun domainToEntity(domain: App): AppEntity {
        return AppEntity(
            packageName = domain.packageName,
            name = domain.name,
            iconUrl = domain.iconUrl,
            isSystemApp = domain.isSystemApp,
            version = domain.version,
            installTime = domain.installTime
        )
    }
}
