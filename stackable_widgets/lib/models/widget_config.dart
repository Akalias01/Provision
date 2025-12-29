/// Configuration for different widget types
enum WidgetType {
  counter,
  todoList,
  weather,
  calendar,
  notes,
  custom,
}

class WidgetConfig {
  final WidgetType type;
  final String displayName;
  final String description;
  final bool supportsStacking;
  final int maxStackSize;

  const WidgetConfig({
    required this.type,
    required this.displayName,
    required this.description,
    this.supportsStacking = true,
    this.maxStackSize = 10,
  });

  static const Map<WidgetType, WidgetConfig> configs = {
    WidgetType.counter: WidgetConfig(
      type: WidgetType.counter,
      displayName: 'Counter',
      description: 'Interactive counter widget',
      maxStackSize: 5,
    ),
    WidgetType.todoList: WidgetConfig(
      type: WidgetType.todoList,
      displayName: 'Todo List',
      description: 'Quick todo list widget',
      maxStackSize: 10,
    ),
    WidgetType.weather: WidgetConfig(
      type: WidgetType.weather,
      displayName: 'Weather',
      description: 'Current weather widget',
      maxStackSize: 3,
    ),
    WidgetType.calendar: WidgetConfig(
      type: WidgetType.calendar,
      displayName: 'Calendar',
      description: 'Calendar events widget',
      maxStackSize: 5,
    ),
    WidgetType.notes: WidgetConfig(
      type: WidgetType.notes,
      displayName: 'Quick Notes',
      description: 'Sticky notes widget',
      maxStackSize: 15,
    ),
  };
}
