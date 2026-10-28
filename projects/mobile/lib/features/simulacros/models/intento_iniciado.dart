import 'tipo_pregunta.dart';

/// Un intento recién iniciado (CU21): las preguntas sin la respuesta correcta.
class IntentoIniciado {
  final String codigo;
  final String simulacroNombre;
  final int duracionMin;
  final List<PreguntaParaRendir> preguntas;

  const IntentoIniciado({
    required this.codigo,
    required this.simulacroNombre,
    required this.duracionMin,
    required this.preguntas,
  });

  factory IntentoIniciado.fromJson(Map<String, dynamic> json) {
    return IntentoIniciado(
      codigo: json['codigo'] as String,
      simulacroNombre: json['simulacroNombre'] as String,
      duracionMin: json['duracionMin'] as int,
      preguntas: (json['preguntas'] as List<dynamic>)
          .map((e) => PreguntaParaRendir.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

class PreguntaParaRendir {
  final String codigo;
  final String enunciado;
  final TipoPregunta tipo;
  final List<OpcionParaRendir> opciones;

  const PreguntaParaRendir({
    required this.codigo,
    required this.enunciado,
    required this.tipo,
    required this.opciones,
  });

  factory PreguntaParaRendir.fromJson(Map<String, dynamic> json) {
    return PreguntaParaRendir(
      codigo: json['codigo'] as String,
      enunciado: json['enunciado'] as String,
      tipo: TipoPregunta.fromJson(json['tipo'] as String),
      opciones: (json['opciones'] as List<dynamic>)
          .map((e) => OpcionParaRendir.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

class OpcionParaRendir {
  /// La letra de la opción dentro de la pregunta (A, B, C...).
  final String id;
  final String texto;

  const OpcionParaRendir({required this.id, required this.texto});

  factory OpcionParaRendir.fromJson(Map<String, dynamic> json) {
    return OpcionParaRendir(
      id: json['id'] as String,
      texto: json['texto'] as String,
    );
  }
}
