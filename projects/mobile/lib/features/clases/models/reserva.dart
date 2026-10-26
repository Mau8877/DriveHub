import '../../horarios/models/fechas.dart';
import 'estado_reserva.dart';

/// Una clase reservada por el estudiante (CU14).
class Reserva {
  final String codigo;
  final DateTime fecha;

  /// `HH:mm`.
  final String horaInicio;
  final String horaFin;
  final String instructorCodigo;
  final String instructorNombre;
  final EstadoReserva estado;

  /// La hora de fin ya pasó: una [EstadoReserva.confirmada] con [terminada] está «pendiente de cierre»
  /// (el instructor todavía no registró cómo fue, CU17).
  final bool terminada;

  const Reserva({
    required this.codigo,
    required this.fecha,
    required this.horaInicio,
    required this.horaFin,
    required this.instructorCodigo,
    required this.instructorNombre,
    required this.estado,
    required this.terminada,
  });

  factory Reserva.fromJson(Map<String, dynamic> json) {
    return Reserva(
      codigo: json['codigo'] as String,
      fecha: fechaDesdeApi(json['fecha'] as String),
      horaInicio: json['horaInicio'] as String,
      horaFin: json['horaFin'] as String,
      instructorCodigo: json['instructorCodigo'] as String,
      instructorNombre: json['instructorNombre'] as String,
      estado: EstadoReserva.fromJson(json['estado'] as String),
      terminada: json['terminada'] as bool,
    );
  }
}
