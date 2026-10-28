import 'tutorial.dart';

/// El PDF que el backend guardó al subirlo (CU33): el formulario lo manda después al crear o editar el tutorial.
class ArchivoSubido {
  final String url;
  final String mime;
  final int tamanoBytes;
  final String sha256;
  final String? nombre;

  const ArchivoSubido({
    required this.url,
    required this.mime,
    required this.tamanoBytes,
    required this.sha256,
    this.nombre,
  });

  factory ArchivoSubido.fromJson(Map<String, dynamic> json) => ArchivoSubido(
    url: json['url'] as String,
    mime: json['mime'] as String,
    tamanoBytes: json['tamanoBytes'] as int,
    sha256: json['sha256'] as String,
    nombre: json['nombre'] as String?,
  );

  Map<String, dynamic> toJson() => {
    'url': url,
    'mime': mime,
    'tamanoBytes': tamanoBytes,
    'sha256': sha256,
    'nombre': ?nombre,
  };
}

/// Lo que se envía para crear o editar un tutorial (CU33).
///
/// * Al crear van todos los campos; al editar también (el formulario siempre manda lo que muestra) salvo el recurso,
///   que solo se manda si cambió.
class TutorialRequest {
  final String titulo;
  final String? descripcion;
  final String categoria;
  final TipoTutorial tipo;
  final List<AudienciaTutorial> audiencia;
  final List<String> etiquetas;

  /// El enlace de YouTube nuevo, si [tipo] es [TipoTutorial.enlace].
  final String? enlace;

  /// El PDF nuevo ya subido, si [tipo] es [TipoTutorial.pdf].
  final ArchivoSubido? archivo;

  const TutorialRequest({
    required this.titulo,
    this.descripcion,
    required this.categoria,
    required this.tipo,
    required this.audiencia,
    this.etiquetas = const [],
    this.enlace,
    this.archivo,
  });

  /// El tipo y el recurso viajan juntos y solo si hay un recurso nuevo: al editar sin cambiar el PDF ni el enlace no se
  /// mandan y el tutorial conserva el que tiene.
  Map<String, dynamic> toJson() {
    final recurso = tipo == TipoTutorial.pdf
        ? archivo?.toJson()
        : (enlace == null ? null : {'url': enlace!.trim()});
    return {
      'titulo': titulo.trim(),
      'descripcion': (descripcion?.trim().isEmpty ?? true)
          ? null
          : descripcion!.trim(),
      'categoria': categoria,
      'audiencia': audiencia.map((a) => a.valorApi).toList(),
      'etiquetas': etiquetas,
      if (recurso != null) ...{'tipo': tipo.valorApi, 'recurso': recurso},
    };
  }
}
