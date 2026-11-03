import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/clase_agenda.dart';
import 'agenda_fecha_provider.dart';
import 'clases_service_provider.dart';

/// Las clases del instructor del día elegido (CU16): la página 1 de 50, en el orden del servidor (por hora).
///
/// * Cambiar de día ([agendaFechaUiProvider]) vuelve a pedir. Los errores (sin red, Identidad caída) quedan en el
/// * `AsyncValue` para que la pantalla ofrezca reintentar.
final agendaListProvider = FutureProvider.autoDispose<List<ClaseAgenda>>((ref) async {
  final fecha = ref.watch(agendaFechaUiProvider);
  final pagina = await ref.watch(clasesServiceProvider).agenda(fecha: fecha, pageSize: 50);
  return pagina.items;
});
