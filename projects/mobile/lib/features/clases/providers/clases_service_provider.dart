import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../services/clases_service.dart';

/// Servicio de las clases del estudiante; es un provider aparte para poder sustituirlo en tests.
final clasesServiceProvider = Provider<ClasesService>((ref) => ClasesService());
