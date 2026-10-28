/// Gravedad de una luz del tablero (CU40). Los valores son los del backend.
enum SeveridadTablero {
  informativa('INFORMATIVA', 'Informativa'),
  advertencia('ADVERTENCIA', 'Advertencia'),
  critica('CRITICA', 'Crítica');

  final String valorApi;
  final String etiqueta;
  const SeveridadTablero(this.valorApi, this.etiqueta);

  static SeveridadTablero fromJson(String valor) =>
      values.firstWhere((s) => s.valorApi == valor);
}

/// Una luz del catálogo local (`assets/ml/luces_tablero.json`): qué significa y qué hacer (CU40).
class LuzTablero {
  final String codigo;
  final String nombre;
  final SeveridadTablero severidad;
  final String significado;
  final String queHacer;

  const LuzTablero({
    required this.codigo,
    required this.nombre,
    required this.severidad,
    required this.significado,
    required this.queHacer,
  });

  factory LuzTablero.fromJson(Map<String, dynamic> json) => LuzTablero(
    codigo: json['codigo'] as String,
    nombre: json['nombre'] as String,
    severidad: SeveridadTablero.fromJson(json['severidad'] as String),
    significado: json['significado'] as String,
    queHacer: json['queHacer'] as String,
  );

  /// La explicación que se muestra y se guarda en el historial.
  String get explicacion => '$significado $queHacer';
}

/// El catálogo de luces y la confianza mínima para dar una luz por reconocida.
class CatalogoTablero {
  /// Por debajo de esta confianza la app no afirma nada y pide otra foto.
  final double umbralConfianza;
  final Map<String, LuzTablero> _luces;

  CatalogoTablero({
    required this.umbralConfianza,
    required List<LuzTablero> luces,
  }) : _luces = {for (final l in luces) l.codigo: l};

  factory CatalogoTablero.fromJson(Map<String, dynamic> json) =>
      CatalogoTablero(
        umbralConfianza: (json['umbralConfianza'] as num).toDouble(),
        luces: (json['luces'] as List<dynamic>)
            .map((l) => LuzTablero.fromJson(l as Map<String, dynamic>))
            .toList(),
      );

  LuzTablero? luz(String codigo) => _luces[codigo];

  int get cantidad => _luces.length;
}
