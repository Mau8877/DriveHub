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

  /// Se puede cerrar: está confirmada y ya empezó (CU17). Es el complemento de [cancelable]. Lo decide el servidor;
  /// un backend que no lo manda se lee como `false`.
  final bool cerrable;

  /// El código de la clase dictada, solo si la clase ya se cerró (CU17): con él se abre para corregirla.
  final String? claseCodigo;

  /// Se puede evaluar: la clase está cerrada con asistencia y le falta algún tipo de prueba (CU18). Lo decide el
  /// servidor; un backend que no lo manda se lee como `false`.
  final bool evaluable;

  /// La clase ya tiene al menos una prueba (CU18): con ella se ven sus resultados. Lo decide el servidor.
  final bool conPrueba;

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
    this.cerrable = false,
    this.claseCodigo,
    this.evaluable = false,
    this.conPrueba = false,
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
      cerrable: json['cerrable'] as bool? ?? false,
      claseCodigo: json['claseCodigo'] as String?,
      evaluable: json['evaluable'] as bool? ?? false,
      conPrueba: json['conPrueba'] as bool? ?? false,
    );
  }
}
