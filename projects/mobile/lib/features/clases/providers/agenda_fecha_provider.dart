import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../horarios/models/fechas.dart';

/// El día de la agenda del instructor que se está mirando (CU16, estado puro de UI).
///
/// * Arranca en hoy y siempre guarda solo la fecha, sin hora: así el día sirve de clave y no se pide dos veces el
/// * mismo día por unos minutos de diferencia.
class AgendaFechaNotifier extends Notifier<DateTime> {
  @override
  DateTime build() => soloFecha(DateTime.now());

  void anterior() => state = sumarDias(state, -1);

  void siguiente() => state = sumarDias(state, 1);

  void hoy() => state = soloFecha(DateTime.now());

  void elegir(DateTime fecha) => state = soloFecha(fecha);
}

final agendaFechaUiProvider = NotifierProvider<AgendaFechaNotifier, DateTime>(AgendaFechaNotifier.new);
