import '../../horarios/models/fechas.dart';

/// De qué evaluación viene una nota (CU23).
enum OrigenResultado {
  simulacro('SIMULACRO', 'Simulacro'),
  pruebaClase('PRUEBA_CLASE', 'Prueba de clase'),
  examenConduccion('EXAMEN_CONDUCCION', 'Examen de conducción');

  final String valorApi;
  final String etiqueta;
  const OrigenResultado(this.valorApi, this.etiqueta);

  static OrigenResultado fromJson(String valor) =>
      values.firstWhere((o) => o.valorApi == valor);
}

/// Una nota del historial del estudiante (CU23): simulacro, prueba de clase o examen de conducción.
class Resultado {
  final String codigo;
  final OrigenResultado origen;
  final String titulo;

  /// El nombre del estudiante: solo lo trae la vista del instructor.
  final String? estudianteNombre;

  /// De 0 a 100.
  final double puntaje;
  final bool aprobado;

  /// El día en la hora de la escuela.
  final DateTime dia;

  const Resultado({
    required this.codigo,
    required this.origen,
    required this.titulo,
    this.estudianteNombre,
    required this.puntaje,
    required this.aprobado,
    required this.dia,
  });

  factory Resultado.fromJson(Map<String, dynamic> json) {
    return Resultado(
      codigo: json['codigo'] as String,
      origen: OrigenResultado.fromJson(json['origen'] as String),
      titulo: json['titulo'] as String,
      estudianteNombre: json['estudianteNombre'] as String?,
      puntaje: (json['puntaje'] as num).toDouble(),
      aprobado: json['aprobado'] as bool,
      dia: fechaDesdeApi(json['dia'] as String),
    );
  }
}
