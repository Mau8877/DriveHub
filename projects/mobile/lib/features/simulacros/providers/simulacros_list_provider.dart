import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/intento_resultado.dart';
import '../models/intento_resumen.dart';
import '../models/simulacro_disponible.dart';
import 'simulacros_service_provider.dart';

/// Los simulacros que la escuela ofrece al estudiante (CU21). `autoDispose`: cada vez que se abre la sección se
/// vuelve a pedir, así uno desactivado desde el panel no sigue ofreciéndose.
final simulacrosDisponiblesProvider =
    FutureProvider.autoDispose<List<SimulacroDisponible>>(
      (ref) => ref.watch(simulacrosServiceProvider).disponibles(),
    );

/// El historial de intentos del estudiante (página 1 de 50).
final intentosHistorialProvider =
    FutureProvider.autoDispose<List<IntentoResumen>>((ref) async {
      final pagina = await ref.watch(simulacrosServiceProvider).historial();
      return pagina.items;
    });

/// La corrección de un intento terminado, por su código.
final intentoCorregidoProvider = FutureProvider.autoDispose
    .family<IntentoResultado, String>(
      (ref, codigo) => ref.watch(simulacrosServiceProvider).corregido(codigo),
    );
