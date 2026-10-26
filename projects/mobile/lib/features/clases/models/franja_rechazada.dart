import '../../horarios/models/fechas.dart';

/// Una franja que no se pudo reservar al confirmar (CU14), con el motivo para mostrárselo al estudiante.
///
/// * [fecha], [horaInicio] y [horaFin] son nulos cuando la franja ya no existe para el estudiante: no hay nada
/// * que mostrar de ella más que el motivo.
class FranjaRechazada {
  final String franjaCodigo;
  final DateTime? fecha;
  final String? horaInicio;
  final String? horaFin;
  final String? instructorNombre;

  /// El código catalogado del motivo (`ERR_ACA_10`, `ERR_ACA_17` a `ERR_ACA_20`).
  final String error;

  /// El motivo en español, tal como lo manda el backend.
  final String mensaje;

  const FranjaRechazada({
    required this.franjaCodigo,
    required this.fecha,
    required this.horaInicio,
    required this.horaFin,
    required this.instructorNombre,
    required this.error,
    required this.mensaje,
  });

  factory FranjaRechazada.fromJson(Map<String, dynamic> json) {
    final fecha = json['fecha'] as String?;
    return FranjaRechazada(
      franjaCodigo: json['franjaCodigo'] as String,
      fecha: fecha == null ? null : fechaDesdeApi(fecha),
      horaInicio: json['horaInicio'] as String?,
      horaFin: json['horaFin'] as String?,
      instructorNombre: json['instructorNombre'] as String?,
      error: json['error'] as String,
      mensaje: json['mensaje'] as String,
    );
  }

  /// La franja la tomó otro estudiante (o dejó de estar libre): `ERR_ACA_18`.
  bool get laTomoOtro => error == 'ERR_ACA_18';

  /// Pagos no respondió: vale la pena reintentar (`ERR_ACA_17`).
  bool get sePuedeReintentar => error == 'ERR_ACA_17';
}
