package com.selfcontrol.deviceowner

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.selfcontrol.domain.model.Permission
import com.selfcontrol.domain.model.PermissionSeverity
import timber.log.Timber

object AccessibilityHelpers {
    
    /**
     * Scan all installed apps and find their accessibility services
     */
    fun scanAccessibilityServices(context: Context): List<com.selfcontrol.domain.model.AccessibilityService> {
        val services = mutableListOf<com.selfcontrol.domain.model.AccessibilityService>()
        val packageManager = context.packageManager
        
        try {
            // Query all accessibility services
            val intent = Intent(AccessibilityService.SERVICE_INTERFACE)
            val resolveInfos = packageManager.queryIntentServices(
                intent,
                PackageManager.GET_META_DATA or PackageManager.GET_SERVICES
            )
            
            for (resolveInfo in resolveInfos) {
                val serviceInfo = resolveInfo.serviceInfo ?: continue
                
                val componentName = ComponentName(
                    serviceInfo.packageName,
                    serviceInfo.name
                )
                
                val label = serviceInfo.loadLabel(packageManager).toString()
                val isEnabled = isAccessibilityServiceEnabled(context, componentName)
                
                services.add(
                    com.selfcontrol.domain.model.AccessibilityService(
                        serviceId = componentName.flattenToString(),
                        packageName = serviceInfo.packageName,
                        serviceName = serviceInfo.name,
                        label = label,
                        isEnabled = isEnabled,
                        isLocked = false // Will be updated from backend
                    )
                )
            }
            
            Timber.i("[AccessibilityHelpers] Found ${services.size} accessibility services")
            
        } catch (e: Exception) {
            Timber.e(e, "[AccessibilityHelpers] Failed to scan accessibility services")
        }
        
        return services
    }
    
    /**
     * Check if an accessibility service is currently enabled
     */
    fun isAccessibilityServiceEnabled(context: Context, componentName: ComponentName): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            
            val serviceName = componentName.flattenToString()
            enabledServices?.contains(serviceName) == true
            
        } catch (e: Exception) {
            Timber.e(e, "[AccessibilityHelpers] Failed to check service status")
            false
        }
    }
    
    /**
     * Get list of currently enabled accessibility services
     */
    fun getEnabledAccessibilityServices(context: Context): List<String> {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return emptyList()
            
            enabledServices.split(":").filter { it.isNotBlank() }
            
        } catch (e: Exception) {
            Timber.e(e, "[AccessibilityHelpers] Failed to get enabled services")
            emptyList()
        }
    }
    
    /**
     * Check if our own AccessibilityMonitor service is enabled
     */
    fun isOurAccessibilityServiceEnabled(context: Context): Boolean {
        val componentName = ComponentName(
            context.packageName,
            "com.selfcontrol.deviceowner.AccessibilityMonitor"
        )
        return isAccessibilityServiceEnabled(context, componentName)
    }
    

    /**
     * Open accessibility settings for a specific service
     */
    fun openAccessibilitySettings(context: Context, componentName: ComponentName? = null) {
        try {
            val intent = if (componentName != null) {
                // Try to open specific service settings
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(":settings:fragment_args_key", componentName.flattenToString())
                }
            } else {
                // Open general accessibility settings
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            
            context.startActivity(intent)
            
        } catch (e: Exception) {
            Timber.e(e, "[AccessibilityHelpers] Failed to open accessibility settings")
        }
    }
    
    /**
     * Scan device permissions and their grant status
     */
    fun scanPermissions(context: Context): List<Permission> {
        val permissions = listOf(
            "android.permission.QUERY_ALL_PACKAGES" to PermissionSeverity.CRITICAL,
            "android.permission.BIND_ACCESSIBILITY_SERVICE" to PermissionSeverity.CRITICAL,
            "android.permission.BIND_DEVICE_ADMIN" to PermissionSeverity.CRITICAL,
            "android.permission.FOREGROUND_SERVICE" to PermissionSeverity.HIGH,
            "android.permission.BIND_VPN_SERVICE" to PermissionSeverity.CRITICAL
        )
        
        return permissions.map { (permName, severity) ->
            val isGranted = checkPermission(context, permName)
            Permission(permName, isGranted, severity)
        }
    }
    
    private fun checkPermission(context: Context, permission: String): Boolean {
        return try {
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }
}
