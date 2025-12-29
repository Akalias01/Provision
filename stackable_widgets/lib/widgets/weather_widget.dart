import 'package:flutter/material.dart';
import '../models/widget_item.dart';

class WeatherWidget extends StatelessWidget {
  final WidgetItem item;
  final VoidCallback? onRefresh;
  final VoidCallback? onTap;

  const WeatherWidget({
    Key? key,
    required this.item,
    this.onRefresh,
    this.onTap,
  }) : super(key: key);

  String get _temperature => item.data['temperature']?.toString() ?? '72';
  String get _condition => item.data['condition'] as String? ?? 'Sunny';
  String get _location => item.data['location'] as String? ?? 'San Francisco';

  IconData get _weatherIcon {
    switch (_condition.toLowerCase()) {
      case 'sunny':
        return Icons.wb_sunny;
      case 'cloudy':
        return Icons.cloud;
      case 'rainy':
        return Icons.water_drop;
      case 'snowy':
        return Icons.ac_unit;
      default:
        return Icons.wb_sunny;
    }
  }

  @override
  Widget build(BuildContext context) {
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
                Colors.orange.shade400,
                Colors.deepOrange.shade600,
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
                  Row(
                    children: [
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
                      const SizedBox(width: 8),
                      IconButton(
                        icon: const Icon(Icons.refresh, color: Colors.white),
                        onPressed: onRefresh,
                      ),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                _location,
                style: TextStyle(
                  color: Colors.white.withOpacity(0.9),
                  fontSize: 16,
                ),
              ),
              const Spacer(),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(
                    _weatherIcon,
                    size: 80,
                    color: Colors.white,
                  ),
                  const SizedBox(width: 24),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '$_temperature°',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 64,
                          fontWeight: FontWeight.bold,
                          height: 1,
                        ),
                      ),
                      Text(
                        _condition,
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 24,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
              const Spacer(),
            ],
          ),
        ),
      ),
    );
  }
}
