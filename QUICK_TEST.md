# Quick Android Auto Test - 5 Minute Verification

## Before Testing

1. **Enable Android Auto Developer Mode:**
   - Open Android Auto app
   - Tap version number 10 times
   - Enable "Unknown sources" in Developer Settings

2. **Connect Phone:**
   ```bash
   adb devices
   ```

---

## Step-by-Step Test (5 minutes)

### 1. Build & Install (1 min)
```bash
cd C:\Users\seana\AndroidStudioProjects\REZON8
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Verify Services Active (30 sec)
```bash
adb shell dumpsys media_session | grep -i rezon
```

**Expected:** See `RezonPlaybackService` or `RezonLegacyBrowserService` with `active=true`

### 3. Force Auto Reconnect (10 sec)
```bash
adb shell am broadcast -a com.google.android.gms.car.RECONNECT
```

Wait 5 seconds for Auto to rescan.

### 4. Check Launcher (1 min)
1. Open Android Auto app on phone
2. Tap hamburger menu (≡)
3. Tap "Customize launcher"
4. Look for "REZON8" in available apps

**Success:** REZON8 appears and can be added to launcher

### 5. Monitor Logs (2 min)
```bash
adb logcat -s RezonPlaybackService:D RezonLegacyBrowserService:D -v brief
```

While monitoring, open Android Auto and browse to REZON8.

**Expected logs:**
```
RezonPlaybackService: onGetLibraryRoot called by: com.google.android.projection.gearhead
RezonPlaybackService: Root hints received:
RezonPlaybackService: Returning root with Android Auto content style hints
RezonPlaybackService: onGetChildren called for parentId: root
RezonPlaybackService: Returning 3 browsable folders for root
```

---

## Quick Troubleshooting

### REZON8 Not in Launcher List

**Fix 1:** Clear Auto cache
```bash
adb shell pm clear com.google.android.projection.gearhead
adb shell am broadcast -a com.google.android.gms.car.RECONNECT
```

**Fix 2:** Verify both services registered
```bash
adb shell dumpsys package com.example.rezon8 | grep -A 5 "Service"
```
Should see both `RezonPlaybackService` AND `RezonLegacyBrowserService`

**Fix 3:** Check for crashes
```bash
adb logcat -s AndroidRuntime:E
```

### App Appears But No Content

**Fix:** Check repository has books
```bash
adb logcat -s LibraryRepository:D
```

Look for "Returning X books for Auto"

### App Crashes When Opened

**Fix:** Check Hilt injection
```bash
adb logcat | grep -i "hilt\|dagger"
```

Look for dependency injection errors.

---

## One-Liner Complete Test

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk && sleep 2 && adb shell am broadcast -a com.google.android.gms.car.RECONNECT && adb logcat -s RezonPlaybackService:D RezonLegacyBrowserService:D
```

Then open Android Auto app and check Customize launcher.

---

## Success Criteria

✅ **Passed if:**
- REZON8 appears in "Customize launcher" list
- Can add REZON8 to launcher
- See 3 folders when opening in Auto
- Books load and can be played

❌ **Failed if:**
- REZON8 not in launcher list
- Opening REZON8 shows empty content
- App crashes when opened from Auto

---

## Next Steps After Success

1. **Test in actual car** (if available)
2. **Verify playback** works from Auto interface
3. **Test skip forward/back** buttons
4. **Check metadata** displays correctly

---

## If Still Failing

See full diagnostics guide: `android_auto_diagnostics.md`

Or contact with these logs:
```bash
adb logcat -d > full_debug.txt
adb shell dumpsys media_session > media_session.txt
adb shell dumpsys package com.example.rezon8 > package.txt
```
