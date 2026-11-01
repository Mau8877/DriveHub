import 'dart:isolate';
import 'dart:typed_data';

import 'package:image/image.dart' as img;

/// Lado de la imagen que espera el modelo (cuadrada, 224 × 224).
const int ladoModelo = 224;

/// El lado mayor de la miniatura que se guarda en el historial.
const int ladoMiniatura = 512;

/// La imagen no se pudo leer (no es una imagen, o está dañada).
class ImagenInvalida implements Exception {
  const ImagenInvalida();
}

/// Una foto lista para el modelo y para el historial.
class ImagenProcesada {
  /// Los píxeles RGB (0 a 255) de la imagen de [ladoModelo] × [ladoModelo], fila por fila.
  final Float32List entrada;

  /// La miniatura JPEG para el historial.
  final Uint8List miniatura;

  const ImagenProcesada({required this.entrada, required this.miniatura});
}

/// Decodifica la foto, corrige su orientación, la lleva al tamaño del modelo y arma la miniatura.
///
/// * El modelo se entrenó con la imagen estirada a 224 × 224 (sin recortar), así que acá se hace igual.
/// * Una foto de cámara pesa varios MB: usa [procesarImagenAparte] para no congelar la pantalla.
ImagenProcesada procesarImagen(Uint8List bytes) {
  final original = img.decodeImage(bytes);
  if (original == null) throw const ImagenInvalida();
  final derecha = img.bakeOrientation(original);

  final cuadrada = img.copyResize(
    derecha,
    width: ladoModelo,
    height: ladoModelo,
  );
  final entrada = Float32List(ladoModelo * ladoModelo * 3);
  var i = 0;
  for (final pixel in cuadrada) {
    entrada[i++] = pixel.r.toDouble();
    entrada[i++] = pixel.g.toDouble();
    entrada[i++] = pixel.b.toDouble();
  }

  final mayor = derecha.width >= derecha.height;
  final chica = (mayor ? derecha.width : derecha.height) > ladoMiniatura
      ? img.copyResize(
          derecha,
          width: mayor ? ladoMiniatura : null,
          height: mayor ? null : ladoMiniatura,
        )
      : derecha;
  final miniatura = Uint8List.fromList(img.encodeJpg(chica, quality: 70));

  return ImagenProcesada(entrada: entrada, miniatura: miniatura);
}

/// [procesarImagen] en otro hilo, para que la pantalla siga fluida con fotos grandes.
Future<ImagenProcesada> procesarImagenAparte(Uint8List bytes) =>
    Isolate.run(() => procesarImagen(bytes));
