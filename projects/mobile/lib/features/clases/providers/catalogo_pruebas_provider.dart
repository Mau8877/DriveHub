import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/catalogo_pruebas.dart';
import 'clases_service_provider.dart';

/// Los criterios de cada tipo de prueba de clase (CU18).
///
/// * Los criterios no cambian mientras la app está abierta: se piden una vez y se guardan (no es `autoDispose`).
///   Si falla (sin red), el error queda en el `AsyncValue` y al invalidarlo se vuelve a pedir.
final catalogoPruebasProvider = FutureProvider<CatalogoPruebas>((ref) {
  return ref.watch(clasesServiceProvider).catalogoDePruebas();
});
