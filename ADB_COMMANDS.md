# ADB Quick Reference - Device Owner Testing

## Uninstall Commands

### Standard Uninstall (Now Works with testOnly flag)
```bash
adb uninstall com.selfcontrol.deviceowner
```

### Force Uninstall (if standard fails)
```bash
# Remove device owner first
adb shell dpm remove-active-admin com.selfcontrol.deviceowner/.deviceowner.DeviceOwnerReceiver

# Then uninstall
adb uninstall com.selfcontrol.deviceowner
```

## Installation Commands

### Install Debug Build
```bash
./gradlew installDebug
```

### Install with ADB directly
```bash
adb install -t app/build/outputs/apk/debug/app-debug.apk
```
**Note:** The `-t` flag is required for testOnly apps

## Device Owner Setup

### Set as Device Owner (Factory Reset Device First!)
```bash
adb shell dpm set-device-owner com.selfcontrol.deviceowner/.deviceowner.DeviceOwnerReceiver
```

### Check Device Owner Status
```bash
adb shell dpm list-owners
```

### Remove Device Owner
```bash
adb shell dpm remove-active-admin com.selfcontrol.deviceowner/.deviceowner.DeviceOwnerReceiver
```

## Data Management

### Clear App Data (Keep App Installed)
```bash
adb shell pm clear com.selfcontrol.deviceowner
```

### View App Data Directory
```bash
adb shell ls -la /data/data/com.selfcontrol.deviceowner/
```

### View DataStore Preferences
```bash
adb shell cat /data/data/com.selfcontrol.deviceowner/files/datastore/app_preferences.preferences_pb
```

## Logging

### Watch All App Logs
```bash
adb logcat | grep -E "Startup|AuthInterceptor|AppPrefs|PolicyRepo|DeviceOwner"
```

### Watch Only Errors
```bash
adb logcat *:E | grep com.selfcontrol
```

### Save Logs to File
```bash
adb logcat > device_logs.txt
```

### Clear Logcat Buffer
```bash
adb logcat -c
```

## Testing Workflow

### Complete Reset & Test Cycle
```bash
# 1. Remove device owner
adb shell dpm remove-active-admin com.selfcontrol.deviceowner/.deviceowner.DeviceOwnerReceiver

# 2. Uninstall app
adb uninstall com.selfcontrol.deviceowner

# 3. Build and install
./gradlew installDebug

# 4. Set as device owner
adb shell dpm set-device-owner com.selfcontrol.deviceowner/.deviceowner.DeviceOwnerReceiver

# 5. Watch logs
adb logcat -c && adb logcat | grep -E "Startup|AuthInterceptor"

# 6. Launch app
adb shell am start -n com.selfcontrol.deviceowner/.presentation.MainActivity
```

### Test Self-Healing (Token Recovery)
```bash
# Clear app data to simulate token loss
adb shell pm clear com.selfcontrol.deviceowner

# Launch app and watch it re-register
adb logcat -c && adb logcat | grep Startup

# Launch app
adb shell am start -n com.selfcontrol.deviceowner/.presentation.MainActivity
```

## Troubleshooting

### "Not allowed to set the device owner" Error
**Solution:** Factory reset the device first. Device owner can only be set on a fresh device with no accounts.

### "INSTALL_FAILED_TEST_ONLY" Error
**Solution:** Use `-t` flag: `adb install -t app-debug.apk`

### App Won't Uninstall
**Solution:** Remove device owner first, then uninstall:
```bash
adb shell dpm remove-active-admin com.selfcontrol.deviceowner/.deviceowner.DeviceOwnerReceiver
adb uninstall com.selfcontrol.deviceowner
```

### Can't See Logs
**Solution:** Make sure Timber is initialized and check log level:
```bash
adb logcat -v time | grep -i timber
```

## Useful Shortcuts

### Quick Reinstall
```bash
adb uninstall com.selfcontrol.deviceowner && ./gradlew installDebug
```

### Full Reset
```bash
adb shell dpm remove-active-admin com.selfcontrol.deviceowner/.deviceowner.DeviceOwnerReceiver && \
adb uninstall com.selfcontrol.deviceowner && \
./gradlew clean installDebug && \
adb shell dpm set-device-owner com.selfcontrol.deviceowner/.deviceowner.DeviceOwnerReceiver
```

### Monitor Network Requests
```bash
adb logcat | grep -E "OkHttp|AuthInterceptor|Retrofit"
```
