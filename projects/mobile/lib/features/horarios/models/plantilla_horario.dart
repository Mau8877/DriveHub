import 'estado_plantilla.dart';
import 'fechas.dart';

/// Un horario semanal del instructor (CU12): nombre, días, ventana de horas, duración de cada clase,
/// vigencia y estado.
class PlantillaHorario {
  final String codigo;
  final String nombre;

  /// 1 (lunes) a 7 (domingo), sin repetir y ordenados.
  final List<int> diasSemana;

  /// `HH:mm`.
  final String horaInicio;
  final String horaFin;
  final int duracionClaseMin;
  final DateTime vigenteDesde;
  final DateTime? vigenteHasta;
  final EstadoPlantilla estado;

  const PlantillaHorario({
    required this.codigo,
    required this.nombre,
    required this.diasSemana,
    required this.horaInicio,
    required this.horaFin,
    required this.duracionClaseMin,
    required this.vigenteDesde,
    this.vigenteHasta,
    required this.estado,
  });

  factory PlantillaHorario.fromJson(Map<String, dynamic> json) {
    final hasta = json['vigenteHasta'] as String?;
    return PlantillaHorario(
      codigo: json['codigo'] as String,
      nombre: json['nombre'] as String,
      diasSemana: (json['diasSemana'] as List<dynamic>).cast<int>(),
      horaInicio: json['horaInicio'] as String,
      horaFin: json['horaFin'] as String,
      duracionClaseMin: json['duracionClaseMin'] as int,
      vigenteDesde: fechaDesdeApi(json['vigenteDesde'] as String),
      vigenteHasta: hasta == null ? null : fechaDesdeApi(hasta),
      estado: EstadoPlantilla.fromJson(json['estado'] as String),
    );
  }

  bool get activa => estado == EstadoPlantilla.activa;

  /// Cuántas franjas de una clase caben en la ventana de un día.
  int get franjasPorDia => franjasDeLaVentana(horaInicio, horaFin, duracionClaseMin);

  /// Franjas de una semana: las de un día por la cantidad de días del horario.
  int get franjasPorSemana => franjasPorDia * diasSemana.length;
}
