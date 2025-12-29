import 'package:flutter/foundation.dart';
import 'package:home_widget/home_widget.dart';
import '../models/widget_item.dart';

/// Service to communicate with native widgets (iOS WidgetKit & Android App Widgets)
class NativeWidgetService {
  static const String _widgetName = 'StackableWidget';
  static const String _iOSWidgetName = 'StackableWidgetExtension';
  static const String _androidWidgetName = 'StackableWidgetProvider';

  /// Update native widget with current data
  Future<void> updateNativeWidget(WidgetItem widget) async {
    try {
      // Save data to shared preferences that native widgets can access
      await HomeWidget.saveWidgetData<String>('widget_id', widget.id);
      await HomeWidget.saveWidgetData<String>('widget_type', widget.type);
      await HomeWidget.saveWidgetData<String>('widget_title', widget.title);
      await HomeWidget.saveWidgetData<String>(
        'widget_data',
        widget.data.toString(),
      );
      await HomeWidget.saveWidgetData<int>('stack_order', widget.stackOrder);

      // Update the widget on the home screen
      await HomeWidget.updateWidget(
        name: defaultTargetPlatform == TargetPlatform.iOS
            ? _iOSWidgetName
            : _androidWidgetName,
      );
    } catch (e) {
      debugPrint('Error updating native widget: $e');
    }
  }

  /// Update widget stack count
  Future<void> updateWidgetStack(List<WidgetItem> widgets) async {
    try {
      await HomeWidget.saveWidgetData<int>('widget_count', widgets.length);

      // Save top 5 widgets for display
      for (int i = 0; i < widgets.length && i < 5; i++) {
        final widget = widgets[i];
        await HomeWidget.saveWidgetData<String>('widget_${i}_title', widget.title);
        await HomeWidget.saveWidgetData<String>('widget_${i}_type', widget.type);
      }

      await HomeWidget.updateWidget(
        name: defaultTargetPlatform == TargetPlatform.iOS
            ? _iOSWidgetName
            : _androidWidgetName,
      );
    } catch (e) {
      debugPrint('Error updating widget stack: $e');
    }
  }

  /// Handle widget interactions from home screen
  Future<String?> getInitialData() async {
    try {
      return await HomeWidget.initiallyLaunchedFromHomeWidget();
    } catch (e) {
      debugPrint('Error getting initial data: $e');
      return null;
    }
  }

  /// Register callbacks for widget interactions
  void registerInteractivity(Function(Uri?) callback) {
    HomeWidget.widgetClicked.listen(callback);
  }
}
