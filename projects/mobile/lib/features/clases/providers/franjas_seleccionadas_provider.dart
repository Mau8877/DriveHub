import 'dart:math' as math;

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../pagos/providers/saldo_horas_provider.dart';
import '../models/franja_disponible.dart';

/// Cuántas franjas se pueden confirmar de una vez: el mismo tope que el backend (`app.reservas.maximo-por-confirmacion`).
const int maximoPorConfirmacion = 10;

/// Las franjas que el estudiante fue eligiendo en «Reservar», por código y en el orden en que las eligió (estado
/// puro de UI). Sobreviven al cambio de día, de momento y de instructor.
///
/// * Elegir no reserva ni gasta nada: solo marca. Las horas se gastan al confirmar.
class FranjasSeleccionadasNotifier extends Notifier<Map<String, FranjaDisponible>> {
  @override
  Map<String, FranjaDisponible> build() => const {};

  /// Elige la franja o, si ya estaba elegida, la quita. No deja elegir más franjas que [horasDisponibles] ni más
  /// de [maximoPorConfirmacion]. Devuelve si pudo hacerlo.
  bool alternar(FranjaDisponible franja, {required int horasDisponibles}) {
    if (state.containsKey(franja.codigo)) {
      quitar(franja.codigo);
      return true;
    }
    if (state.length >= math.min(horasDisponibles, maximoPorConfirmacion)) return false;
    state = {...state, franja.codigo: franja};
    return true;
  }

  void quitar(String codigo) {
    if (!state.containsKey(codigo)) return;
    state = {...state}..remove(codigo);
  }

  void vaciar() {
    if (state.isNotEmpty) state = const {};
  }
}

final franjasSeleccionadasUiProvider =
    NotifierProvider<FranjasSeleccionadasNotifier, Map<String, FranjaDisponible>>(FranjasSeleccionadasNotifier.new);

/// Las horas que le quedarían al estudiante con las franjas elegidas: su saldo menos una hora por franja, sin bajar
/// de cero. Es solo la vista previa del indicador; el saldo real lo informa Pagos al confirmar.
final horasQueQuedanProvider = Provider.autoDispose<AsyncValue<int>>((ref) {
  final elegidas = ref.watch(franjasSeleccionadasUiProvider).length;
  return ref.watch(saldoHorasProvider).whenData((saldo) => math.max(0, saldo - elegidas));
});
