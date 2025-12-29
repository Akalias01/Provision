import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/widget_item.dart';
import '../models/widget_config.dart';
import '../services/widget_manager.dart';
import '../services/native_widget_service.dart';

class AddWidgetScreen extends StatefulWidget {
  const AddWidgetScreen({Key? key}) : super(key: key);

  @override
  State<AddWidgetScreen> createState() => _AddWidgetScreenState();
}

class _AddWidgetScreenState extends State<AddWidgetScreen> {
  final _formKey = GlobalKey<FormState>();
  final _titleController = TextEditingController();
  WidgetType _selectedType = WidgetType.counter;
  final NativeWidgetService _nativeWidgetService = NativeWidgetService();

  @override
  void dispose() {
    _titleController.dispose();
    super.dispose();
  }

  Future<void> _addWidget() async {
    if (_formKey.currentState!.validate()) {
      final manager = context.read<WidgetManager>();

      final widget = WidgetItem(
        id: DateTime.now().millisecondsSinceEpoch.toString(),
        type: _selectedType.name,
        title: _titleController.text,
        data: _getInitialData(_selectedType),
      );

      await manager.addWidget(widget);
      await _nativeWidgetService.updateWidgetStack(manager.widgets);

      if (mounted) {
        Navigator.pop(context);
      }
    }
  }

  Map<String, dynamic> _getInitialData(WidgetType type) {
    switch (type) {
      case WidgetType.counter:
        return {'count': 0};
      case WidgetType.todoList:
        return {
          'todos': [
            {'title': 'First todo', 'completed': false},
          ]
        };
      case WidgetType.weather:
        return {
          'temperature': '72',
          'condition': 'Sunny',
          'location': 'San Francisco',
        };
      case WidgetType.calendar:
        return {
          'events': [],
        };
      case WidgetType.notes:
        return {
          'note': '',
        };
      default:
        return {};
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Add Widget'),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            TextFormField(
              controller: _titleController,
              decoration: const InputDecoration(
                labelText: 'Widget Title',
                border: OutlineInputBorder(),
                hintText: 'e.g., My Counter',
              ),
              validator: (value) {
                if (value == null || value.isEmpty) {
                  return 'Please enter a title';
                }
                return null;
              },
            ),
            const SizedBox(height: 24),
            const Text(
              'Widget Type',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            ...WidgetType.values.map((type) {
              final config = WidgetConfig.configs[type];
              if (config == null) return const SizedBox.shrink();

              return Card(
                elevation: _selectedType == type ? 4 : 1,
                color: _selectedType == type
                    ? Theme.of(context).colorScheme.primaryContainer
                    : null,
                child: ListTile(
                  title: Text(
                    config.displayName,
                    style: TextStyle(
                      fontWeight: _selectedType == type
                          ? FontWeight.bold
                          : FontWeight.normal,
                    ),
                  ),
                  subtitle: Text(config.description),
                  leading: Radio<WidgetType>(
                    value: type,
                    groupValue: _selectedType,
                    onChanged: (value) {
                      if (value != null) {
                        setState(() => _selectedType = value);
                      }
                    },
                  ),
                  trailing: Icon(
                    _getIconForType(type),
                    color: _selectedType == type
                        ? Theme.of(context).colorScheme.primary
                        : null,
                  ),
                  onTap: () => setState(() => _selectedType = type),
                ),
              );
            }).toList(),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: _addWidget,
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.all(16),
              ),
              child: const Text(
                'Add Widget',
                style: TextStyle(fontSize: 18),
              ),
            ),
          ],
        ),
      ),
    );
  }

  IconData _getIconForType(WidgetType type) {
    switch (type) {
      case WidgetType.counter:
        return Icons.add_circle_outline;
      case WidgetType.todoList:
        return Icons.checklist;
      case WidgetType.weather:
        return Icons.wb_sunny;
      case WidgetType.calendar:
        return Icons.calendar_today;
      case WidgetType.notes:
        return Icons.note;
      default:
        return Icons.widgets;
    }
  }
}
