# Quick Start Guide - Device Owner Granular Control

## What Was Fixed

You now have **complete granular control** over apps with three distinct behaviors:

### 1. **Lock Only** (`isLocked = true`, `isBlocked = false`)
- ✅ App is **visible** in launcher
- ✅ User can **open and use** the app
- ❌ User **CANNOT uninstall** the app
- **Use Case**: Apps you want available but permanent (e.g., educational apps, work apps)

### 2. **Block Only** (`isLocked = false`, `isBlocked = true`)
- ❌ App is **hidden** from launcher
- ❌ User **CANNOT open** the app
- ✅ User **CAN uninstall** (if they find it in settings)
- **Use Case**: Temporary restrictions (e.g., social media during study time)

### 3. **Lock + Block** (`isLocked = true`, `isBlocked = true`)
- ❌ App is **hidden** from launcher
- ❌ User **CANNOT open** the app
- ❌ User **CANNOT uninstall** the app
- **Use Case**: Maximum restriction (e.g., gambling apps, adult content)

### 4. **Normal** (`isLocked = false`, `isBlocked = false`)
- ✅ Fully functional
- ✅ Can install/uninstall freely
- **Use Case**: All other apps

---

## How to Test

### Step 1: Build and Install
```bash
cd "d:\device owner project\MyApplication"
./gradlew installDebug
```

### Step 2: Set Device Owner
```bash
# Make sure no accounts are on the device first
adb shell dpm set-device-owner com.selfcontrol/.deviceowner.DeviceOwnerReceiver
```

**Expected Output**:
```
Success: Device owner set to package com.selfcontrol
```

### Step 3: Open the App
- Dashboard should show **"Device Active" in GREEN**
- "Total Apps" should show actual count (not 0)

### Step 4: Check Logs
```bash
adb logcat -s SelfControl
```

**Expected Logs**:
```
[DeviceOwner] Initializing granular policies
[DeviceOwner] Uninstall blocked for com.selfcontrol: true
[DeviceOwner] Accessibility Service Locked & Enabled
[Startup] Scanning installed apps...
[Startup] Registering with Backend...
[Startup] ✅ Registration Successful! ID: 123
```

### Step 5: Test Lock Feature
1. Go to **Apps** screen in your app
2. Find any app (e.g., "Calculator")
3. Toggle **"Lock"** ON
4. Try to uninstall Calculator from Android Settings
5. **Expected**: Android shows "Action not allowed by admin"

### Step 6: Test Block Feature
1. Toggle **"Block"** ON for an app
2. Go to Android launcher
3. **Expected**: App disappears from launcher
4. Toggle **"Block"** OFF
5. **Expected**: App reappears

---

## Backend Integration

### What the Backend Needs to Return

#### Registration Response (`POST /api/device/register`)
```json
{
  "success": true,
  "data": {
    "device_id": "123",
    "device_token": "your-auth-token",
    "policy_version": 1
  }
}
```

#### Policy Response (`GET /api/device/policies`)
```json
{
  "success": true,
  "data": [
    {
      "package_name": "com.example.app",
      "is_blocked": false,      // Controls visibility/usage
      "is_uninstallable": true  // Maps to isLocked (prevents uninstall)
    }
  ]
}
```

**Important**: The backend field `is_uninstallable` maps to `isLocked` in the Kotlin app.

---

## Configuration

### API URL Setup

Edit `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        // For Android Emulator
        buildConfigField("String", "API_URL", "\"http://10.0.2.2:3000\"")
        
        // For Real Device (replace with your computer's IP)
        // buildConfigField("String", "API_URL", "\"http://192.168.1.50:3000\"")
        
        // For Production (Render)
        // buildConfigField("String", "API_URL", "\"https://your-app.onrender.com\"")
    }
}
```

---

## Troubleshooting

### Problem: "Device Active" shows Red
**Cause**: Device Owner not set
**Solution**: Run ADB command again (Step 2 above)

### Problem: "Total Apps" shows 0
**Cause**: Registration failed or app scan didn't run
**Solution**: 
1. Check logs for registration errors
2. Verify backend is accessible
3. Check API_URL configuration

### Problem: Lock/Block doesn't work
**Cause**: Device Owner not active or policies not synced
**Solution**:
1. Verify "Device Active" is green
2. Check logs for policy enforcement
3. Ensure backend returns correct policy format

### Problem: Registration fails
**Cause**: Backend not reachable
**Solution**:
1. Verify backend is running
2. Check API_URL matches your setup
3. For emulator, use `10.0.2.2` instead of `localhost`
4. For real device, use computer's local IP

---

## Architecture Summary

```
┌─────────────────────────────────────────────────────────┐
│                    App Startup                          │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  DeviceOwnerManager.initialize()                        │
│  • Update UI (isDeviceOwner = true)                     │
│  • Clear global restrictions                            │
│  • Lock SelfControl app                                 │
│  • Force accessibility service                          │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  DeviceSetupRepository.performStartupChecks()           │
│  • Scan installed apps                                  │
│  • Check if registered                                  │
│    ├─ No → Register with backend                        │
│    └─ Yes → Sync policies                               │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  PackageMonitor & Background Workers                    │
│  • Monitor app installs/uninstalls                      │
│  • Periodic policy sync                                 │
│  • Heartbeat to backend                                 │
└─────────────────────────────────────────────────────────┘
```

---

## Files Changed

### Core Logic:
1. ✅ `DeviceOwnerManager.kt` - Granular control implementation
2. ✅ `AppBlockManager.kt` - Policy enforcement with lock/block separation
3. ✅ `DeviceSetupRepository.kt` - Registration and startup logic
4. ✅ `SelfControlApp.kt` - Wiring everything together

### API Integration:
5. ✅ `SelfControlApi.kt` - Updated registration endpoint
6. ✅ `DeviceDto.kt` - Added authToken field

### Already Correct:
- `AppPolicy.kt` - Has both isBlocked and isLocked
- `PolicyMapper.kt` - Maps backend fields correctly
- `AppPreferences.kt` - Has setDeviceOwner method

---

## Next Actions

1. **Verify Build**: Wait for `./gradlew build` to complete
2. **Install App**: Run `./gradlew installDebug`
3. **Set Device Owner**: Use ADB command
4. **Test Features**: Follow testing steps above
5. **Check Backend**: Verify device appears in database

---

## Support

If you encounter issues:
1. Check `adb logcat -s SelfControl` for detailed logs
2. Verify backend is running and accessible
3. Ensure device owner is set correctly
4. Review `DEVICE_OWNER_IMPLEMENTATION.md` for detailed architecture

---

**Status**: ✅ All code changes complete. Ready for testing.
