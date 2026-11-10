import 'dart:io';

import 'package:dio/dio.dart';

/// Los archivos del asistente de voz (CU50) que no vienen dentro de la app: el modelo de acciones (FunctionGemma) y
/// Whisper. Ruta relativa a `<documentos>/ia/` y tamaño en bytes (para el progreso de la descarga).
///
/// * Las rutas son las que leen `rutaModeloDeAccionesProvider` y `carpetaDeWhisperProvider`.
const archivosDeModelosIa = <String, int>{
  'acciones.litertlm': 284203024,
  'whisper/tiny-encoder.int8.onnx': 12937772,
  'whisper/tiny-decoder.int8.onnx': 89855401,
  'whisper/tiny-tokens.txt': 816730,
};

/// Megabytes de la descarga completa, para avisarle a la persona antes de empezar.
final megasDeModelosIa = (archivosDeModelosIa.values.fold<int>(0, (a, b) => a + b) / (1024 * 1024)).round();

/// Si el teléfono tiene los modelos del asistente y, si no, cómo conseguirlos.
abstract interface class ModelosDeIa {
  /// Hay de dónde descargarlos (`MODELOS_IA_URL`). En desarrollo no: los archivos se copian con `adb push`.
  bool get descargables;

  /// Están todos los archivos.
  Future<bool> instalados();

  /// Descarga los que faltan. [alAvanzar] recibe el avance total, de 0 a 1.
  Future<void> descargar(void Function(double progreso) alAvanzar);
}

/// Sin dirección de descarga: el asistente se comporta como siempre (si falta un archivo, avisa que no está).
class SinDescargaDeModelos implements ModelosDeIa {
  const SinDescargaDeModelos();

  @override
  bool get descargables => false;

  @override
  Future<bool> instalados() async => true;

  @override
  Future<void> descargar(void Function(double progreso) alAvanzar) async {}
}

/// Descarga los modelos de [urlBase] (`<urlBase>/acciones.litertlm`, `<urlBase>/whisper/...`) a [carpeta].
///
/// * Usa su propio `Dio` y no el `ApiClient`: son archivos estáticos del almacenamiento, no la API (sin sobre, sin
///   Bearer y sin el modo offline, que no debe guardar 375 MB en su caché).
/// * Cada archivo se baja a `<nombre>.parte` y se renombra al terminar: un corte a mitad nunca deja un modelo roto
///   que parezca instalado.
class ModelosDeIaEnArchivos implements ModelosDeIa {
  final String carpeta;
  final String urlBase;
  final Dio _dio;

  ModelosDeIaEnArchivos({required this.carpeta, required this.urlBase, Dio? dio}) : _dio = dio ?? Dio();

  @override
  bool get descargables => true;

  File _archivo(String relativa) => File('$carpeta${Platform.pathSeparator}${relativa.replaceAll('/', Platform.pathSeparator)}');

  @override
  Future<bool> instalados() async {
    for (final relativa in archivosDeModelosIa.keys) {
      if (!await _archivo(relativa).exists()) return false;
    }
    return true;
  }

  @override
  Future<void> descargar(void Function(double progreso) alAvanzar) async {
    final total = archivosDeModelosIa.values.fold<int>(0, (a, b) => a + b);
    var listos = 0;
    final base = urlBase.endsWith('/') ? urlBase.substring(0, urlBase.length - 1) : urlBase;
    for (final MapEntry(key: relativa, value: tamano) in archivosDeModelosIa.entries) {
      final destino = _archivo(relativa);
      if (!await destino.exists()) {
        await destino.parent.create(recursive: true);
        final parte = File('${destino.path}.parte');
        await _dio.download(
          '$base/$relativa',
          parte.path,
          onReceiveProgress: (recibidos, _) => alAvanzar(((listos + recibidos) / total).clamp(0, 1).toDouble()),
        );
        await parte.rename(destino.path);
      }
      listos += tamano;
      alAvanzar(listos / total);
    }
  }
}
