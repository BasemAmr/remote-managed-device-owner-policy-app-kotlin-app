# Device Owner Setup Guide

## Prerequisites
- Android device with Android 8.0 (API 26) or higher
- ADB (Android Debug Bridge) installed on your computer
- USB debugging enabled on the device
- **IMPORTANT**: Device must be factory reset (no accounts added)

## Method 1: ADB Setup (Development)

### Step 1: Factory Reset Device
1. Go to Settings → System → Reset options
2. Select "Erase all data (factory reset)"
3. **Do NOT add any Google account after reset**

### Step 2: Enable USB Debugging
1. Go to Settings → About phone
2. Tap "Build number" 7 times to enable Developer options
3. Go to Settings → System → Developer options
4. Enable "USB debugging"

### Step 3: Install the App
```bash
# Build and install the app
./gradlew installDebug

# Or install manually
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 4: Set as Device Owner
```bash
# Set device owner (replace with your actual package name if different)
adb shell dpm set-device-owner com.selfcontrol/.deviceowner.DeviceOwnerReceiver

# Expected output:
# Success: Device owner set to package com.selfcontrol
```

### Step 5: Verify Device Owner Status
```bash
# Check if device owner is set
adb shell dpm list-owners

# Expected output:
# Device Owner: package=com.selfcontrol
```

### Step 6: Enable Accessibility Service
1. Open the app
2. Go to Settings
3. Enable "App Monitoring Service"
4. Grant accessibility permission when prompted

## Method 2: QR Code Provisioning (Production)

### Generate QR Code
Create a QR code with this JSON:

```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.selfcontrol/.deviceowner.DeviceOwnerReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://yourserver.com/app-release.apk",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false
}
```

### Provisioning Steps
1. Factory reset the device
2. On the welcome screen, tap the screen 6 times
3. Scan the QR code
4. Follow the on-screen instructions

## Troubleshooting

### Error: "Not allowed to set the device owner"
- **Cause**: Device has accounts or is already set up
- **Solution**: Factory reset and try again without adding accounts

### Error: "java.lang.IllegalStateException: Trying to set device owner but device is already provisioned"
- **Cause**: Device is already provisioned
- **Solution**: Factory reset required

### Error: "Trying to set the device owner, but device owner is already set"
- **Cause**: Another app is already device owner
- **Solution**: Remove existing device owner first:
  ```bash
  adb shell dpm remove-active-admin <existing-package>/.receiver
  ```

### Remove Device Owner (for testing)
```bash
# Remove device owner status
adb shell dpm remove-active-admin com.selfcontrol/.deviceowner.DeviceOwnerReceiver

# Uninstall app
adb uninstall com.selfcontrol
```

## Testing Device Owner Features

### Test App Blocking
```bash
# Block an app (e.g., Chrome)
# This should be done through the app UI, but you can verify via:
adb shell dumpsys device_policy
```

### Test Accessibility Monitoring
1. Install a test app (e.g., Chrome)
2. Block it via Self Control app
3. Try to open Chrome
4. You should be redirected to home screen

### Check Logs
```bash
# View app logs
adb logcat -s SelfControl:* DeviceOwner:* AppBlockManager:*

# Clear logs
adb logcat -c
```

## Production Deployment

### Requirements
1. Signed APK with release keystore
2. HTTPS server to host APK
3. QR code generation tool
4. Device provisioning documentation for end users

### Security Considerations
- Store keystore securely
- Use HTTPS for APK download
- Implement certificate pinning
- Enable ProGuard/R8 obfuscation
- Set up Firebase Crashlytics for monitoring

## Important Notes

⚠️ **Device Owner Limitations:**
- Only one device owner per device
- Cannot be set if device has accounts
- Requires factory reset to remove
- Cannot be uninstalled normally (must remove device owner first)

✅ **Device Owner Capabilities:**
- Hide/disable apps
- Prevent app uninstallation
- Lock device
- Wipe device data
- Set user restrictions
- Monitor app usage

## Support

For issues or questions:
- Check logs: `adb logcat`
- Review device policy: `adb shell dumpsys device_policy`
- Verify permissions: `adb shell dumpsys package com.selfcontrol`
