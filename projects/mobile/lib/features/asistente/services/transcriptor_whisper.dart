import 'dart:io';
import 'dart:isolate';
import 'dart:typed_data';

import 'package:sherpa_onnx/sherpa_onnx.dart' as sherpa;

import 'reconocedor_de_voz.dart';

/// Los archivos de Whisper tiny multilingüe (cuantizado a int8) que se buscan en la carpeta del modelo, con los
/// nombres con los que los publica sherpa-onnx.
const archivosDeWhisper = (
  codificador: 'tiny-encoder.int8.onnx',
  decodificador: 'tiny-decoder.int8.onnx',
  vocabulario: 'tiny-tokens.txt',
);

/// El [TranscriptorDeVoz] real: Whisper tiny con `sherpa_onnx`, en español, dentro del teléfono y en CPU.
///
/// * Transcribe en otro hilo (`Isolate.run`): cargar el modelo y decodificar tarda y no debe congelar la pantalla.
/// * El reconocedor se crea y se libera en cada frase: así no queda el modelo en memoria entre una frase y otra.
///   Si hace falta más velocidad, se mantiene un hilo vivo (mejora futura, no de esta UT).
/// * Sin los archivos del modelo lanza [ModeloDeVozNoDisponible] antes de arrancar nada.
class TranscriptorWhisper implements TranscriptorDeVoz {
  final String _carpetaDelModelo;

  TranscriptorWhisper({required this._carpetaDelModelo});

  @override
  Future<String> transcribir(Float32List muestras, int frecuencia) async {
    final rutas = _rutas();
    for (final ruta in [rutas.codificador, rutas.decodificador, rutas.vocabulario]) {
      if (!File(ruta).existsSync()) throw ModeloDeVozNoDisponible('Falta $ruta');
    }
    try {
      return await Isolate.run(() => _transcribirAparte(rutas, muestras, frecuencia));
    } catch (e) {
      throw ModeloDeVozNoDisponible('No se pudo transcribir: $e');
    }
  }

  ({String codificador, String decodificador, String vocabulario}) _rutas() {
    final base = _carpetaDelModelo.endsWith(Platform.pathSeparator)
        ? _carpetaDelModelo
        : '$_carpetaDelModelo${Platform.pathSeparator}';
    return (
      codificador: '$base${archivosDeWhisper.codificador}',
      decodificador: '$base${archivosDeWhisper.decodificador}',
      vocabulario: '$base${archivosDeWhisper.vocabulario}',
    );
  }
}

// * Corre en otro hilo: cada hilo carga sus propios enlaces nativos de sherpa-onnx
String _transcribirAparte(
  ({String codificador, String decodificador, String vocabulario}) rutas,
  Float32List muestras,
  int frecuencia,
) {
  sherpa.initBindings();
  final reconocedor = sherpa.OfflineRecognizer(
    sherpa.OfflineRecognizerConfig(
      model: sherpa.OfflineModelConfig(
        whisper: sherpa.OfflineWhisperModelConfig(
          encoder: rutas.codificador,
          decoder: rutas.decodificador,
          language: 'es',
          task: 'transcribe',
        ),
        tokens: rutas.vocabulario,
        numThreads: 2,
        debug: false,
        modelType: 'whisper',
      ),
    ),
  );
  final flujo = reconocedor.createStream();
  try {
    flujo.acceptWaveform(samples: muestras, sampleRate: frecuencia);
    reconocedor.decode(flujo);
    return reconocedor.getResult(flujo).text;
  } finally {
    flujo.free();
    reconocedor.free();
  }
}
