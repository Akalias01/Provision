import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/widget_item.dart';

/// Manages widget stack and persistence
class WidgetManager extends ChangeNotifier {
  static const String _storageKey = 'widget_stack';

  List<WidgetItem> _widgets = [];
  SharedPreferences? _prefs;

  List<WidgetItem> get widgets => List.unmodifiable(_widgets);

  int get widgetCount => _widgets.length;

  Future<void> initialize() async {
    _prefs = await SharedPreferences.getInstance();
    await _loadWidgets();
  }

  Future<void> _loadWidgets() async {
    final jsonString = _prefs?.getString(_storageKey);
    if (jsonString != null) {
      try {
        final List<dynamic> jsonList = jsonDecode(jsonString);
        _widgets = jsonList.map((json) => WidgetItem.fromJson(json)).toList();
        _widgets.sort((a, b) => a.stackOrder.compareTo(b.stackOrder));
        notifyListeners();
      } catch (e) {
        debugPrint('Error loading widgets: $e');
      }
    }
  }

  Future<void> _saveWidgets() async {
    final jsonString = jsonEncode(_widgets.map((w) => w.toJson()).toList());
    await _prefs?.setString(_storageKey, jsonString);
  }

  Future<void> addWidget(WidgetItem widget) async {
    final newWidget = widget.copyWith(
      stackOrder: _widgets.length,
    );
    _widgets.add(newWidget);
    await _saveWidgets();
    notifyListeners();
  }

  Future<void> removeWidget(String id) async {
    _widgets.removeWhere((w) => w.id == id);
    await _reorderStack();
    await _saveWidgets();
    notifyListeners();
  }

  Future<void> updateWidget(WidgetItem widget) async {
    final index = _widgets.indexWhere((w) => w.id == widget.id);
    if (index != -1) {
      _widgets[index] = widget;
      await _saveWidgets();
      notifyListeners();
    }
  }

  Future<void> moveWidget(String id, int newPosition) async {
    final oldIndex = _widgets.indexWhere((w) => w.id == id);
    if (oldIndex == -1 || newPosition < 0 || newPosition >= _widgets.length) {
      return;
    }

    final widget = _widgets.removeAt(oldIndex);
    _widgets.insert(newPosition, widget);
    await _reorderStack();
    await _saveWidgets();
    notifyListeners();
  }

  Future<void> _reorderStack() async {
    for (int i = 0; i < _widgets.length; i++) {
      _widgets[i] = _widgets[i].copyWith(stackOrder: i);
    }
  }

  WidgetItem? getWidget(String id) {
    try {
      return _widgets.firstWhere((w) => w.id == id);
    } catch (e) {
      return null;
    }
  }

  List<WidgetItem> getWidgetsByType(String type) {
    return _widgets.where((w) => w.type == type).toList();
  }

  Future<void> clearAll() async {
    _widgets.clear();
    await _saveWidgets();
    notifyListeners();
  }
}
