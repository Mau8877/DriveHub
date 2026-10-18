import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../providers/health_provider.dart';

/// Pantalla de diagnóstico -- prueba de punta a punta el camino completo
/// `service -> provider -> screen` contra el backend real: `ApiClient`
/// (Dio + interceptor), Riverpod (`FutureProvider` + `AsyncValue`) y el
/// desempaquetado de un error catalogado real. Sirve como referencia de
/// cómo se ve ese camino antes de que exista la primera feature de negocio.
class HealthCheckScreen extends ConsumerWidget {
  const HealthCheckScreen({super.key});

  Future<void> _probarRutaInexistente(BuildContext context, WidgetRef ref) async {
    final messenger = ScaffoldMessenger.of(context);
    try {
      await ref.read(healthServiceProvider).pingRutaInexistente();
      messenger.showSnackBar(
        const SnackBar(content: Text('Respondió 2xx (inesperado para una ruta inexistente)')),
      );
    } on DioException catch (error) {
      final apiException = error.error;
      final mensaje = apiException is ApiException
          ? 'ApiException: [${apiException.code}] ${apiException.message}'
          : 'DioException sin ApiException -- status ${error.response?.statusCode}, body: ${error.response?.data}';
      messenger.showSnackBar(SnackBar(content: Text(mensaje)));
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final health = ref.watch(healthCheckProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Diagnóstico de conexión')),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('GET /health (fuera de /api/v1)', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            health.when(
              loading: () => const Row(
                children: [
                  SizedBox(height: 16, width: 16, child: CircularProgressIndicator(strokeWidth: 2)),
                  SizedBox(width: 12),
                  Text('Consultando backend...'),
                ],
              ),
              error: (error, stackTrace) => Text(
                'No se pudo conectar: $error',
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
              data: (data) => Text(
                'Backend conectado -- status: ${data['status']}',
                style: TextStyle(color: Theme.of(context).colorScheme.primary),
              ),
            ),
            const SizedBox(height: 8),
            OutlinedButton(
              onPressed: () => ref.invalidate(healthCheckProvider),
              child: const Text('Reintentar'),
            ),
            const Divider(height: 40),
            Text(
              'GET /api/v1/ping-ruta-que-no-existe',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 4),
            Text(
              'Prueba que ApiClient desempaqueta un error real del backend en ApiException.',
              style: Theme.of(context).textTheme.bodySmall,
            ),
            const SizedBox(height: 12),
            FilledButton(
              onPressed: () => _probarRutaInexistente(context, ref),
              child: const Text('Probar'),
            ),
          ],
        ),
      ),
    );
  }
}
