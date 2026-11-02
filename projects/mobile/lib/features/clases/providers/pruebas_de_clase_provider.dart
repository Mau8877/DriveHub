import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/prueba_clase.dart';
import 'clases_service_provider.dart';

/// Las pruebas de una clase (CU18), por el código de la clase dictada: lo ya evaluado.
///
/// * Los errores (sin red, `ERR_ACA_35`) quedan en el `AsyncValue` para que las hojas ofrezcan reintentar.
final pruebasDeClaseDetailProvider = FutureProvider.autoDispose.family<List<PruebaClase>, String>((ref, claseCodigo) {
  return ref.watch(clasesServiceProvider).pruebasDeClase(claseCodigo);
});
