import 'dart:io';

import 'package:open_filex/open_filex.dart';
import 'package:path_provider/path_provider.dart';

/// Abre un PDF en el teléfono (CU30): lo guarda en el directorio temporal como [nombre] (se sobrescribe) y lo
/// abre con el visor del sistema. Devuelve `false` si no hay ninguna app que abra PDF.
Future<bool> abrirPdf(List<int> bytes, String nombre) async {
  final directorio = await getTemporaryDirectory();
  final archivo = File('${directorio.path}${Platform.pathSeparator}$nombre');
  await archivo.writeAsBytes(bytes, flush: true);
  final resultado = await OpenFilex.open(archivo.path, type: 'application/pdf');
  return resultado.type == ResultType.done;
}
