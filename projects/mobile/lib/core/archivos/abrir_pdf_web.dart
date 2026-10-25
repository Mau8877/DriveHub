import 'dart:js_interop';
import 'dart:typed_data';

import 'package:web/web.dart' as web;

/// Abre un PDF en el navegador (la app corriendo en web, CU30): lo muestra en una pestaña nueva y, si el navegador
/// la bloquea, lo descarga con el nombre [nombre]. No hay disco ni visor del sistema: todo es un objeto en memoria.
Future<bool> abrirPdf(List<int> bytes, String nombre) async {
  final blob = web.Blob(
    [Uint8List.fromList(bytes).toJS].toJS,
    web.BlobPropertyBag(type: 'application/pdf'),
  );
  final direccion = web.URL.createObjectURL(blob);
  final pestana = web.window.open(direccion, '_blank');
  if (pestana == null) {
    final enlace = web.HTMLAnchorElement()
      ..href = direccion
      ..download = nombre;
    enlace.click();
  }
  return true;
}
