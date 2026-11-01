import 'hora_de_cancelacion.dart';
import 'reserva.dart';

/// Cómo quedó una cancelación (CU15): la clase cancelada, qué pasó con la hora y las horas que le quedan.
class ResultadoCancelacion {
  final Reserva reserva;
  final HoraDeCancelacion horaDevuelta;

  /// El saldo real tras cancelar; nulo si Pagos no respondió o la hora todavía está en proceso (la app lo vuelve a
  /// pedir).
  final int? horasDisponibles;

  const ResultadoCancelacion({required this.reserva, required this.horaDevuelta, required this.horasDisponibles});

  factory ResultadoCancelacion.fromJson(Map<String, dynamic> json) {
    return ResultadoCancelacion(
      reserva: Reserva.fromJson(json['reserva'] as Map<String, dynamic>),
      horaDevuelta: HoraDeCancelacion.fromJson(json['horaDevuelta'] as String),
      horasDisponibles: (json['horasDisponibles'] as num?)?.toInt(),
    );
  }
}
