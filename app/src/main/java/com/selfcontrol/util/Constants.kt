package com.selfcontrol.util

/**
 * Application-wide constants
 */
object Constants {
    
    // API Configuration
    const val API_TIMEOUT_SECONDS = 30L
    const val API_RETRY_COUNT = 3
    
    // WorkManager Tags
    const val WORK_TAG_POLICY_SYNC = "policy_sync"
    const val WORK_TAG_REQUEST_CHECK = "request_check"
    const val WORK_TAG_VIOLATION_UPLOAD = "violation_upload"
    const val WORK_TAG_HEARTBEAT = "heartbeat"
    const val WORK_TAG_URL_SYNC = "url_sync"
    const val WORK_TAG_APP_SYNC = "app_sync"
    const val WORK_TAG_ACCESSIBILITY_ENFORCE = "accessibility_enforce"
    
    // WorkManager Intervals (in minutes)
    const val POLICY_SYNC_INTERVAL = 15L
    const val REQUEST_CHECK_INTERVAL = 15L
    const val VIOLATION_UPLOAD_INTERVAL = 60L
    const val HEARTBEAT_INTERVAL = 10L
    const val URL_SYNC_INTERVAL = 60L
    const val APP_SYNC_INTERVAL = 60L
    const val ACCESSIBILITY_ENFORCE_INTERVAL = 360L // 6 hours
    
    // Database
    const val DATABASE_NAME = "selfcontrol.db"
    const val DATABASE_VERSION = 4
    
    // DataStore
    const val DATASTORE_NAME = "selfcontrol_preferences"
    
    // Preferences Keys
    const val PREF_DEVICE_ID = "device_id"
    const val PREF_AUTH_TOKEN = "auth_token"
    const val PREF_LAST_POLICY_SYNC = "last_policy_sync"
    const val PREF_IS_DEVICE_OWNER = "is_device_owner"
    const val PREF_COOLDOWN_HOURS = "cooldown_hours"
    
    // Default Values
    const val DEFAULT_COOLDOWN_HOURS = 24
    
    // Notification Channels
    const val NOTIFICATION_CHANNEL_MONITORING = "monitoring_channel"
    const val NOTIFICATION_CHANNEL_VIOLATIONS = "violations_channel"
    const val NOTIFICATION_CHANNEL_REQUESTS = "requests_channel"
    
    // Notification IDs
    const val NOTIFICATION_ID_MONITORING = 1001
    const val NOTIFICATION_ID_VIOLATION = 1002
    const val NOTIFICATION_ID_REQUEST = 1003
    
    // Intent Actions
    const val ACTION_BLOCK_APP = "com.selfcontrol.ACTION_BLOCK_APP"
    const val ACTION_UNBLOCK_APP = "com.selfcontrol.ACTION_UNBLOCK_APP"
    const val ACTION_CHECK_POLICY = "com.selfcontrol.ACTION_CHECK_POLICY"
    
    // Intent Extras
    const val EXTRA_PACKAGE_NAME = "package_name"
    const val EXTRA_APP_NAME = "app_name"
    const val EXTRA_VIOLATION_ID = "violation_id"
    const val EXTRA_REQUEST_ID = "request_id"
    
    // Request Status
    const val REQUEST_STATUS_PENDING = "pending"
    const val REQUEST_STATUS_APPROVED = "approved"
    const val REQUEST_STATUS_DENIED = "denied"
    const val REQUEST_STATUS_EXPIRED = "expired"
    
    // Violation Types
    const val VIOLATION_TYPE_APP_LAUNCH = "app_launch"
    const val VIOLATION_TYPE_URL_ACCESS = "url_access"
    const val VIOLATION_TYPE_POLICY_BYPASS = "policy_bypass"
    
    // Device Owner
    const val DEVICE_OWNER_PACKAGE = "com.selfcontrol"
    
    // VPN Configuration
    const val VPN_ADDRESS = "10.0.0.2"
    const val VPN_PREFIX_LENGTH = 24
    const val VPN_DNS_SERVER = "8.8.8.8"
    const val VPN_SESSION_NAME = "SelfControl VPN Filter"
    
    // Logging Tags
    const val TAG_APP = "SelfControl"
    const val TAG_POLICY = "Policy"
    const val TAG_WORKER = "Worker"
    const val TAG_DEVICE_OWNER = "DeviceOwner"
    const val TAG_NETWORK = "Network"
}
