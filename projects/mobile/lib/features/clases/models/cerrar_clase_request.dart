import 'asistencia_clase.dart';

/// Lo que el instructor registra al cerrar una clase que ya empezó (CU17).
///
/// * Los campos vacíos no viajan: el backend los toma como «sin dato».
class CerrarClaseRequest {
  final String reservaCodigo;
  final AsistenciaClase asistencia;
  final String? tema;
  final String? observaciones;
  final int? kmInicio;
  final int? kmFin;

  const CerrarClaseRequest({
    required this.reservaCodigo,
    required this.asistencia,
    this.tema,
    this.observaciones,
    this.kmInicio,
    this.kmFin,
  });

  Map<String, dynamic> toJson() => {
    'reservaCodigo': reservaCodigo,
    'asistencia': asistencia.valorApi,
    'tema': ?tema,
    'observaciones': ?observaciones,
    'kmInicio': ?kmInicio,
    'kmFin': ?kmFin,
  };
}
