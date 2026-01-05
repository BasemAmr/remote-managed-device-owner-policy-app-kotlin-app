# Device Owner Provisioning Guide

## QR Code Provisioning (Recommended)

This allows you to set up the SelfControl app as Device Owner on a freshly reset Android device.

### Prerequisites
1. Push the APK to GitHub (already done)
2. Factory reset the target device

### How to Provision

1. **Factory reset** the target Android device
2. When you see the Welcome screen, **tap 6 times** on the screen to access QR provisioning mode
3. Connect to Wi-Fi when prompted
4. Scan the QR code below

### Generate QR Code

Use this JSON to generate your QR code at [QR Code Generator](https://www.qr-code-generator.com/) or any QR code tool:

```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.selfcontrol/.deviceowner.DeviceOwnerReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://github.com/BasemAmr/remote-managed-device-owner-policy-app-kotlin-app/raw/main/app/build/outputs/apk/release/app-release.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": "sfAGVTFgR8/ZrUUPXT6IpROIWbJCWfdmJL3rra85saw=",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": true,
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true
}
```

### APK Details
- **Package:** `com.selfcontrol`
- **Device Admin Component:** `com.selfcontrol/.deviceowner.DeviceOwnerReceiver`
- **SHA256 Checksum (Base64):** `sfAGVTFgR8/ZrUUPXT6IpROIWbJCWfdmJL3rra85saw=`
- **SHA256 Checksum (Hex):** `B1F00655316047CFD9AD450F5D3E88A5138859B24259F76624BDEBADAF39B1AC`

### Features Enabled
- ✅ Factory Reset Protection (blocks factory reset from settings)
- ✅ Accessibility Service Enforcement
- ✅ App Blocking via Device Owner
- ✅ URL Blocking via VPN

### Alternative: ADB Provisioning (For Testing)

If you have ADB access and the device has no accounts set up:

```bash
# Install the APK
adb install app-release.apk

# Set as Device Owner
adb shell dpm set-device-owner com.selfcontrol/.deviceowner.DeviceOwnerReceiver
```

### Troubleshooting

1. **"Package checksum mismatch"**: The APK was updated. Re-calculate the checksum and update the QR code.
2. **"Device already has an account"**: Factory reset the device before provisioning.
3. **QR scanner not appearing**: Make sure you tap 6 times on the Welcome screen (not after setup).

### Updating the APK

When you rebuild the APK, you must:
1. Recalculate the SHA256 hash: `Get-FileHash app-release.apk -Algorithm SHA256`
2. Convert to Base64
3. Update the QR code configuration
4. Push to GitHub
