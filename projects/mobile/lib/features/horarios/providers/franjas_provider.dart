import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/dia_bloqueado.dart';
import '../models/fechas.dart';
import '../models/franja.dart';
import 'horarios_service_provider.dart';

/// El lunes de la semana cuyas franjas se están viendo (estado puro de UI). La semana de hoy por defecto.
class FranjasSemanaNotifier extends Notifier<DateTime> {
  @override
  DateTime build() => lunesDeLaSemana(DateTime.now());

  void anterior() => state = sumarDias(state, -7);

  void siguiente() => state = sumarDias(state, 7);

  /// Va a la semana que contiene [fecha].
  void irA(DateTime fecha) => state = lunesDeLaSemana(fecha);
}

final franjasSemanaUiProvider = NotifierProvider<FranjasSemanaNotifier, DateTime>(FranjasSemanaNotifier.new);

/// Las franjas del instructor de una semana (del lunes dado al domingo): la página 1 de 100, que
/// alcanza para una semana.
final franjasListProvider = FutureProvider.autoDispose.family<List<Franja>, DateTime>((ref, lunes) async {
  final inicio = lunesDeLaSemana(lunes);
  final pagina = await ref
      .watch(horariosServiceProvider)
      .listarFranjas(desde: inicio, hasta: sumarDias(inicio, 6), pageSize: 100);
  return pagina.items;
});

/// Bloquear y desbloquear franjas (CU12), de una en una o un día completo. No guarda datos: al terminar
/// con éxito invalida la lista.
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

  Future<DiaBloqueado> bloquearDia(DateTime fecha) async {
    final dia = await ref.read(horariosServiceProvider).bloquearDia(soloFecha(fecha));
    ref.invalidate(franjasListProvider);
    return dia;
  }

  Future<DiaBloqueado> desbloquearDia(DateTime fecha) async {
    final dia = await ref.read(horariosServiceProvider).desbloquearDia(soloFecha(fecha));
    ref.invalidate(franjasListProvider);
    return dia;
  }
}

final franjasProvider = NotifierProvider<FranjasNotifier, void>(FranjasNotifier.new);
