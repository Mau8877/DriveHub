import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../pagos/providers/saldo_horas_provider.dart';
import '../models/reservas_resultado.dart';
import 'clases_service_provider.dart';
import 'franjas_disponibles_provider.dart';
import 'franjas_seleccionadas_provider.dart';

/// Confirmar las franjas elegidas (CU14). No guarda datos: devuelve el resultado y deja el resto de los providers
/// al día.
///
/// * Cada franja se reserva por separado: un resultado con rechazadas NO es un error, se devuelve igual.
/// * Con el resultado en mano (haya o no rechazadas) se vacía la selección y se refrescan las franjas, las clases y
/// * el saldo. Si la llamada falla (sin red, un 400) la selección se conserva para reintentar, pero igual se
/// * refrescan las clases y el saldo: el servidor pudo haber alcanzado a reservar antes de cortarse.
/// * Los errores se propagan al llamador: la pantalla decide cómo mostrarlos.
class ReservasNotifier extends Notifier<void> {
  @override
  void build() {}

  Future<ReservasResultado> confirmar() async {
    final codigos = ref.read(franjasSeleccionadasUiProvider).keys.toList();
    final ReservasResultado resultado;
    try {
      resultado = await ref.read(clasesServiceProvider).reservar(codigos);
    } catch (_) {
      ref.invalidate(reservasListProvider);
      ref.invalidate(saldoHorasProvider);
      rethrow;
    }
    ref.read(franjasSeleccionadasUiProvider.notifier).vaciar();
    ref.invalidate(franjasDisponiblesListProvider);
    ref.invalidate(instructoresDisponiblesListProvider);
    ref.invalidate(reservasListProvider);
    ref.invalidate(saldoHorasProvider);
    return resultado;
  }
}

final reservasProvider = NotifierProvider<ReservasNotifier, void>(ReservasNotifier.new);
