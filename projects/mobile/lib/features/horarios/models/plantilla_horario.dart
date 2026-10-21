import 'fechas.dart';

/// Un horario semanal del instructor (CU12): día, ventana de horas, duración de cada clase y vigencia.
class PlantillaHorario {
  final String codigo;

  /// 1 (lunes) a 7 (domingo).
  final int diaSemana;

  /// `HH:mm`.
  final String horaInicio;
  final String horaFin;
  final int duracionClaseMin;
  final DateTime vigenteDesde;
  final DateTime? vigenteHasta;
  final bool activa;

  const PlantillaHorario({
    required this.codigo,
    required this.diaSemana,
    required this.horaInicio,
    required this.horaFin,
    required this.duracionClaseMin,
    required this.vigenteDesde,
    this.vigenteHasta,
    required this.activa,
  });

  factory PlantillaHorario.fromJson(Map<String, dynamic> json) {
    final hasta = json['vigenteHasta'] as String?;
    return PlantillaHorario(
      codigo: json['codigo'] as String,
      diaSemana: json['diaSemana'] as int,
      horaInicio: json['horaInicio'] as String,
      horaFin: json['horaFin'] as String,
      duracionClaseMin: json['duracionClaseMin'] as int,
      vigenteDesde: fechaDesdeApi(json['vigenteDesde'] as String),
      vigenteHasta: hasta == null ? null : fechaDesdeApi(hasta),
      activa: json['activa'] as bool,
    );
  }
}
