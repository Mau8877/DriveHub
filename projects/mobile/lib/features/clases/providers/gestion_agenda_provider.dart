import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/resultado_cancelacion_instructor.dart';
import 'agenda_provider.dart';
import 'clases_service_provider.dart';

/// Cancelar una clase como instructor (CU16). No guarda datos: devuelve el resultado y deja la agenda al día.
///
/// * La agenda se refresca tanto si la cancelación sale bien como si la llamada falla (sin red, un 4xx): el
///   servidor pudo haber alcanzado a cancelar antes de cortarse. Los errores se propagan: la pantalla decide cómo
///   mostrarlos.
/// * No toca el saldo de horas: es del estudiante, no del instructor.
class GestionAgendaNotifier extends Notifier<void> {
  @override
  void build() {}

  Future<ResultadoCancelacionInstructor> cancelar(String codigoReserva, {String? descripcion}) async {
    final ResultadoCancelacionInstructor resultado;
    try {
      resultado = await ref.read(clasesServiceProvider).cancelarComoInstructor(codigoReserva, descripcion: descripcion);
    } catch (_) {
      ref.invalidate(agendaListProvider);
      rethrow;
    }
    ref.invalidate(agendaListProvider);
    return resultado;
  }
}

final gestionAgendaProvider = NotifierProvider<GestionAgendaNotifier, void>(GestionAgendaNotifier.new);
