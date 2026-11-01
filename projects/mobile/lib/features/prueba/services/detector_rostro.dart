import 'dart:async';

import 'package:camera/camera.dart';
import 'package:flutter/widgets.dart';
import 'package:google_mlkit_face_detection/google_mlkit_face_detection.dart';

import '../models/prueba.dart';

/// La persona no dio permiso para usar la cámara.
class PermisoCamaraDenegado implements Exception {
  const PermisoCamaraDenegado();
}

/// El teléfono no tiene cámara frontal o no se pudo abrir.
class CamaraNoDisponible implements Exception {
  const CamaraNoDisponible();
}

/// Mide la orientación de la cabeza con la cámara frontal (CU39). Todo corre en el teléfono: ningún cuadro se
/// guarda ni se envía.
abstract class DetectorRostro {
  /// Nombre y versión del modelo, para el historial.
  String get modelo;

  /// Una medición por cuadro procesado; `null` si en ese cuadro no se ve ningún rostro.
  Stream<Medicion?> get mediciones;

  /// Abre la cámara y empieza a medir. Lanza [PermisoCamaraDenegado] o [CamaraNoDisponible].
  Future<void> iniciar();

  Future<void> detener();

  /// La imagen de la cámara para mostrarle a la persona.
  Widget vistaPrevia(BuildContext context);
}

/// El detector real: la cámara frontal con el detector de rostros de ML Kit, que corre en el dispositivo.
///
/// * Procesa un cuadro a la vez: si el detector sigue ocupado, el cuadro nuevo se descarta.
/// * Solo Android (formato NV21) y con el teléfono en vertical; la pantalla de la prueba lo fija.
class DetectorRostroMlKit implements DetectorRostro {
  final _salida = StreamController<Medicion?>.broadcast();
  CameraController? _camara;
  FaceDetector? _detector;
  bool _procesando = false;

  @override
  String get modelo => 'mlkit-rostro-v1';

  @override
  Stream<Medicion?> get mediciones => _salida.stream;

  @override
  Future<void> iniciar() async {
    final List<CameraDescription> camaras;
    try {
      camaras = await availableCameras();
    } on CameraException {
      throw const CamaraNoDisponible();
    }
    final frontal = camaras.where(
      (c) => c.lensDirection == CameraLensDirection.front,
    );
    if (frontal.isEmpty) throw const CamaraNoDisponible();

    final camara = CameraController(
      frontal.first,
      ResolutionPreset.medium,
      enableAudio: false,
      imageFormatGroup: ImageFormatGroup.nv21,
    );
    try {
      await camara.initialize();
    } on CameraException catch (e) {
      await camara.dispose();
      throw e.code.toLowerCase().contains('permission')
          ? const PermisoCamaraDenegado()
          : const CamaraNoDisponible();
    }
    _camara = camara;
    _detector = FaceDetector(
      options: FaceDetectorOptions(
        enableClassification: true,
        performanceMode: FaceDetectorMode.fast,
      ),
    );
    await camara.startImageStream(_procesar);
  }

  Future<void> _procesar(CameraImage imagen) async {
    if (_procesando || _detector == null || _camara == null) return;
    _procesando = true;
    try {
      final entrada = _aEntrada(imagen, _camara!.description);
      if (entrada == null) return;
      final caras = await _detector!.processImage(entrada);
      if (_salida.isClosed) return;
      if (caras.isEmpty) {
        _salida.add(null);
        return;
      }
      // * Con varios rostros se toma el más grande: el de quien está frente al teléfono
      caras.sort(
        (a, b) => (b.boundingBox.width * b.boundingBox.height).compareTo(
          a.boundingBox.width * a.boundingBox.height,
        ),
      );
      final cara = caras.first;
      final ojos = [
        cara.leftEyeOpenProbability,
        cara.rightEyeOpenProbability,
      ].whereType<double>();
      _salida.add(
        Medicion(
          giro: cara.headEulerAngleY ?? 0,
          cabeceo: cara.headEulerAngleX ?? 0,
          ojosAbiertos: ojos.isEmpty
              ? null
              : ojos.reduce((a, b) => a + b) / ojos.length,
        ),
      );
    } catch (_) {
      if (!_salida.isClosed) _salida.add(null);
    } finally {
      _procesando = false;
    }
  }

  /// El cuadro de la cámara como entrada de ML Kit. Con el teléfono en vertical, la rotación es la de la cámara.
  InputImage? _aEntrada(CameraImage imagen, CameraDescription camara) {
    final rotacion = InputImageRotationValue.fromRawValue(
      camara.sensorOrientation,
    );
    final formato = InputImageFormatValue.fromRawValue(
      imagen.format.raw as int,
    );
    if (rotacion == null ||
        formato != InputImageFormat.nv21 ||
        imagen.planes.length != 1) {
      return null;
    }
    final plano = imagen.planes.first;
    return InputImage.fromBytes(
      bytes: plano.bytes,
      metadata: InputImageMetadata(
        size: Size(imagen.width.toDouble(), imagen.height.toDouble()),
        rotation: rotacion,
        format: formato!,
        bytesPerRow: plano.bytesPerRow,
      ),
    );
  }

  @override
  Future<void> detener() async {
    final camara = _camara;
    final detector = _detector;
    _camara = null;
    _detector = null;
    if (camara != null) {
      try {
        if (camara.value.isStreamingImages) await camara.stopImageStream();
      } catch (_) {
        // * Ya estaba parada
      }
      await camara.dispose();
    }
    await detector?.close();
  }

  @override
  Widget vistaPrevia(BuildContext context) {
    final camara = _camara;
    if (camara == null || !camara.value.isInitialized) {
      return const SizedBox.shrink();
    }
    return CameraPreview(camara);
  }
}
