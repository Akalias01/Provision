import 'package:flutter/material.dart';
import '../models/widget_item.dart';

class CounterWidget extends StatelessWidget {
  final WidgetItem item;
  final VoidCallback? onIncrement;
  final VoidCallback? onDecrement;
  final VoidCallback? onTap;

  const CounterWidget({
    Key? key,
    required this.item,
    this.onIncrement,
    this.onDecrement,
    this.onTap,
  }) : super(key: key);

  int get _count => item.data['count'] as int? ?? 0;

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
                Colors.blue.shade400,
                Colors.blue.shade700,
              ],
            ),
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
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
              const SizedBox(height: 16),
              Center(
                child: Text(
                  '$_count',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 64,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  _buildButton(
                    icon: Icons.remove,
                    onPressed: onDecrement,
                  ),
                  _buildButton(
                    icon: Icons.add,
                    onPressed: onIncrement,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildButton({
    required IconData icon,
    required VoidCallback? onPressed,
  }) {
    return ElevatedButton(
      onPressed: onPressed,
      style: ElevatedButton.styleFrom(
        backgroundColor: Colors.white,
        foregroundColor: Colors.blue.shade700,
        shape: const CircleBorder(),
        padding: const EdgeInsets.all(16),
      ),
      child: Icon(icon, size: 24),
    );
  }
}
