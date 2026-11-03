import '../../horarios/models/fechas.dart';
import 'asistencia_clase.dart';

/// Una clase ya dictada y cerrada por el instructor (CU17): cómo asistió el estudiante y lo que se registró.
class ClaseDictada {
  final String codigo;
  final DateTime fecha;
  final String estudianteCodigo;
  final String estudianteNombre;
  final AsistenciaClase asistencia;
  final String? tema;
  final String? observaciones;
  final int? kmInicio;
  final int? kmFin;

  const ClaseDictada({
    required this.codigo,
    required this.fecha,
    required this.estudianteCodigo,
    required this.estudianteNombre,
    required this.asistencia,
    this.tema,
    this.observaciones,
    this.kmInicio,
    this.kmFin,
  });

  factory ClaseDictada.fromJson(Map<String, dynamic> json) {
    return ClaseDictada(
      codigo: json['codigo'] as String,
      fecha: fechaDesdeApi(json['fecha'] as String),
      estudianteCodigo: json['estudianteCodigo'] as String,
      estudianteNombre: json['estudianteNombre'] as String,
      asistencia: AsistenciaClase.fromJson(json['asistencia'] as String),
      tema: json['tema'] as String?,
      observaciones: json['observaciones'] as String?,
      kmInicio: (json['kmInicio'] as num?)?.toInt(),
      kmFin: (json['kmFin'] as num?)?.toInt(),
    );
  }
}
