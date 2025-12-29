# Stackable Widgets

A Flutter application featuring interactive, stackable widgets for both iOS and Android, similar to iOS home screen widgets.

## Features

- **Stackable Widgets**: Create and manage multiple widgets in a stack
- **Interactive**: Widgets respond to user interactions (tap, increment, toggle, etc.)
- **Cross-Platform**: Works on both iOS (using WidgetKit) and Android (using App Widgets)
- **Multiple Widget Types**:
  - Counter Widget
  - Todo List Widget
  - Weather Widget
  - Calendar Widget (coming soon)
  - Notes Widget (coming soon)
- **Drag & Drop Reordering**: Rearrange widgets by dragging
- **Persistent Storage**: Widgets persist between app launches
- **Native Home Screen Widgets**: Display widget information on the home screen

## Architecture

### Flutter App (Dart)
- **Models**: Data structures for widgets (`WidgetItem`, `WidgetConfig`)
- **Services**:
  - `WidgetManager`: Manages widget stack and persistence
  - `NativeWidgetService`: Bridges Flutter with native widgets
- **Widgets**: Reusable widget UI components
- **Screens**: Main app screens (Home, Add Widget)

### iOS (Swift - WidgetKit)
- Uses WidgetKit for native iOS home screen widgets
- Shares data via App Groups and UserDefaults
- Located in `ios/WidgetExtension/`

### Android (Kotlin - App Widgets)
- Uses Android App Widget framework
- Shares data via SharedPreferences
- Located in `android/app/src/main/java/com/stackablewidgets/`

## Getting Started

### Prerequisites

- Flutter SDK (3.0.0 or higher)
- Xcode (for iOS development)
- Android Studio (for Android development)

### Installation

1. **Clone or navigate to the project**:
   ```bash
   cd /home/user/stackable_widgets
   ```

2. **Install Flutter dependencies**:
   ```bash
   flutter pub get
   ```

3. **Run the app**:
   ```bash
   # For iOS
   flutter run -d ios

   # For Android
   flutter run -d android
   ```

### iOS Widget Setup

1. Open the iOS project in Xcode:
   ```bash
   open ios/Runner.xcworkspace
   ```

2. Add a new Widget Extension target:
   - File → New → Target
   - Select "Widget Extension"
   - Name it "StackableWidgetExtension"
   - Replace the generated code with the code from `ios/WidgetExtension/StackableWidget.swift`

3. Set up App Groups:
   - Select Runner target → Signing & Capabilities
   - Add "App Groups" capability
   - Create group: `group.com.stackablewidgets`
   - Repeat for StackableWidgetExtension target

4. Build and run the app

5. Add the widget to your home screen:
   - Long press on the home screen
   - Tap the "+" button
   - Search for "Stackable Widgets"
   - Add the widget

### Android Widget Setup

1. The Android widget is already configured in the AndroidManifest.xml

2. Build and run the app:
   ```bash
   flutter run -d android
   ```

3. Add the widget to your home screen:
   - Long press on the home screen
   - Tap "Widgets"
   - Find "Stackable Widgets"
   - Drag to your home screen

## Usage

### Creating Widgets

1. Tap the **+** button on the home screen
2. Enter a title for your widget
3. Select a widget type
4. Tap "Add Widget"

### Interacting with Widgets

- **Counter Widget**: Tap + or - buttons to increment/decrement
- **Todo Widget**: Check/uncheck todos, tap "Add Todo" to create new items
- **Weather Widget**: Tap refresh to update (simulated data)

### Reordering Widgets

- Long press on a widget
- Drag it to a new position in the stack
- Release to place it

### Deleting Widgets

- Tap on a widget to view details
- Tap "Delete Widget" button

### Clearing All Widgets

- Tap the trash icon in the app bar
- Confirm the action

## Project Structure

```
stackable_widgets/
├── lib/
│   ├── main.dart                    # App entry point
│   ├── models/
│   │   ├── widget_item.dart         # Widget data model
│   │   └── widget_config.dart       # Widget configuration
│   ├── services/
│   │   ├── widget_manager.dart      # Widget management logic
│   │   └── native_widget_service.dart # Native widget bridge
│   ├── widgets/
│   │   ├── counter_widget.dart      # Counter widget UI
│   │   ├── todo_widget.dart         # Todo widget UI
│   │   └── weather_widget.dart      # Weather widget UI
│   └── screens/
│       ├── home_screen.dart         # Main screen
│       └── add_widget_screen.dart   # Add widget screen
├── ios/
│   ├── Runner/                      # iOS app
│   └── WidgetExtension/             # iOS home screen widget
│       ├── StackableWidget.swift
│       └── Info.plist
├── android/
│   └── app/
│       └── src/main/
│           ├── java/                # Kotlin code
│           │   └── StackableWidgetProvider.kt
│           ├── res/                 # Android resources
│           │   ├── layout/
│           │   ├── drawable/
│           │   └── xml/
│           └── AndroidManifest.xml
└── pubspec.yaml                     # Dependencies
```

## Dependencies

- **home_widget** (^0.6.0): Communication between Flutter and native widgets
- **provider** (^6.1.1): State management
- **shared_preferences** (^2.2.2): Local data persistence
- **flutter_staggered_grid_view** (^0.7.0): Grid layout
- **animations** (^2.0.11): Smooth transitions

## Customization

### Adding New Widget Types

1. Add widget type to `lib/models/widget_config.dart`:
   ```dart
   enum WidgetType {
     // ... existing types
     myCustomWidget,
   }
   ```

2. Create widget UI in `lib/widgets/my_custom_widget.dart`

3. Add configuration in `WidgetConfig.configs`

4. Update `home_screen.dart` to handle the new widget type

5. Update native widgets to display the new type

### Customizing Widget Appearance

- Widget colors and styles are defined in each widget file
- Use Flutter's theming system in `main.dart` for global styles
- iOS widget styles are in `StackableWidget.swift`
- Android widget styles are in `res/layout/stackable_widget.xml`

## Troubleshooting

### iOS Widget Not Updating

- Ensure App Groups are properly configured
- Check that the group name matches in both Flutter and Swift code
- Rebuild the app and widget extension

### Android Widget Not Showing

- Verify the widget is declared in AndroidManifest.xml
- Check SharedPreferences keys match between Flutter and Kotlin
- Rebuild the app

### Data Not Persisting

- Ensure `shared_preferences` package is installed
- Check for errors in console when saving/loading data
- Clear app data and try again

## Future Enhancements

- [ ] Calendar event widget
- [ ] Quick notes widget
- [ ] Weather API integration
- [ ] Widget themes/customization
- [ ] Sharing widgets between users
- [ ] Cloud sync
- [ ] Widget templates
- [ ] Animation improvements

## License

This project is open source and available under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions, please open an issue on the project repository.
