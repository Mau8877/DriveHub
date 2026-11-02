import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/clase_dictada.dart';
import 'clases_service_provider.dart';

/// Una clase ya cerrada (CU17), por su código: lo guardado, para corregirlo.
///
/// * Los errores (sin red, `ERR_ACA_35`) quedan en el `AsyncValue` para que la hoja de editar ofrezca reintentar.
final claseDictadaDetailProvider = FutureProvider.autoDispose.family<ClaseDictada, String>((ref, codigo) {
  return ref.watch(clasesServiceProvider).claseDictada(codigo);
});
