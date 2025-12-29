import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/widget_item.dart';
import '../services/widget_manager.dart';
import '../services/native_widget_service.dart';
import '../widgets/counter_widget.dart';
import '../widgets/todo_widget.dart';
import '../widgets/weather_widget.dart';
import 'add_widget_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final NativeWidgetService _nativeWidgetService = NativeWidgetService();

  @override
  void initState() {
    super.initState();
    _nativeWidgetService.registerInteractivity(_handleWidgetInteraction);
  }

  void _handleWidgetInteraction(Uri? uri) {
    if (uri != null) {
      debugPrint('Widget interaction: $uri');
    }
  }

  Future<void> _updateCounter(WidgetItem item, int delta) async {
    final manager = context.read<WidgetManager>();
    final currentCount = item.data['count'] as int? ?? 0;
    final updatedItem = item.copyWith(
      data: {'count': currentCount + delta},
      lastUpdated: DateTime.now(),
    );
    await manager.updateWidget(updatedItem);
    await _nativeWidgetService.updateNativeWidget(updatedItem);
  }

  Future<void> _toggleTodo(WidgetItem item, int index) async {
    final manager = context.read<WidgetManager>();
    final todos = List<Map<String, dynamic>>.from(
      (item.data['todos'] as List).map((e) => Map<String, dynamic>.from(e as Map)),
    );

    if (index >= 0 && index < todos.length) {
      todos[index]['completed'] = !(todos[index]['completed'] as bool? ?? false);
      final updatedItem = item.copyWith(
        data: {'todos': todos},
        lastUpdated: DateTime.now(),
      );
      await manager.updateWidget(updatedItem);
      await _nativeWidgetService.updateNativeWidget(updatedItem);
    }
  }

  Future<void> _addTodo(WidgetItem item) async {
    final controller = TextEditingController();
    final result = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Add Todo'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(hintText: 'Enter todo'),
          autofocus: true,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, controller.text),
            child: const Text('Add'),
          ),
        ],
      ),
    );

    if (result != null && result.isNotEmpty) {
      final manager = context.read<WidgetManager>();
      final todos = List<Map<String, dynamic>>.from(
        (item.data['todos'] as List? ?? []).map((e) => Map<String, dynamic>.from(e as Map)),
      );
      todos.add({'title': result, 'completed': false});

      final updatedItem = item.copyWith(
        data: {'todos': todos},
        lastUpdated: DateTime.now(),
      );
      await manager.updateWidget(updatedItem);
      await _nativeWidgetService.updateNativeWidget(updatedItem);
    }
  }

  Future<void> _refreshWeather(WidgetItem item) async {
    final manager = context.read<WidgetManager>();
    final updatedItem = item.copyWith(
      data: {
        ...item.data,
        'temperature': (60 + (DateTime.now().millisecond % 40)).toString(),
      },
      lastUpdated: DateTime.now(),
    );
    await manager.updateWidget(updatedItem);
    await _nativeWidgetService.updateNativeWidget(updatedItem);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Stackable Widgets'),
        actions: [
          IconButton(
            icon: const Icon(Icons.delete_sweep),
            onPressed: _confirmClearAll,
          ),
        ],
      ),
      body: Consumer<WidgetManager>(
        builder: (context, manager, child) {
          if (manager.widgets.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    Icons.widgets_outlined,
                    size: 100,
                    color: Colors.grey.shade300,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'No widgets yet',
                    style: TextStyle(
                      fontSize: 24,
                      color: Colors.grey.shade600,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Tap + to add your first widget',
                    style: TextStyle(
                      fontSize: 16,
                      color: Colors.grey.shade500,
                    ),
                  ),
                ],
              ),
            );
          }

          return ReorderableListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: manager.widgets.length,
            onReorder: (oldIndex, newIndex) async {
              if (newIndex > oldIndex) newIndex--;
              final widget = manager.widgets[oldIndex];
              await manager.moveWidget(widget.id, newIndex);
              await _nativeWidgetService.updateWidgetStack(manager.widgets);
            },
            itemBuilder: (context, index) {
              final widget = manager.widgets[index];
              return Padding(
                key: ValueKey(widget.id),
                padding: const EdgeInsets.only(bottom: 16),
                child: SizedBox(
                  height: 250,
                  child: _buildWidgetCard(widget),
                ),
              );
            },
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _navigateToAddWidget(),
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildWidgetCard(WidgetItem item) {
    switch (item.type) {
      case 'counter':
        return CounterWidget(
          item: item,
          onIncrement: () => _updateCounter(item, 1),
          onDecrement: () => _updateCounter(item, -1),
          onTap: () => _showWidgetDetails(item),
        );
      case 'todo':
        return TodoWidget(
          item: item,
          onToggleTodo: (index) => _toggleTodo(item, index),
          onAddTodo: () => _addTodo(item),
          onTap: () => _showWidgetDetails(item),
        );
      case 'weather':
        return WeatherWidget(
          item: item,
          onRefresh: () => _refreshWeather(item),
          onTap: () => _showWidgetDetails(item),
        );
      default:
        return Card(
          child: Center(child: Text('Unknown widget type: ${item.type}')),
        );
    }
  }

  void _showWidgetDetails(WidgetItem item) {
    showModalBottomSheet(
      context: context,
      builder: (context) => Container(
        padding: const EdgeInsets.all(16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              item.title,
              style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text('Type: ${item.type}'),
            Text('Position: #${item.stackOrder + 1}'),
            Text('Last updated: ${_formatDate(item.lastUpdated)}'),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: () async {
                  Navigator.pop(context);
                  await context.read<WidgetManager>().removeWidget(item.id);
                  await _nativeWidgetService.updateWidgetStack(
                    context.read<WidgetManager>().widgets,
                  );
                },
                icon: const Icon(Icons.delete),
                label: const Text('Delete Widget'),
                style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _formatDate(DateTime date) {
    return '${date.hour}:${date.minute.toString().padLeft(2, '0')}';
  }

  Future<void> _navigateToAddWidget() async {
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const AddWidgetScreen()),
    );
  }

  Future<void> _confirmClearAll() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Clear All Widgets'),
        content: const Text('Are you sure you want to remove all widgets?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: const Text('Clear All'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      await context.read<WidgetManager>().clearAll();
      await _nativeWidgetService.updateWidgetStack([]);
    }
  }
}
