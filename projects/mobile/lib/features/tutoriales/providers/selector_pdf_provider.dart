import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Un PDF elegido del teléfono: su contenido y su nombre.
class PdfElegido {
  final Uint8List bytes;
  final String nombre;

  const PdfElegido({required this.bytes, required this.nombre});
}

/// Abre el selector de archivos del sistema y devuelve el PDF elegido, o `null` si la persona canceló.
///
/// * Es un provider para sustituirlo en los tests: ninguno abre un selector de verdad.
final selectorDePdfProvider = Provider<Future<PdfElegido?> Function()>(
  (ref) => () async {
    final elegidos = await FilePicker.pickFiles(
      type: FileType.custom,
      allowedExtensions: const ['pdf'],
    );
    if (elegidos.isEmpty) return null;
    final archivo = elegidos.first;
    return PdfElegido(bytes: await archivo.readAsBytes(), nombre: archivo.name);
  },
);
