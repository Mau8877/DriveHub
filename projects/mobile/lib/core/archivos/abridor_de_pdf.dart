import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'abrir_pdf.dart';

/// Abre un PDF con el visor de la plataforma (el teléfono o el navegador); devuelve `false` si no hay ninguna app
/// que lo abra. Es un provider para sustituirlo en los tests: ninguno abre un visor de verdad.
///
/// * Vive en `core/` porque lo usan varias features (comprobantes, manuales).
final abridorDePdfProvider = Provider<Future<bool> Function(List<int> bytes, String nombre)>(
  (ref) => abrirPdf,
);

/// No hay ninguna app en el teléfono que abra PDF.
class SinVisorDePdf implements Exception {
  const SinVisorDePdf();
}
