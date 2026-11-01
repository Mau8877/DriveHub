/// Lo que se mide de la cabeza en un instante (CU39). Los ángulos son los de ML Kit, en grados.
class Medicion {
  /// Giro a los lados (`headEulerAngleY`): positivo hacia un lado y negativo hacia el otro.
  final double giro;

  /// Cabeceo (`headEulerAngleX`): positivo si la cabeza mira hacia arriba.
  final double cabeceo;

  /// Probabilidad de que los ojos estén abiertos (de 0 a 1), si el detector la da.
  final double? ojosAbiertos;

  const Medicion({
    required this.giro,
    required this.cabeceo,
    this.ojosAbiertos,
  });
}

/// Hacia dónde tiene que mirar la persona en un paso de la prueba (CU39).
enum TipoPaso { frente, espejoIzquierdo, espejoDerecho, retrovisor }

/// Un paso de la prueba: qué se le pide a la persona.
class PasoPrueba {
  final TipoPaso tipo;
  final String instruccion;

  const PasoPrueba(this.tipo, this.instruccion);
}

/// La secuencia de revisión antes de maniobrar: al frente, espejo izquierdo, al frente, espejo derecho y retrovisor.
const List<PasoPrueba> pasosDePrueba = [
  PasoPrueba(TipoPaso.frente, 'Mira al frente'),
  PasoPrueba(TipoPaso.espejoIzquierdo, 'Mira el espejo izquierdo'),
  PasoPrueba(TipoPaso.frente, 'Vuelve a mirar al frente'),
  PasoPrueba(TipoPaso.espejoDerecho, 'Mira el espejo derecho'),
  PasoPrueba(TipoPaso.retrovisor, 'Mira el espejo retrovisor'),
];

/// Cómo le fue a la persona en un paso.
class ResultadoPaso {
  final int orden;
  final PasoPrueba paso;
  final bool aprobado;

  /// Lo que tardó en lograrlo, o el tiempo que se le dio si no lo logró.
  final int tiempoMs;

  const ResultadoPaso({
    required this.orden,
    required this.paso,
    required this.aprobado,
    required this.tiempoMs,
  });
}

/// El resultado de una prueba terminada (CU39).
class ResultadoPrueba {
  final List<ResultadoPaso> pasos;
  final int duracionSeg;

  /// De 0 a 100: el porcentaje de pasos logrados.
  final double puntaje;
  final String retroalimentacion;

  /// Nombre y versión del modelo del teléfono que validó los pasos.
  final String modelo;

  const ResultadoPrueba({
    required this.pasos,
    required this.duracionSeg,
    required this.puntaje,
    required this.retroalimentacion,
    required this.modelo,
  });

  int get aprobados => pasos.where((p) => p.aprobado).length;
}
