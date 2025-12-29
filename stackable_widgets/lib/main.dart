import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'services/widget_manager.dart';
import 'screens/home_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final widgetManager = WidgetManager();
  await widgetManager.initialize();

  runApp(MyApp(widgetManager: widgetManager));
}

class MyApp extends StatelessWidget {
  final WidgetManager widgetManager;

  const MyApp({Key? key, required this.widgetManager}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider.value(
      value: widgetManager,
      child: MaterialApp(
        title: 'Stackable Widgets',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(
            seedColor: Colors.blue,
            brightness: Brightness.light,
          ),
          useMaterial3: true,
          cardTheme: CardTheme(
            elevation: 2,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
        ),
        darkTheme: ThemeData(
          colorScheme: ColorScheme.fromSeed(
            seedColor: Colors.blue,
            brightness: Brightness.dark,
          ),
          useMaterial3: true,
          cardTheme: CardTheme(
            elevation: 2,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
        ),
        home: const HomeScreen(),
      ),
    );
  }
}
