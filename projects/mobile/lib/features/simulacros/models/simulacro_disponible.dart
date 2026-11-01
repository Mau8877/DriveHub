/// Un simulacro escrito que el estudiante puede rendir (CU21).
class SimulacroDisponible {
  final String codigo;
  final String nombre;
  final String categoria;
  final int duracionMin;
  final int nroPreguntas;

  /// Porcentaje de aciertos para aprobar (1 a 100).
  final double puntajeAprobacion;

  const SimulacroDisponible({
    required this.codigo,
    required this.nombre,
    required this.categoria,
    required this.duracionMin,
    required this.nroPreguntas,
    required this.puntajeAprobacion,
  });

  factory SimulacroDisponible.fromJson(Map<String, dynamic> json) {
    return SimulacroDisponible(
      codigo: json['codigo'] as String,
      nombre: json['nombre'] as String,
      categoria: json['categoria'] as String,
      duracionMin: json['duracionMin'] as int,
      nroPreguntas: json['nroPreguntas'] as int,
      puntajeAprobacion: (json['puntajeAprobacion'] as num).toDouble(),
    );
  }
}
