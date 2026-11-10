import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';

import '../models/catalogo_acciones.dart';
import '../services/modelo_de_acciones.dart';
import '../services/motor_litert_lm.dart';

/// Las acciones de la pantalla de Clases que el modelo puede elegir.
final catalogoAccionesProvider = FutureProvider<CatalogoAcciones>((ref) => CatalogoAcciones.cargar());

/// Dónde busca la app el modelo de acciones: `<documentos>/ia/acciones.litertlm`.
///
/// * El modelo pesa ~284 MB y no se versiona. En desarrollo se copia con `adb push` a esa carpeta; en producción la
///   app lo descarga la primera vez (`modelosDeIaProvider`, `MODELOS_IA_URL`).
const nombreDelModeloDeAcciones = 'acciones.litertlm';

final rutaModeloDeAccionesProvider = FutureProvider<String>((ref) async {
  final documentos = await getApplicationDocumentsDirectory();
  return '${documentos.path}${Platform.pathSeparator}ia${Platform.pathSeparator}$nombreDelModeloDeAcciones';
});

/// El modelo de acciones en el teléfono (CU50). Se libera al dejar de usarse. Un provider para poder sustituirlo
/// en los tests por un modelo falso.
final modeloDeAccionesProvider = FutureProvider<ModeloDeAcciones>((ref) async {
  final catalogo = await ref.watch(catalogoAccionesProvider.future);
  final ruta = await ref.watch(rutaModeloDeAccionesProvider.future);
  final modelo = ModeloDeAccionesLitert(motor: MotorLitertLm(rutaModelo: ruta, catalogo: catalogo), catalogo: catalogo);
  ref.onDispose(modelo.liberar);
  return modelo;
});
