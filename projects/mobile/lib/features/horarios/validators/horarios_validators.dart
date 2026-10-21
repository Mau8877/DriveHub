// * Reglas de validación del formulario de un horario (CU12). Deben coincidir con las del backend
// * (PlantillaHorarioService.crear: ERR_ACA_01, ERR_ACA_02 y ERR_ACA_03). Mensajes en español.
// * Cada función devuelve el mensaje de error o `null` si el valor es válido, lista para usarse como
// * `validator` de un campo del formulario. `hoy` se recibe por parámetro para poder probar sin reloj.

import '../models/fechas.dart';

const int duracionMinima = 15;
const int duracionMaxima = 240;

final RegExp _formatoHora = RegExp(r'^([01]\d|2[0-3]):[0-5]\d$');

/// Minutos del día de una hora `HH:mm`, o `null` si no tiene ese formato.
int? _minutos(String? hora) {
  if (hora == null || !_formatoHora.hasMatch(hora)) return null;
  final partes = hora.split(':');
  return int.parse(partes[0]) * 60 + int.parse(partes[1]);
}

String? validarHoraInicio(String? horaInicio) {
  if (horaInicio == null || horaInicio.isEmpty) return 'La hora de inicio es obligatoria';
  return _minutos(horaInicio) == null ? 'La hora de inicio debe tener el formato HH:mm' : null;
}

/// La hora de fin es obligatoria y posterior a la de inicio (`ERR_ACA_01`).
String? validarHoraFin(String? horaInicio, String? horaFin) {
  if (horaFin == null || horaFin.isEmpty) return 'La hora de fin es obligatoria';
  final fin = _minutos(horaFin);
  if (fin == null) return 'La hora de fin debe tener el formato HH:mm';
  final inicio = _minutos(horaInicio);
  if (inicio != null && fin <= inicio) return 'La hora de fin debe ser posterior a la de inicio';
  return null;
}

/// Obligatoria, un número entero entre [duracionMinima] y [duracionMaxima] (`ERR_ACA_02`).
String? validarDuracion(String? valor) {
  if (valor == null || valor.trim().isEmpty) return 'La duración de la clase es obligatoria';
  final minutos = int.tryParse(valor.trim());
  if (minutos == null) return 'La duración debe ser un número entero de minutos';
  if (minutos < duracionMinima || minutos > duracionMaxima) {
    return 'La duración debe estar entre $duracionMinima y $duracionMaxima minutos';
  }
  return null;
}

/// El tramo entre inicio y fin es un múltiplo exacto de la duración (`ERR_ACA_02`).
///
/// * Solo se evalúa si las tres cosas son válidas por separado: si no, devuelve `null` para no
/// * repetir el mensaje que ya muestra otro campo.
String? validarVentana(String? horaInicio, String? horaFin, String? duracion) {
  final inicio = _minutos(horaInicio);
  final fin = _minutos(horaFin);
  final minutos = int.tryParse(duracion?.trim() ?? '');
  if (inicio == null || fin == null || minutos == null || fin <= inicio) return null;
  if (minutos < duracionMinima || minutos > duracionMaxima) return null;
  return (fin - inicio) % minutos == 0
      ? null
      : 'El tramo entre inicio y fin debe ser un múltiplo de la duración de la clase';
}

/// No puede ser anterior a hoy (`ERR_ACA_03`). Nulo se acepta: el backend toma hoy.
String? validarVigenteDesde(DateTime? desde, {required DateTime hoy}) {
  if (desde == null) return null;
  return soloFecha(desde).isBefore(soloFecha(hoy)) ? 'La fecha de inicio no puede ser anterior a hoy' : null;
}

/// Opcional; si está, no puede ser anterior a `desde` (o a hoy si no hay `desde`) (`ERR_ACA_03`).
String? validarVigenteHasta(DateTime? hasta, DateTime? desde, {required DateTime hoy}) {
  if (hasta == null) return null;
  final minimo = soloFecha(desde ?? hoy);
  return soloFecha(hasta).isBefore(minimo)
      ? 'La fecha en que termina el horario no puede ser anterior a la de inicio'
      : null;
}
