import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/fechas.dart';
import '../models/franja.dart';
import 'horarios_service_provider.dart';

/// La fecha cuyas franjas se están viendo (estado puro de UI). Hoy por defecto.
class FranjasFechaNotifier extends Notifier<DateTime> {
  @override
  DateTime build() => soloFecha(DateTime.now());

  void fijar(DateTime fecha) => state = soloFecha(fecha);
}

final franjasFechaUiProvider = NotifierProvider<FranjasFechaNotifier, DateTime>(FranjasFechaNotifier.new);

/// Las franjas del instructor en una fecha: la página 1 de 50 (un día tiene muy pocas).
final franjasListProvider = FutureProvider.autoDispose.family<List<Franja>, DateTime>((ref, fecha) async {
  final pagina = await ref.watch(horariosServiceProvider).listarFranjas(fecha: soloFecha(fecha), pageSize: 50);
  return pagina.items;
});

/// Bloquear y desbloquear franjas (CU12). No guarda datos: al terminar con éxito invalida las listas.
///
/// * Los errores (`ApiException` o de red) se propagan al llamador: la pantalla decide cómo mostrarlos.
class FranjasNotifier extends Notifier<void> {
  @override
  void build() {}

  Future<Franja> bloquear(String codigo) async {
    final franja = await ref.read(horariosServiceProvider).bloquearFranja(codigo);
    ref.invalidate(franjasListProvider);
    return franja;
  }

  Future<Franja> desbloquear(String codigo) async {
    final franja = await ref.read(horariosServiceProvider).desbloquearFranja(codigo);
    ref.invalidate(franjasListProvider);
    return franja;
  }
}

final franjasProvider = NotifierProvider<FranjasNotifier, void>(FranjasNotifier.new);
