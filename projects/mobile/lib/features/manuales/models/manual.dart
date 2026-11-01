/// Un manual publicado de la escuela (CU34): sus datos y dónde está su PDF.
///
/// * La app solo los lee; los publica el Administrador (CU32).
class Manual {
  final String codigo;
  final String titulo;
  final String? descripcion;

  /// `GENERAL` o la sigla de una categoría de licencia (`A`, `B`, `C`).
  final String categoria;
  final String? tema;
  final int version;
  final List<String> etiquetas;

  /// Dirección pública del PDF en el almacén de archivos.
  final String archivoUrl;
  final String? archivoNombre;
  final int? tamanoBytes;

  const Manual({
    required this.codigo,
    required this.titulo,
    this.descripcion,
    required this.categoria,
    this.tema,
    required this.version,
    this.etiquetas = const [],
    required this.archivoUrl,
    this.archivoNombre,
    this.tamanoBytes,
  });

  factory Manual.fromJson(Map<String, dynamic> json) {
    final archivo = json['archivo'] as Map<String, dynamic>;
    return Manual(
      codigo: json['codigo'] as String,
      titulo: json['titulo'] as String,
      descripcion: json['descripcion'] as String?,
      categoria: json['categoria'] as String,
      tema: json['tema'] as String?,
      version: json['version'] as int,
      etiquetas: (json['etiquetas'] as List<dynamic>? ?? const [])
          .map((e) => e as String)
          .toList(),
      archivoUrl: archivo['url'] as String,
      archivoNombre: archivo['nombre'] as String?,
      tamanoBytes: archivo['tamanoBytes'] as int?,
    );
  }
}
