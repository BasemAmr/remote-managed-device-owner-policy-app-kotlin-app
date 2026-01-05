# GitHub Release Setup for Device Owner Provisioning

## Problem
QR code provisioning fails with "unable to download device admin" because GitHub's raw file URLs are unreliable for Android provisioning.

## Solution: Use GitHub Releases

### Step 1: Create a GitHub Release

1. Go to your repo: https://github.com/BasemAmr/remote-managed-device-owner-policy-app-kotlin-app
2. Click **"Releases"** (right sidebar)
3. Click **"Create a new release"**
4. Fill in:
   - **Tag:** `v1.0.0` (or any version)
   - **Title:** `Production Release v1.0.0`
   - **Description:** `Device Owner APK with factory reset protection`
5. **Upload the APK:**
   - Drag and drop: `app/build/outputs/apk/release/app-release.apk`
6. Click **"Publish release"**

### Step 2: Get the Release Download URL

After publishing, the download URL will be:
```
https://github.com/BasemAmr/remote-managed-device-owner-policy-app-kotlin-app/releases/download/v1.0.0/app-release.apk
```

### Step 3: Update QR Code Configuration

Use this new JSON (with the release URL):

```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.selfcontrol/.deviceowner.DeviceOwnerReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://github.com/BasemAmr/remote-managed-device-owner-policy-app-kotlin-app/releases/download/v1.0.0/app-release.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": "Zu5xRcw82oROE7hWSXAXgPQrCMt17C6f3JKANrblAD8=",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": true,
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true
}
```

### Step 4: Generate New QR Code

1. Copy the JSON above
2. Go to https://www.qr-code-generator.com/
3. Paste the JSON
4. Generate and save the QR code

### Alternative: Use a Direct Hosting Service

If GitHub Releases still doesn't work, you can use:

1. **Dropbox** - Upload APK, get direct download link
2. **Google Drive** - Make public, get direct download link
3. **Your own web server** - Host the APK at any HTTPS URL

The key is that the URL must:
- ✅ Be HTTPS
- ✅ Return the APK file directly (not an HTML page)
- ✅ Not require authentication
- ✅ Be publicly accessible

## Troubleshooting

If provisioning still fails:
1. Test the download URL in a browser - it should download the APK immediately
2. Verify the checksum matches
3. Ensure Wi-Fi is connected during provisioning
4. Try a different network (some corporate networks block downloads)
