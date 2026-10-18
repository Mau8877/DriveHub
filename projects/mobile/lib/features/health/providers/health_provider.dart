import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../services/health_service.dart';

final healthServiceProvider = Provider<HealthService>((ref) => HealthService());

/// `autoDispose`: es un chequeo puntual (se dispara al entrar a la
/// pantalla), no un dato que tenga sentido mantener cacheado mientras nadie
/// la está mirando.
final healthCheckProvider = FutureProvider.autoDispose<Map<String, dynamic>>((ref) {
  return ref.watch(healthServiceProvider).checkHealth();
});
