import 'fechas.dart';

/// Cuerpo de `POST /plantillas-horario` (crear un horario, CU12).
///
/// * `vigenteDesde` nulo = hoy (lo decide el backend). Los campos nulos no se envían.
class CrearPlantillaHorarioRequest {
  final String nombre;

  /// 1 (lunes) a 7 (domingo).
  final List<int> diasSemana;

  /// `HH:mm`.
  final String horaInicio;
  final String horaFin;
  final int duracionClaseMin;
  final DateTime? vigenteDesde;
  final DateTime? vigenteHasta;

  const CrearPlantillaHorarioRequest({
    required this.nombre,
    required this.diasSemana,
    required this.horaInicio,
    required this.horaFin,
    required this.duracionClaseMin,
    this.vigenteDesde,
    this.vigenteHasta,
  });

  Map<String, dynamic> toJson() => {
    'nombre': nombre,
    'diasSemana': diasSemana,
    'horaInicio': horaInicio,
    'horaFin': horaFin,
    'duracionClaseMin': duracionClaseMin,
    if (vigenteDesde != null) 'vigenteDesde': fechaParaApi(vigenteDesde!),
    if (vigenteHasta != null) 'vigenteHasta': fechaParaApi(vigenteHasta!),
  };
}
