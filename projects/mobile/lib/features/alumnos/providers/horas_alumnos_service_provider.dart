import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../services/horas_alumnos_service.dart';

/// Servicio de las horas de los alumnos; es un provider aparte para poder sustituirlo en tests.
final horasAlumnosServiceProvider = Provider<HorasAlumnosService>(
  (ref) => HorasAlumnosService(),
);
