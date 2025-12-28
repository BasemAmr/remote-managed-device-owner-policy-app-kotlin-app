# Device Owner Implementation - Granular Control Fix

## Summary of Changes

This document outlines all the changes made to implement granular app control with proper registration, device owner enforcement, and separate lock/block capabilities.

## 1. DeviceOwnerManager.kt - UPDATED ✅

**Location**: `app/src/main/java/com/selfcontrol/deviceowner/DeviceOwnerManager.kt`

### Key Changes:
- **Removed Global Restriction**: Cleared `DISALLOW_UNINSTALL_APPS` to allow normal app uninstallation
- **Added Granular Controls**:
  - `setAppUninstallBlocked(packageName, blocked)` - Prevents specific app uninstallation
  - `setAppHidden(packageName, hidden)` - Hides/shows specific apps
- **Self-Protection**: Locks the SelfControl app itself on initialization
- **Accessibility Enforcement**: Forces accessibility service to stay enabled
- **UI Status Update**: Updates `AppPreferences` with device owner status

### Behavior:
- Apps with `isLocked = true`: **Openable but NOT uninstallable**
- Apps with `isBlocked = true`: **Hidden and NOT openable**
- Other apps: **Fully normal** (can install/uninstall/use)

---

## 2. AppBlockManager.kt - UPDATED ✅

**Location**: `app/src/main/java/com/selfcontrol/deviceowner/AppBlockManager.kt`

### Key Changes:
- **Granular Policy Enforcement**:
  - Separately handles `isBlocked` (usage blocking via hiding)
  - Separately handles `isLocked` (uninstall prevention)
- **Expiration Handling**: Expired policies auto-unblock usage but can keep lock
- **Self-Protection**: Calls `deviceOwnerManager.initialize()` during `enforceAllPolicies()`

### Policy Logic:
```kotlin
// Usage Blocking (Hiding)
val shouldHide = policy.isBlocked && !policy.isExpired()
deviceOwnerManager.setAppHidden(packageName, shouldHide)

// Uninstall Blocking (Locking)
deviceOwnerManager.setAppUninstallBlocked(packageName, policy.isLocked)
```

---

## 3. DeviceSetupRepository.kt - CREATED ✅

**Location**: `app/src/main/java/com/selfcontrol/data/repository/DeviceSetupRepository.kt`

### Purpose:
Handles first-run initialization and backend registration

### Functions:
1. **performStartupChecks()**: Main startup routine
   - Scans installed apps via `appRepository.refreshInstalledApps()`
   - Checks if device is registered
   - Registers if needed, otherwise syncs policies

2. **registerDevice()**: Backend registration
   - Gets Android ID from device
   - Calls `/api/device/register` with `device_name` and `android_id`
   - Saves `device_id` and `device_token` to preferences

---

## 4. SelfControlApp.kt - UPDATED ✅

**Location**: `app/src/main/java/com/selfcontrol/SelfControlApp.kt`

### Startup Sequence:
```kotlin
override fun onCreate() {
    // 1. Initialize Device Owner (Locks policies & Updates UI)
    deviceOwnerManager.initialize()
    
    // 2. Start monitoring package changes
    packageMonitor.startMonitoring()
    
    // 3. Register & Scan Apps (Background)
    appScope.launch {
        deviceSetupRepository.performStartupChecks()
    }
    
    // 4. Schedule all background workers
    scheduleBackgroundWorkers()
}
```

---

## 5. SelfControlApi.kt - UPDATED ✅

**Location**: `app/src/main/java/com/selfcontrol/data/remote/api/SelfControlApi.kt`

### Change:
```kotlin
@POST("api/management/device/register")
suspend fun registerDevice(
    @Body body: Map<String, String>  // Changed from DeviceDto
): ResponseWrapper<DeviceDto>
```

**Reason**: Backend expects `{ "device_name": "...", "android_id": "..." }`

---

## 6. DeviceDto.kt - UPDATED ✅

**Location**: `app/src/main/java/com/selfcontrol/data/remote/dto/DeviceDto.kt`

### Changes:
- Made all fields optional with default values
- Added `authToken` field:
```kotlin
@SerializedName("device_token")
val authToken: String? = null
```

**Reason**: Backend returns `device_token` on registration

---

## 7. AppPolicy.kt - Already Correct ✅

**Location**: `app/src/main/java/com/selfcontrol/domain/model/AppPolicy.kt`

Contains both:
- `isBlocked: Boolean` - For usage blocking
- `isLocked: Boolean` - For uninstall prevention

---

## 8. PolicyMapper.kt - Already Correct ✅

**Location**: `app/src/main/java/com/selfcontrol/data/remote/mapper/PolicyMapper.kt`

Maps:
- `dto.isBlocked` → `domain.isBlocked`
- `dto.isLocked` → `domain.isLocked`

---

## Backend Requirements

### Registration Endpoint
The backend should respond to `POST /api/device/register` with:

```json
{
  "success": true,
  "data": {
    "device_id": "123",
    "device_token": "abc-xyz-token",
    "policy_version": 1
  }
}
```

### Policy Endpoint
The backend should send policies with both flags:

```json
{
  "package_name": "com.example.app",
  "is_blocked": false,      // Hide/show app
  "is_uninstallable": true  // Lock/unlock uninstall
}
```

**Note**: The backend field `is_uninstallable` maps to `isLocked` in the app.

---

## Testing Checklist

### 1. Device Owner Setup
```bash
# Set device owner via ADB
adb shell dpm set-device-owner com.selfcontrol/.deviceowner.DeviceOwnerReceiver
```

### 2. Expected Behavior After Setup
- [ ] Dashboard shows "Device Active" as Green
- [ ] Logcat shows `[DeviceOwner] Initializing granular policies`
- [ ] Logcat shows `[Startup] Registering with Backend...`
- [ ] Logcat shows `[Startup] ✅ Registration Successful!`
- [ ] Backend database has new device entry
- [ ] "Total Apps" count updates from 0 to actual count

### 3. App Control Testing
- [ ] **Lock Test**: Toggle "Lock" on an app → App opens normally, but uninstall fails
- [ ] **Block Test**: Toggle "Block" on an app → App disappears from launcher
- [ ] **Normal Apps**: Other apps can be installed/uninstalled freely
- [ ] **Self-Protection**: SelfControl app cannot be uninstalled

### 4. Accessibility Service
- [ ] Accessibility service is enabled automatically
- [ ] User cannot disable it from Settings

---

## Potential Issues & Solutions

### Issue 1: Backend URL
**Symptom**: Registration fails with connection error

**Solution**: Update `build.gradle.kts`:
```kotlin
// For Emulator
buildConfigField("String", "API_URL", "\"http://10.0.2.2:3000\"")

// For Real Device (use your computer's local IP)
buildConfigField("String", "API_URL", "\"http://192.168.1.X:3000\"")

// For Render deployment
buildConfigField("String", "API_URL", "\"https://your-app.onrender.com\"")
```

### Issue 2: Backend Response Mismatch
**Symptom**: Registration succeeds but device_id is not saved

**Check**: Backend controller returns correct JSON structure with `device_id` and `device_token`

### Issue 3: Policies Not Enforcing
**Symptom**: Toggling lock/block doesn't work

**Solution**: Ensure:
1. Device is registered (check preferences)
2. Policies are synced from server
3. `AppBlockManager.enforcePolicy()` is called
4. Device Owner is active

---

## Next Steps

1. **Build and Install**:
   ```bash
   ./gradlew installDebug
   ```

2. **Set Device Owner**:
   ```bash
   adb shell dpm set-device-owner com.selfcontrol/.deviceowner.DeviceOwnerReceiver
   ```

3. **Monitor Logs**:
   ```bash
   adb logcat -s SelfControl
   ```

4. **Verify Backend**:
   - Check database for new device entry
   - Verify API endpoints are accessible
   - Test policy creation from dashboard

---

## Architecture Flow

```
App Startup
    ↓
DeviceOwnerManager.initialize()
    ├─ Update UI Preferences (isDeviceOwner = true)
    ├─ Clear global uninstall restriction
    ├─ Lock SelfControl app
    └─ Force accessibility service
    ↓
DeviceSetupRepository.performStartupChecks()
    ├─ Scan installed apps
    ├─ Check if registered
    │   ├─ No → registerDevice()
    │   │       ├─ Get Android ID
    │   │       ├─ POST /api/device/register
    │   │       └─ Save device_id & token
    │   └─ Yes → syncPoliciesFromServer()
    ↓
PackageMonitor.startMonitoring()
    ↓
Background Workers Scheduled
```

---

## Files Modified/Created

### Modified:
1. `DeviceOwnerManager.kt`
2. `AppBlockManager.kt`
3. `SelfControlApp.kt`
4. `SelfControlApi.kt`
5. `DeviceDto.kt`

### Created:
1. `DeviceSetupRepository.kt`

### Already Correct:
1. `AppPolicy.kt`
2. `PolicyMapper.kt`
3. `AppPreferences.kt`

---

## Summary

The implementation now provides:
- ✅ **Granular Control**: Separate lock (prevent uninstall) and block (prevent usage)
- ✅ **Self-Protection**: App locks itself on startup
- ✅ **Registration**: Automatic backend registration with Android ID
- ✅ **App Scanning**: Populates app list on first run
- ✅ **UI Updates**: Dashboard shows correct device owner status
- ✅ **Accessibility**: Forces accessibility service to stay enabled

The missing "glue logic" has been implemented. The app will now:
1. Register with backend on first run
2. Scan and display all installed apps
3. Enforce policies with granular control
4. Protect itself from uninstallation
5. Update UI to show active status
