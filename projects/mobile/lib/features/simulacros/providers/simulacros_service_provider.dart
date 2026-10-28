import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../services/simulacros_service.dart';

/// Servicio de los simulacros del estudiante; es un provider aparte para poder sustituirlo en tests.
final simulacrosServiceProvider = Provider<SimulacrosService>(
  (ref) => SimulacrosService(),
);
