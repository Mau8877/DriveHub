/// El tipo de recurso de un tutorial (CU33): un PDF subido o un enlace de YouTube.
enum TipoTutorial {
  pdf('PDF', 'PDF'),
  enlace('ENLACE', 'Video');

  final String valorApi;
  final String etiqueta;
  const TipoTutorial(this.valorApi, this.etiqueta);

  static TipoTutorial fromJson(String valor) =>
      values.firstWhere((t) => t.valorApi == valor, orElse: () => enlace);
}

/// El estado de publicación de un tutorial (CU33).
enum EstadoTutorial {
  borrador('BORRADOR', 'Borrador'),
  publicado('PUBLICADO', 'Publicado'),
  archivado('ARCHIVADO', 'Archivado');

  final String valorApi;
  final String etiqueta;
  const EstadoTutorial(this.valorApi, this.etiqueta);

  static EstadoTutorial fromJson(String valor) =>
      values.firstWhere((e) => e.valorApi == valor);
}

/// A quién va dirigido un tutorial (CU33): puede ser a ambos.
enum AudienciaTutorial {
  estudiante('ESTUDIANTE', 'Estudiantes'),
  instructor('INSTRUCTOR', 'Instructores');

  final String valorApi;
  final String etiqueta;
  const AudienciaTutorial(this.valorApi, this.etiqueta);

  static AudienciaTutorial fromJson(String valor) =>
      values.firstWhere((a) => a.valorApi == valor);
}

/// Un tutorial de un instructor (CU33 y CU35): sus datos y su recurso.
class Tutorial {
  final String codigo;
  final String titulo;
  final String? descripcion;

  /// `GENERAL` o la sigla de una categoría de licencia (`A`, `B`, `C`).
  final String categoria;
  final TipoTutorial tipo;
  final EstadoTutorial estado;
  final List<AudienciaTutorial> audiencia;
  final List<String> etiquetas;

  /// La dirección del PDF en el almacén de archivos, o el enlace de YouTube.
  final String recursoUrl;
  final String? recursoNombre;
  final int? tamanoBytes;

  const Tutorial({
    required this.codigo,
    required this.titulo,
    this.descripcion,
    required this.categoria,
    required this.tipo,
    required this.estado,
    required this.audiencia,
    this.etiquetas = const [],
    required this.recursoUrl,
    this.recursoNombre,
    this.tamanoBytes,
  });

  factory Tutorial.fromJson(Map<String, dynamic> json) {
    final recurso = json['recurso'] as Map<String, dynamic>;
    return Tutorial(
      codigo: json['codigo'] as String,
      titulo: json['titulo'] as String,
      descripcion: json['descripcion'] as String?,
      categoria: json['categoria'] as String,
      tipo: TipoTutorial.fromJson(json['tipo'] as String),
      estado: EstadoTutorial.fromJson(json['estado'] as String),
      audiencia: (json['audiencia'] as List<dynamic>)
          .map((a) => AudienciaTutorial.fromJson(a as String))
          .toList(),
      etiquetas: (json['etiquetas'] as List<dynamic>? ?? const [])
          .map((e) => e as String)
          .toList(),
      recursoUrl: recurso['url'] as String,
      recursoNombre: recurso['nombre'] as String?,
      tamanoBytes: recurso['tamanoBytes'] as int?,
    );
  }
}
