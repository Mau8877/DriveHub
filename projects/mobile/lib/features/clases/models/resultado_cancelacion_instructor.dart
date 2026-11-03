import 'clase_agenda.dart';
import 'hora_de_cancelacion.dart';

/// Cómo quedó una cancelación hecha por el instructor (CU16): la clase y qué pasó con la hora del estudiante.
///
/// * El instructor no tiene plazo: la hora siempre se devuelve ([HoraDeCancelacion.devuelta]) o queda en proceso
/// * si Pagos no respondió ([HoraDeCancelacion.enProceso]).
class ResultadoCancelacionInstructor {
  final ClaseAgenda clase;
  final HoraDeCancelacion horaDevuelta;

  const ResultadoCancelacionInstructor({required this.clase, required this.horaDevuelta});

  factory ResultadoCancelacionInstructor.fromJson(Map<String, dynamic> json) {
    return ResultadoCancelacionInstructor(
      clase: ClaseAgenda.fromJson(json['clase'] as Map<String, dynamic>),
      horaDevuelta: HoraDeCancelacion.fromJson(json['horaDevuelta'] as String),
    );
  }
}
