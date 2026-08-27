import 'package:flutter/material.dart';

import 'ui/home_screen.dart';

void main() {
  runApp(const NetraApp());
}

/// Netra (नेत्र — eye). A hidden-number vision game:
/// one generated plate, two ways to hide the number, ten questions a round.
class NetraApp extends StatelessWidget {
  const NetraApp({super.key});

  static const Color seed = Color(0xFF6FE3A8);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Netra — Hidden Number',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        brightness: Brightness.dark,
        colorScheme: ColorScheme.fromSeed(seedColor: seed, brightness: Brightness.dark),
        scaffoldBackgroundColor: const Color(0xFF07080D),
        appBarTheme: const AppBarTheme(backgroundColor: Colors.transparent, elevation: 0),
        filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
            minimumSize: const Size.fromHeight(52),
            textStyle: const TextStyle(fontWeight: FontWeight.w700, fontSize: 17),
          ),
        ),
      ),
      home: const HomeScreen(),
    );
  }
}
