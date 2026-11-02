import '../../horarios/models/fechas.dart';
import 'estado_reserva.dart';

/// Una clase de la agenda del instructor (CU16): quién la toma, cuándo y en qué estado está.
class ClaseAgenda {
  final String codigo;
  final DateTime fecha;

  /// `HH:mm`.
  final String horaInicio;
  final String horaFin;
  final String estudianteCodigo;
  final String estudianteNombre;
  final EstadoReserva estado;

  /// La hora de fin ya pasó: una [EstadoReserva.confirmada] con [terminada] está «pendiente de cierre»
  /// (el instructor todavía no registró cómo fue, CU17).
  final bool terminada;

  /// Se puede cancelar: está confirmada y todavía no empezó. Lo decide el servidor con su reloj; un backend que
  /// no lo manda se lee como `false`.
  final bool cancelable;

  const ClaseAgenda({
    required this.codigo,
    required this.fecha,
    required this.horaInicio,
    required this.horaFin,
    required this.estudianteCodigo,
    required this.estudianteNombre,
    required this.estado,
    this.terminada = false,
    this.cancelable = false,
  });

  factory ClaseAgenda.fromJson(Map<String, dynamic> json) {
    return ClaseAgenda(
      codigo: json['codigo'] as String,
      fecha: fechaDesdeApi(json['fecha'] as String),
      horaInicio: json['horaInicio'] as String,
      horaFin: json['horaFin'] as String,
      estudianteCodigo: json['estudianteCodigo'] as String,
      estudianteNombre: json['estudianteNombre'] as String,
      estado: EstadoReserva.fromJson(json['estado'] as String),
      terminada: json['terminada'] as bool? ?? false,
      cancelable: json['cancelable'] as bool? ?? false,
    );
  }
}
