import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/theme/app_theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await dotenv.load();
  runApp(const ProviderScope(child: MyApp()));
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    // * Sin `core/routing/` todavía (ver NAVEGACION_MOBILE.md) -- se usa
    // * MaterialApp estándar con un `home` placeholder en vez de
    // * MaterialApp.router, para no forzar una decisión de navegación que
    // * le corresponde a cada proyecto generado a partir de este template.
    return MaterialApp(
      title: 'App',
      theme: AppTheme.light,
      darkTheme: AppTheme.dark,
      home: const _HomePlaceholder(),
    );
  }
}

class _HomePlaceholder extends StatelessWidget {
  const _HomePlaceholder();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Text('App', style: Theme.of(context).textTheme.headlineMedium),
      ),
    );
  }
}
