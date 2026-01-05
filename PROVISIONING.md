# Device Owner Provisioning Guide

## IMPORTANT: Use GitHub Releases URL

The QR code MUST use the GitHub Releases download URL, not the raw file URL.

## Final Production QR Code Configuration

```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.selfcontrol/.deviceowner.DeviceOwnerReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://github.com/BasemAmr/remote-managed-device-owner-policy-app-kotlin-app/releases/download/v1.0.0/app-release.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": "CP0ODPPh0IXbL6jKQjaTps4CAd10cT//zoAWyF45ars=",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": true,
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true
}
```

## APK Details (Final Production Build)
- **Package:** `com.selfcontrol`
- **Device Admin Component:** `com.selfcontrol/.deviceowner.DeviceOwnerReceiver`
- **SHA256 Checksum (Base64):** `CP0ODPPh0IXbL6jKQjaTps4CAd10cT//zoAWyF45ars=`
- **SHA256 Checksum (Hex):** `08FD0E0CF3E1D085DB2FA8CA423693A6CE0201DD74713FFFCE8016C85E396ABB`
- **testOnly:** ❌ Removed (production ready)

## Setup Steps

### 1. Upload APK to GitHub Releases

1. Go to: https://github.com/BasemAmr/remote-managed-device-owner-policy-app-kotlin-app/releases/new
2. Create release:
   - **Tag:** `v1.0.0`
   - **Title:** `Production Release v1.0.0`
   - **Description:** `Production-ready Device Owner APK`
3. Upload: `app/build/outputs/apk/release/app-release.apk`
4. Click **"Publish release"**

### 2. Generate QR Code

1. Copy the JSON configuration above
2. Go to: https://www.qr-code-generator.com/
3. Paste the JSON
4. Generate and download QR code

### 3. Provision Device

1. **Factory reset** the target device
2. **DO NOT sign in** to any account
3. On the Welcome screen, **tap 6 times**
4. Connect to Wi-Fi
5. Scan the QR code
6. Wait for download and installation

## Troubleshooting

### "Unable to download device admin"
- ✅ Verify the GitHub Release exists at the URL
- ✅ Test the download URL in a browser
- ✅ Ensure device has internet connection

### "Unable to set device admin"
- ✅ Ensure device has NO accounts (Google, Samsung, etc.)
- ✅ Factory reset and try again WITHOUT signing in
- ✅ Verify `android:testOnly` is removed from manifest

### "Package checksum mismatch"
- ✅ Recalculate checksum after any rebuild
- ✅ Update QR code with new checksum
- ✅ Upload new APK to GitHub Releases

## Features Enabled
- ✅ Factory Reset Protection
- ✅ Accessibility Service Enforcement
- ✅ App Blocking via Device Owner
- ✅ URL Blocking via VPN
- ✅ Production-ready (no test flags)
