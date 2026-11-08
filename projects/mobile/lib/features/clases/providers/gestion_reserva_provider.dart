import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../pagos/providers/saldo_horas_provider.dart';
import '../models/reserva.dart';
import '../models/resultado_cancelacion.dart';
import 'clases_service_provider.dart';
import 'franjas_disponibles_provider.dart';

/// Cancelar y reprogramar una clase (CU15). No guarda datos: devuelve el resultado y deja las listas al día.
///
/// * Cancelar libera la franja y puede devolver la hora: refresca las clases, las franjas y el saldo.
/// * Reprogramar mueve la clase sin tocar las horas: refresca las clases y las franjas, NO el saldo.
/// * Si la llamada falla (sin red, un 4xx) igual se refrescan las clases y las franjas: el servidor pudo haber
///   alcanzado a cambiar algo antes de cortarse. Los errores se propagan: la pantalla decide cómo mostrarlos.
class GestionReservaNotifier extends Notifier<void> {
  @override
  void build() {}

  Future<ResultadoCancelacion> cancelar(String codigoReserva, {String? descripcion}) async {
    final ResultadoCancelacion resultado;
    try {
      resultado = await ref.read(clasesServiceProvider).cancelar(codigoReserva, descripcion: descripcion);
    } catch (_) {
      // * El saldo también: si la cancelación llegó a hacerse, la hora pudo haberse devuelto
      _refrescarClasesYFranjas();
      ref.invalidate(saldoHorasProvider);
      rethrow;
    }
    _refrescarClasesYFranjas();
    ref.invalidate(saldoHorasProvider);
    return resultado;
  }

  Future<Reserva> reprogramar(String codigoReserva, String franjaCodigo, {String? descripcion}) async {
    final Reserva reserva;
    try {
      reserva = await ref.read(clasesServiceProvider).reprogramar(codigoReserva, franjaCodigo, descripcion: descripcion);
    } catch (_) {
      _refrescarClasesYFranjas();
      rethrow;
    }
    _refrescarClasesYFranjas();
    return reserva;
  }

  void _refrescarClasesYFranjas() {
    ref.invalidate(reservasListProvider);
    ref.invalidate(franjasDisponiblesListProvider);
    ref.invalidate(instructoresDisponiblesListProvider);
  }
}

final gestionReservaProvider = NotifierProvider<GestionReservaNotifier, void>(GestionReservaNotifier.new);
