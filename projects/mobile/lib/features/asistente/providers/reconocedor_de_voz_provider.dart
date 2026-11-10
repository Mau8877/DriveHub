import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';

import '../services/grabador_record.dart';
import '../services/reconocedor_de_voz.dart';
import '../services/transcriptor_whisper.dart';

/// Dónde busca la app los archivos de Whisper: `<documentos>/ia/whisper/`.
///
/// * Pesan ~100 MB y no se versionan. En desarrollo se copian con `adb push` a esa carpeta; en producción la app los
///   descarga la primera vez (`modelosDeIaProvider`, `MODELOS_IA_URL`).
final carpetaDeWhisperProvider = FutureProvider<String>((ref) async {
  final documentos = await getApplicationDocumentsDirectory();
  final s = Platform.pathSeparator;
  return '${documentos.path}${s}ia${s}whisper';
});

/// El reconocedor de voz del asistente (CU50). Un provider para poder sustituirlo en los tests por uno falso:
/// `flutter test` nunca abre el micrófono ni carga Whisper.
final reconocedorDeVozProvider = FutureProvider<ReconocedorDeVoz>((ref) async {
  final carpeta = await ref.watch(carpetaDeWhisperProvider.future);
  final grabador = GrabadorRecord();
  ref.onDispose(grabador.liberar);
  return ReconocedorDeVozPorGrabacion(grabador: grabador, transcriptor: TranscriptorWhisper(carpetaDelModelo: carpeta));
});
