import 'plantilla_horario.dart';

/// Respuesta de crear un horario: el horario y cuántas franjas se generaron con él.
class PlantillaHorarioCreada {
  final PlantillaHorario horario;
  final int franjasGeneradas;

  const PlantillaHorarioCreada({required this.horario, required this.franjasGeneradas});

  /// El JSON trae los campos del horario más `franjasGeneradas`, en el mismo nivel.
  factory PlantillaHorarioCreada.fromJson(Map<String, dynamic> json) {
    return PlantillaHorarioCreada(
      horario: PlantillaHorario.fromJson(json),
      franjasGeneradas: json['franjasGeneradas'] as int,
    );
  }
}
