import '../../horarios/models/fechas.dart';

/// Una franja libre de un instructor de la escuela que el estudiante puede reservar (CU14).
///
/// * Es la vista del estudiante; la franja del instructor, con su estado, es `horarios/models/franja.dart` (CU12).
/// * Reusa los formatos de fecha de `horarios/models/fechas.dart`: son los mismos del CU12 y la relación de negocio
/// * entre las dos features (el instructor ofrece, el estudiante reserva) justifica esa dependencia.
class FranjaDisponible {
  final String codigo;
  final DateTime fecha;

  /// `HH:mm`.
  final String horaInicio;
  final String horaFin;
  final String instructorCodigo;
  final String instructorNombre;

  const FranjaDisponible({
    required this.codigo,
    required this.fecha,
    required this.horaInicio,
    required this.horaFin,
    required this.instructorCodigo,
    required this.instructorNombre,
  });

  factory FranjaDisponible.fromJson(Map<String, dynamic> json) {
    return FranjaDisponible(
      codigo: json['codigo'] as String,
      fecha: fechaDesdeApi(json['fecha'] as String),
      horaInicio: json['horaInicio'] as String,
      horaFin: json['horaFin'] as String,
      instructorCodigo: json['instructorCodigo'] as String,
      instructorNombre: json['instructorNombre'] as String,
    );
  }

  /// La franja ya empezó o ya pasó (el inicio es igual o anterior a [ahora]).
  ///
  /// * Es solo para la interfaz: el backend es quien rechaza con `ERR_ACA_10`.
  bool yaEmpezo(DateTime ahora) {
    final partes = horaInicio.split(':');
    final inicio = DateTime(fecha.year, fecha.month, fecha.day, int.parse(partes[0]), int.parse(partes[1]));
    return !inicio.isAfter(ahora);
  }
}
