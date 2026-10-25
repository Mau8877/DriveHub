import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../services/pagos_service.dart';

/// Servicio de la compra de horas; es un provider aparte para poder sustituirlo en tests.
final pagosServiceProvider = Provider<PagosService>((ref) => PagosService());
