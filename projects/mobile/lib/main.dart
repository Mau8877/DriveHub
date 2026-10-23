import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/network/api_client.dart';
import 'core/routing/app_router.dart';
import 'core/theme/app_theme.dart';
import 'core/theme/tema_provider.dart';
import 'features/auth/providers/sesion_provider.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await dotenv.load();

  // * Se crea el container antes de runApp para conectar el cliente HTTP con la
  // * sesión (Bearer y renovación ante un 401) y recuperar la sesión guardada.
  final container = ProviderContainer();
  final sesion = container.read(sesionProvider.notifier);
  ApiClient.configurarSesion(
    obtenerToken: sesion.obtenerToken,
    renovar: sesion.renovar,
  );
  unawaited(sesion.restaurar());

  runApp(UncontrolledProviderScope(container: container, child: const MyApp()));
}

class MyApp extends ConsumerWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return MaterialApp.router(
      debugShowCheckedModeBanner: false,
      title: 'DriveHub',
      theme: AppTheme.light,
      darkTheme: AppTheme.dark,
      themeMode: ref.watch(temaProvider),
      routerConfig: ref.watch(routerProvider),
    );
  }
}
