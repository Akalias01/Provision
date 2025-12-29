/// Model representing a stackable widget item
class WidgetItem {
  final String id;
  final String type;
  final String title;
  final Map<String, dynamic> data;
  final int stackOrder;
  final bool isInteractive;
  final DateTime lastUpdated;

  WidgetItem({
    required this.id,
    required this.type,
    required this.title,
    required this.data,
    this.stackOrder = 0,
    this.isInteractive = true,
    DateTime? lastUpdated,
  }) : lastUpdated = lastUpdated ?? DateTime.now();

  WidgetItem copyWith({
    String? id,
    String? type,
    String? title,
    Map<String, dynamic>? data,
    int? stackOrder,
    bool? isInteractive,
    DateTime? lastUpdated,
  }) {
    return WidgetItem(
      id: id ?? this.id,
      type: type ?? this.type,
      title: title ?? this.title,
      data: data ?? this.data,
      stackOrder: stackOrder ?? this.stackOrder,
      isInteractive: isInteractive ?? this.isInteractive,
      lastUpdated: lastUpdated ?? this.lastUpdated,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'type': type,
      'title': title,
      'data': data,
      'stackOrder': stackOrder,
      'isInteractive': isInteractive,
      'lastUpdated': lastUpdated.toIso8601String(),
    };
  }

  factory WidgetItem.fromJson(Map<String, dynamic> json) {
    return WidgetItem(
      id: json['id'] as String,
      type: json['type'] as String,
      title: json['title'] as String,
      data: Map<String, dynamic>.from(json['data'] as Map),
      stackOrder: json['stackOrder'] as int? ?? 0,
      isInteractive: json['isInteractive'] as bool? ?? true,
      lastUpdated: DateTime.parse(json['lastUpdated'] as String),
    );
  }
}
