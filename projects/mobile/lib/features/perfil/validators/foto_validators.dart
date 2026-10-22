import 'dart:typed_data';
import 'dart:ui' as ui;

/// Revisión previa de la foto en el teléfono (CU06): solo da feedback inmediato; el backend es el
/// que decide (revisa el contenido real del archivo). Debe coincidir con sus límites.
const int fotoMaxBytes = 5 * 1024 * 1024;
const int fotoMaxLadoPx = 4000;

const String mensajeFotoFormato = 'Solo se admiten imágenes JPG, PNG o WebP';
const String mensajeFotoPeso = 'La imagen no puede superar 5 MB';
const String mensajeFotoDimensiones =
    'La imagen no puede superar 4000×4000 píxeles';
const String mensajeFotoIlegible = 'No pudimos leer la imagen';

/// Tipo MIME según la extensión del archivo (`jpg`, `jpeg`, `png`, `webp`), o `null` si no es
/// uno de los admitidos.
String? tipoDeImagen(String nombre) {
  final punto = nombre.lastIndexOf('.');
  if (punto < 0) return null;
  return switch (nombre.substring(punto + 1).toLowerCase()) {
    'jpg' || 'jpeg' => 'image/jpeg',
    'png' => 'image/png',
    'webp' => 'image/webp',
    _ => null,
  };
}

/// Mensaje del primer problema de una foto ya medida, o `null` si puede enviarse.
String? validarFoto({
  required String nombre,
  required int bytes,
  required int ancho,
  required int alto,
}) {
  if (tipoDeImagen(nombre) == null) return mensajeFotoFormato;
  if (bytes > fotoMaxBytes) return mensajeFotoPeso;
  if (ancho > fotoMaxLadoPx || alto > fotoMaxLadoPx) {
    return mensajeFotoDimensiones;
  }
  return null;
}

/// Ancho y alto de la imagen, leídos con el decodificador de Flutter. Lanza si no se puede leer.
Future<({int ancho, int alto})> leerDimensiones(Uint8List bytes) async {
  final codec = await ui.instantiateImageCodec(bytes);
  try {
    final cuadro = await codec.getNextFrame();
    final imagen = cuadro.image;
    final dimensiones = (ancho: imagen.width, alto: imagen.height);
    imagen.dispose();
    return dimensiones;
  } finally {
    codec.dispose();
  }
}
