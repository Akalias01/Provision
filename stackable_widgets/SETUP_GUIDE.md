# Setup Guide for Stackable Widgets

This guide will help you set up and run the Stackable Widgets Flutter project.

## Step 1: Install Flutter

If you don't have Flutter installed, follow these steps:

### macOS/Linux:
```bash
# Download Flutter
git clone https://github.com/flutter/flutter.git -b stable ~/flutter

# Add Flutter to PATH (add to ~/.bashrc or ~/.zshrc)
export PATH="$HOME/flutter/bin:$PATH"

# Verify installation
flutter doctor
```

### Windows:
1. Download Flutter SDK from https://flutter.dev/docs/get-started/install/windows
2. Extract to desired location
3. Add Flutter bin directory to PATH
4. Run `flutter doctor` in Command Prompt

## Step 2: Install Dependencies

Navigate to the project directory and run:

```bash
cd /home/user/stackable_widgets
flutter pub get
```

## Step 3: iOS Setup (Mac only)

### Install Xcode:
1. Install Xcode from the Mac App Store
2. Run: `sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer`
3. Run: `sudo xcodebuild -runFirstLaunch`

### Set up iOS Simulator:
```bash
# List available simulators
xcrun simctl list devices

# Boot a simulator (example)
open -a Simulator
```

### Configure Widget Extension:

1. **Open Xcode Project**:
   ```bash
   open ios/Runner.xcworkspace
   ```

2. **Add Widget Extension**:
   - Click File → New → Target
   - Search for "Widget Extension"
   - Click "Next"
   - Product Name: "StackableWidgetExtension"
   - Uncheck "Include Configuration Intent"
   - Click "Finish"
   - Click "Activate" when prompted

3. **Copy Widget Code**:
   - Delete the generated Swift file
   - Add the `StackableWidget.swift` file from `ios/WidgetExtension/` to the StackableWidgetExtension target
   - Replace `Info.plist` with the one from `ios/WidgetExtension/`

4. **Set up App Groups**:

   **For Runner target**:
   - Select "Runner" target
   - Go to "Signing & Capabilities"
   - Click "+ Capability"
   - Add "App Groups"
   - Click "+"
   - Enter: `group.com.stackablewidgets`

   **For StackableWidgetExtension target**:
   - Select "StackableWidgetExtension" target
   - Repeat the same process
   - Use the same group ID: `group.com.stackablewidgets`

5. **Build and Run**:
   ```bash
   flutter run -d ios
   ```

6. **Add Widget to Home Screen**:
   - Long press on home screen
   - Tap "+" button
   - Search "Stackable Widgets"
   - Add widget

## Step 4: Android Setup

### Install Android Studio:
1. Download from https://developer.android.com/studio
2. Install Android SDK and tools
3. Run `flutter doctor --android-licenses` and accept all

### Set up Android Emulator:
```bash
# List available devices
flutter emulators

# Launch an emulator
flutter emulators --launch <emulator_id>

# Or create a new one in Android Studio
# Tools → Device Manager → Create Device
```

### Configure Kotlin (Already done in project):

The following files are already configured:
- `android/app/build.gradle`
- `android/build.gradle`
- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/java/com/stackablewidgets/stackable_widgets/StackableWidgetProvider.kt`

### Build and Run:
```bash
flutter run -d android
```

### Add Widget to Home Screen:
1. Long press on home screen
2. Tap "Widgets"
3. Find "Stackable Widgets"
4. Drag to home screen

## Step 5: Verify Setup

Run Flutter Doctor to check for issues:
```bash
flutter doctor -v
```

Expected output should show:
- ✓ Flutter (Channel stable)
- ✓ Android toolchain
- ✓ Xcode (Mac only)
- ✓ iOS Simulator (Mac only)
- ✓ Android Studio
- ✓ Connected device

## Step 6: Running the App

### iOS:
```bash
# List devices
flutter devices

# Run on specific device
flutter run -d <device_id>

# Or simply
flutter run
```

### Android:
```bash
# Run on Android
flutter run -d android

# Or on specific device
flutter devices
flutter run -d <device_id>
```

## Common Issues and Solutions

### Issue: "Flutter command not found"
**Solution**: Add Flutter to PATH
```bash
export PATH="$HOME/flutter/bin:$PATH"
```

### Issue: "CocoaPods not installed" (iOS)
**Solution**:
```bash
sudo gem install cocoapods
cd ios
pod install
```

### Issue: "Gradle build failed" (Android)
**Solution**:
```bash
cd android
./gradlew clean
cd ..
flutter clean
flutter pub get
```

### Issue: "App Groups not working" (iOS)
**Solution**:
- Ensure both targets have the SAME App Group ID
- Check that App Group is enabled in both targets
- Clean build folder (Cmd+Shift+K in Xcode)

### Issue: "Widget not updating"
**Solution**:
- iOS: Force close the app and widget, relaunch
- Android: Remove and re-add the widget
- Check that SharedPreferences keys match

## Development Tips

### Hot Reload:
While the app is running, press:
- `r` for hot reload
- `R` for hot restart
- `p` to show performance overlay

### Debugging:
```bash
# Enable verbose logging
flutter run --verbose

# Debug on specific device
flutter run -d <device_id> --debug
```

### Building for Release:

**iOS**:
```bash
flutter build ios --release
```

**Android**:
```bash
flutter build apk --release
# or
flutter build appbundle --release
```

## Next Steps

1. Run the app on your device
2. Create some widgets
3. Add the widget to your home screen
4. Explore the code in `lib/` directory
5. Customize widgets to your liking!

## Additional Resources

- [Flutter Documentation](https://flutter.dev/docs)
- [WidgetKit (iOS) Documentation](https://developer.apple.com/documentation/widgetkit)
- [Android App Widgets Documentation](https://developer.android.com/guide/topics/appwidgets)
- [home_widget Package](https://pub.dev/packages/home_widget)

## Need Help?

If you encounter issues:
1. Check `flutter doctor` output
2. Review error messages carefully
3. Search for the error on Stack Overflow
4. Check the package documentation
5. Open an issue on the project repository

Happy coding! 🚀
