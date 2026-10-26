import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../horarios/models/fechas.dart';
import '../models/franja_disponible.dart';
import '../models/instructor_disponible.dart';
import '../models/momento_dia.dart';
import '../models/reserva.dart';
import '../models/vista_reservas.dart';
import 'clases_service_provider.dart';

/// El día cuyas franjas se están viendo en «Reservar» (estado puro de UI). Hoy por defecto.
class ReservarFechaNotifier extends Notifier<DateTime> {
  @override
  DateTime build() => soloFecha(DateTime.now());

  void elegir(DateTime fecha) => state = soloFecha(fecha);
}

final reservarFechaUiProvider = NotifierProvider<ReservarFechaNotifier, DateTime>(ReservarFechaNotifier.new);

/// El momento del día elegido en «Reservar» (estado puro de UI). Todos por defecto.
class ReservarMomentoNotifier extends Notifier<MomentoDia> {
  @override
  MomentoDia build() => MomentoDia.todos;

  void elegir(MomentoDia momento) => state = momento;
}

final reservarMomentoUiProvider = NotifierProvider<ReservarMomentoNotifier, MomentoDia>(ReservarMomentoNotifier.new);

/// El instructor elegido en «Reservar» (estado puro de UI): su código, o `null` para todos.
///
/// * Lo que vale para filtrar es [reservarInstructorEfectivoProvider], no este valor crudo.
class ReservarInstructorNotifier extends Notifier<String?> {
  @override
  String? build() => null;

  void elegir(String? instructorCodigo) => state = instructorCodigo;
}

final reservarInstructorUiProvider = NotifierProvider<ReservarInstructorNotifier, String?>(
  ReservarInstructorNotifier.new,
);

/// Los instructores con alguna franja libre el día elegido, por nombre.
final instructoresDisponiblesListProvider = FutureProvider.autoDispose<List<InstructorDisponible>>((ref) {
  final fecha = ref.watch(reservarFechaUiProvider);
  return ref.watch(clasesServiceProvider).instructoresDisponibles(fecha: fecha);
});

/// El instructor por el que realmente se filtra: el elegido, siempre que siga teniendo franjas el día elegido; si
/// no, `null` (todos). Así, al cambiar de día, un instructor que ese día no atiende no deja la lista vacía sin
/// explicación.
final reservarInstructorEfectivoProvider = Provider.autoDispose<String?>((ref) {
  final elegido = ref.watch(reservarInstructorUiProvider);
  if (elegido == null) return null;
  return ref
      .watch(instructoresDisponiblesListProvider)
      .maybeWhen(data: (lista) => lista.any((i) => i.codigo == elegido) ? elegido : null, orElse: () => elegido);
});

/// Las franjas libres del día, momento e instructor elegidos: la página 1 de 100, que alcanza para un día.
///
/// * Las que ya empezaron según el reloj del teléfono no se muestran (el backend igual las rechazaría).
final franjasDisponiblesListProvider = FutureProvider.autoDispose<List<FranjaDisponible>>((ref) async {
  final fecha = ref.watch(reservarFechaUiProvider);
  final momento = ref.watch(reservarMomentoUiProvider);
  final instructor = ref.watch(reservarInstructorEfectivoProvider);
  final pagina = await ref
      .watch(clasesServiceProvider)
      .franjasDisponibles(fecha: fecha, momento: momento, instructorCodigo: instructor, pageSize: 100);
  final ahora = DateTime.now();
  return pagina.items.where((f) => !f.yaEmpezo(ahora)).toList();
});

/// Las clases del estudiante de una vista: la página 1 de 50.
final reservasListProvider = FutureProvider.autoDispose.family<List<Reserva>, VistaReservas>((ref, vista) async {
  final pagina = await ref.watch(clasesServiceProvider).reservas(vista: vista, pageSize: 50);
  return pagina.items;
});
