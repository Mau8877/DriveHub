import 'tipo_pregunta.dart';

/// Un intento terminado con su corrección (CU21): cada pregunta con sus opciones correctas, lo que se eligió y la
/// explicación.
class IntentoResultado {
  final String codigo;
  final String simulacroNombre;

  /// `FINALIZADO` o `EXPIRADO` (se acabó el tiempo: 0 puntos).
  final String estado;
  final int aciertos;
  final int total;
  final double puntaje;
  final bool aprobado;
  final double puntajeAprobacion;
  final List<Correccion> preguntas;

  const IntentoResultado({
    required this.codigo,
    required this.simulacroNombre,
    required this.estado,
    required this.aciertos,
    required this.total,
    required this.puntaje,
    required this.aprobado,
    required this.puntajeAprobacion,
    required this.preguntas,
  });

  bool get expirado => estado == 'EXPIRADO';

  factory IntentoResultado.fromJson(Map<String, dynamic> json) {
    return IntentoResultado(
      codigo: json['codigo'] as String,
      simulacroNombre: json['simulacroNombre'] as String,
      estado: json['estado'] as String,
      aciertos: json['aciertos'] as int,
      total: json['total'] as int,
      puntaje: (json['puntaje'] as num).toDouble(),
      aprobado: json['aprobado'] as bool,
      puntajeAprobacion: (json['puntajeAprobacion'] as num).toDouble(),
      preguntas: (json['preguntas'] as List<dynamic>)
          .map((e) => Correccion.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

class Correccion {
  final String codigo;
  final String enunciado;
  final TipoPregunta tipo;
  final List<OpcionCorregida> opciones;

  /// Los ids de las opciones que marcó el estudiante (vacío si no respondió).
  final List<String> elegidas;
  final bool correcta;
  final String? explicacion;

  const Correccion({
    required this.codigo,
    required this.enunciado,
    required this.tipo,
    required this.opciones,
    required this.elegidas,
    required this.correcta,
    required this.explicacion,
  });

  factory Correccion.fromJson(Map<String, dynamic> json) {
    return Correccion(
      codigo: json['codigo'] as String,
      enunciado: json['enunciado'] as String,
      tipo: TipoPregunta.fromJson(json['tipo'] as String),
      opciones: (json['opciones'] as List<dynamic>)
          .map((e) => OpcionCorregida.fromJson(e as Map<String, dynamic>))
          .toList(),
      elegidas: (json['elegidas'] as List<dynamic>).cast<String>(),
      correcta: json['correcta'] as bool,
      explicacion: json['explicacion'] as String?,
    );
  }
}

class OpcionCorregida {
  final String id;
  final String texto;
  final bool correcta;

  const OpcionCorregida({
    required this.id,
    required this.texto,
    required this.correcta,
  });

  factory OpcionCorregida.fromJson(Map<String, dynamic> json) {
    return OpcionCorregida(
      id: json['id'] as String,
      texto: json['texto'] as String,
      correcta: json['correcta'] as bool,
    );
  }
}
