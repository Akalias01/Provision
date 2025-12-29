import 'package:flutter/material.dart';
import '../models/widget_item.dart';

class TodoWidget extends StatelessWidget {
  final WidgetItem item;
  final Function(int)? onToggleTodo;
  final VoidCallback? onAddTodo;
  final VoidCallback? onTap;

  const TodoWidget({
    Key? key,
    required this.item,
    this.onToggleTodo,
    this.onAddTodo,
    this.onTap,
  }) : super(key: key);

  List<Map<String, dynamic>> get _todos {
    final todosData = item.data['todos'] as List?;
    if (todosData == null) return [];
    return todosData.map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  @override
  Widget build(BuildContext context) {
    final completedCount = _todos.where((t) => t['completed'] == true).length;

    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                Colors.purple.shade400,
                Colors.purple.shade700,
              ],
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    item.title,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: Colors.white.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      '#${item.stackOrder + 1}',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 12,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                '$completedCount / ${_todos.length} completed',
                style: TextStyle(
                  color: Colors.white.withOpacity(0.8),
                  fontSize: 14,
                ),
              ),
              const SizedBox(height: 16),
              Expanded(
                child: _todos.isEmpty
                    ? Center(
                        child: Text(
                          'No todos yet',
                          style: TextStyle(
                            color: Colors.white.withOpacity(0.6),
                            fontSize: 16,
                          ),
                        ),
                      )
                    : ListView.builder(
                        itemCount: _todos.length,
                        itemBuilder: (context, index) {
                          final todo = _todos[index];
                          return _buildTodoItem(todo, index);
                        },
                      ),
              ),
              const SizedBox(height: 8),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: onAddTodo,
                  icon: const Icon(Icons.add),
                  label: const Text('Add Todo'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.white,
                    foregroundColor: Colors.purple.shade700,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTodoItem(Map<String, dynamic> todo, int index) {
    final isCompleted = todo['completed'] as bool? ?? false;
    final title = todo['title'] as String? ?? '';

    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.1),
        borderRadius: BorderRadius.circular(8),
      ),
      child: CheckboxListTile(
        value: isCompleted,
        onChanged: (value) => onToggleTodo?.call(index),
        title: Text(
          title,
          style: TextStyle(
            color: Colors.white,
            decoration: isCompleted ? TextDecoration.lineThrough : null,
          ),
        ),
        controlAffinity: ListTileControlAffinity.leading,
        activeColor: Colors.white,
        checkColor: Colors.purple.shade700,
      ),
    );
  }
}
