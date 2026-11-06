import 'dart:typed_data';

import 'package:record/record.dart';

import 'reconocedor_de_voz.dart';

/// El [GrabadorDeAudio] real: el micrófono con el paquete `record`, en streaming (nada se escribe en disco).
///
/// * PCM de 16 bits, mono, a 16 kHz: lo que espera Whisper.
/// * Sin permiso de micrófono lanza [PermisoDeMicrofonoDenegado]; pedirlo la primera vez lo hace `hasPermission`.
class GrabadorRecord implements GrabadorDeAudio {
  final AudioRecorder _grabador;

  GrabadorRecord({AudioRecorder? grabador}) : _grabador = grabador ?? AudioRecorder();

  @override
  Future<Stream<Uint8List>> empezar() async {
    if (!await _grabador.hasPermission()) throw const PermisoDeMicrofonoDenegado();
    return _grabador.startStream(
      const RecordConfig(encoder: AudioEncoder.pcm16bits, sampleRate: frecuenciaDeVoz, numChannels: 1),
    );
  }

  @override
  Future<void> detener() async {
    if (await _grabador.isRecording()) await _grabador.stop();
  }

  @override
  Future<void> liberar() => _grabador.dispose();
}
