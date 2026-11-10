import 'dart:io';

import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';

import '../services/modelos_de_ia.dart';

/// De dónde y adónde se descargan los modelos del asistente (CU50).
///
/// * `MODELOS_IA_URL` existe en `.env.produccion` (el bucket público de la nube). Sin esa variable (desarrollo y
///   tests) no se descarga nada y los archivos se copian con `adb push`, como siempre.
final modelosDeIaProvider = FutureProvider<ModelosDeIa>((ref) async {
  final url = dotenv.isInitialized ? dotenv.env['MODELOS_IA_URL'] : null;
  if (url == null || url.isEmpty) return const SinDescargaDeModelos();
  final documentos = await getApplicationDocumentsDirectory();
  return ModelosDeIaEnArchivos(carpeta: '${documentos.path}${Platform.pathSeparator}ia', urlBase: url);
});
