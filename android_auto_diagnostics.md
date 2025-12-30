# Android Auto Diagnostics and Testing Guide

This guide provides comprehensive ADB commands to diagnose why REZON8 may not be appearing in Android Auto launcher customization.

## Prerequisites

1. Enable Developer Mode on Android Auto:
   - Open Android Auto app on phone
   - Tap version number 10 times
   - Go to Settings > Developer Settings
   - Enable "Unknown sources" (allows non-Google Play apps)

2. Connect phone via USB debugging:
   ```bash
   adb devices
   ```

---

## Step 1: Check if MediaSessions are Active

This shows all active media sessions on the device:

```bash
adb shell dumpsys media_session
```

**What to look for:**
- Look for "REZON8" or "com.example.rezon8" in the output
- Check if both services are listed:
  - `RezonPlaybackService` (Media3 modern)
  - `RezonLegacyBrowserService` (MediaBrowserServiceCompat)
- Verify session state is "active: true"

**Expected output snippet:**
```
Session: MediaSessionRecord
  package=com.example.rezon8
  userId=0
  active=true
  flags=0x3
  callback=android.media.session.MediaSession$CallbackStub
```

---

## Step 2: Check MediaBrowserService Discovery

This shows which MediaBrowser services are registered:

```bash
adb shell dumpsys package com.example.rezon8 | grep -A 20 "Service"
```

**What to look for:**
- Both services should be listed:
  - `RezonPlaybackService`
  - `RezonLegacyBrowserService`
- Each should have these intent filters:
  - `android.media.browse.MediaBrowserService`
  - `android.intent.action.MEDIA_BUTTON` (optional)
  - `android.media.action.MEDIA_PLAY_FROM_SEARCH` (optional)

---

## Step 3: Force Android Auto to Reconnect

This forces Auto to re-scan for media apps:

```bash
adb shell am broadcast -a com.google.android.gms.car.RECONNECT
```

After running this:
1. Wait 5-10 seconds
2. Open Android Auto app
3. Go to Customize Launcher
4. Check if REZON8 appears

---

## Step 4: Check Logcat for Connection Errors

Monitor logs while connecting Android Auto:

```bash
adb logcat -s MediaBrowser:* MediaSession:* CarMediaService:* RezonPlaybackService:* RezonLegacyBrowserService:*
```

**Start Android Auto** and watch for:

1. **Connection attempts:**
   ```
   RezonPlaybackService: onGetLibraryRoot called by: com.google.android.projection.gearhead
   RezonLegacyBrowserService: onGetRoot called by: com.google.android.projection.gearhead
   ```

2. **Root hints logging:**
   ```
   RezonPlaybackService: Root hints received:
   RezonPlaybackService:   android.media.browse.EXTRA_RECENT = true
   ```

3. **Any errors:**
   - Look for "SecurityException", "IllegalStateException", "RemoteException"
   - Connection refused errors
   - Timeout errors

---

## Step 5: Verify App Installation

Check if the app is properly installed with all permissions:

```bash
adb shell pm list packages | grep rezon8
adb shell dumpsys package com.example.rezon8 | grep permission
```

**What to look for:**
- Package should be listed: `com.example.rezon8`
- Check for media-related permissions granted

---

## Step 6: Check Android Auto Package

Verify Android Auto is installed and updated:

```bash
adb shell pm list packages | grep gearhead
adb shell dumpsys package com.google.android.projection.gearhead | grep version
```

**Note:** Some devices use different package names:
- `com.google.android.projection.gearhead` (most common)
- `com.google.android.gms` (Android Auto embedded)

---

## Step 7: Test MediaBrowser Connection Manually

Use Google's test tool to verify MediaBrowser compatibility:

```bash
# Install the Media Controller Test app (if available)
adb install -r path/to/MediaControllerTest.apk

# Or use adb shell to manually connect
adb shell am start -n com.example.rezon8/.service.RezonPlaybackService
```

---

## Step 8: Check Content Style Support

Verify that content style hints are being sent:

```bash
adb logcat | grep "CONTENT_STYLE"
```

**What to look for:**
```
RezonPlaybackService: Returning root with Android Auto content style hints
```

---

## Step 9: Restart Android Auto Service

Sometimes Auto needs a complete restart:

```bash
# Force stop Android Auto
adb shell am force-stop com.google.android.projection.gearhead

# Clear Auto cache (optional)
adb shell pm clear com.google.android.projection.gearhead

# Restart Auto
adb shell am start -n com.google.android.projection.gearhead/.setup.SetupActivity
```

---

## Step 10: Test on DHU (Desktop Head Unit)

If you have DHU installed, test the app in the emulator:

```bash
# Start DHU server mode
desktop-head-unit.exe

# Install app
adb install -r app-debug.apk

# Launch Android Auto in DHU
adb shell am start -n com.google.android.projection.gearhead/.setup.SetupActivity
```

---

## Common Issues and Solutions

### Issue 1: App Not Appearing in Launcher Customization

**Possible causes:**
1. MediaSession not active when Auto scans
2. Missing content style hints
3. Android Auto caching old app list
4. Legacy Android Auto version requiring MediaBrowserServiceCompat

**Solutions:**
- Run Step 3 (Force reconnect)
- Check Step 4 logs for connection attempts
- Verify both services are running (Step 1)

---

### Issue 2: App Appears but Content Not Loading

**Possible causes:**
1. `onGetChildren()` returning empty lists
2. MediaItems not properly formatted
3. Repository returning null/empty data

**Solutions:**
- Check logcat: `adb logcat -s RezonPlaybackService:D RezonLegacyBrowserService:D`
- Look for "Returning X items for parentId: root"
- Verify database has books loaded

---

### Issue 3: App Crashes When Opened in Auto

**Possible causes:**
1. Null pointer exceptions in service
2. Missing Hilt dependency injection
3. ExoPlayer not initialized

**Solutions:**
- Check crash logs: `adb logcat -s AndroidRuntime:E`
- Verify Hilt is working: `adb logcat | grep Hilt`

---

## Quick Diagnostic Script

Run all checks at once:

```bash
echo "=== Media Sessions ==="
adb shell dumpsys media_session | grep -A 10 rezon8

echo "\n=== Services Registered ==="
adb shell dumpsys package com.example.rezon8 | grep -A 5 "Service"

echo "\n=== Force Reconnect ==="
adb shell am broadcast -a com.google.android.gms.car.RECONNECT

echo "\n=== Monitoring Logs (Ctrl+C to stop) ==="
adb logcat -s MediaBrowser:* MediaSession:* RezonPlaybackService:* RezonLegacyBrowserService:*
```

---

## Expected Behavior Timeline

When Android Auto connects to REZON8:

1. **T+0s:** Auto broadcasts discovery request
2. **T+0.1s:** Both services receive connection request
3. **T+0.2s:** `onGetLibraryRoot()` called with root hints logged
4. **T+0.3s:** Root returned with content style hints
5. **T+0.5s:** Auto caches REZON8 as available media app
6. **T+1s:** REZON8 appears in launcher customization

If any step fails, check the corresponding diagnostic above.

---

## Additional Resources

- [Android Auto Developer Guide](https://developer.android.com/training/cars/media)
- [Media3 Documentation](https://developer.android.com/guide/topics/media/media3)
- [MediaBrowserService Migration](https://developer.android.com/guide/topics/media/media3/getting-started/migration-guide)

---

## Contact and Debugging

If issues persist after all diagnostics:

1. Capture full logcat during Auto connection:
   ```bash
   adb logcat -d > android_auto_debug.txt
   ```

2. Capture dumpsys output:
   ```bash
   adb shell dumpsys media_session > media_session_dump.txt
   adb shell dumpsys package com.example.rezon8 > package_dump.txt
   ```

3. Check for ANR (Application Not Responding):
   ```bash
   adb shell ls /data/anr/
   adb pull /data/anr/traces.txt
   ```

---

## Version-Specific Notes

### Android 10 (API 29)
- Requires both MediaLibraryService and MediaBrowserServiceCompat
- Must have foregroundServiceType="mediaPlayback"

### Android 11+ (API 30+)
- Can use MediaLibraryService alone, but legacy support recommended
- Better MediaSession discovery

### Android Auto App Versions
- **5.x and below:** Only supports MediaBrowserServiceCompat
- **6.x - 8.x:** Supports both APIs
- **9.x and above:** Prefers MediaLibraryService but falls back to legacy

**CRITICAL:** Always include both services for maximum compatibility!
