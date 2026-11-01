import '../models/youtube.dart';

/// Reglas de validación del formulario de tutoriales (CU33), iguales a las del backend y a las de la web.
const int tituloMin = 3;
const int tituloMax = 150;
const int descripcionMax = 1000;
const int etiquetasMax = 10;
const int etiquetaLargoMax = 30;

/// El PDF pesa hasta 20 MB.
const int pdfMaxBytes = 20 * 1024 * 1024;

String? validarTitulo(String? valor) {
  final texto = valor?.trim() ?? '';
  if (texto.isEmpty) return 'El título es obligatorio';
  if (texto.length < tituloMin || texto.length > tituloMax) {
    return 'El título debe tener entre $tituloMin y $tituloMax caracteres';
  }
  return null;
}

String? validarDescripcion(String? valor) =>
    (valor?.trim().length ?? 0) > descripcionMax
    ? 'La descripción no puede superar $descripcionMax caracteres'
    : null;

/// Las etiquetas se escriben separadas por comas.
List<String> separarEtiquetas(String? valor) => {
  for (final e in (valor ?? '').split(','))
    if (e.trim().isNotEmpty) e.trim(),
}.toList();

String? validarEtiquetas(String? valor) {
  final etiquetas = separarEtiquetas(valor);
  if (etiquetas.length > etiquetasMax) {
    return 'Puedes poner hasta $etiquetasMax etiquetas';
  }
  if (etiquetas.any((e) => e.length > etiquetaLargoMax)) {
    return 'Cada etiqueta puede tener hasta $etiquetaLargoMax caracteres';
  }
  return null;
}

String? validarEnlaceYoutube(String? valor) {
  final texto = valor?.trim() ?? '';
  if (texto.isEmpty) return 'Pega el enlace del video';
  return idDeYoutube(texto) == null
      ? 'El enlace debe ser de un video de YouTube'
      : null;
}

String? validarAudiencia(Set<Object> elegidas) =>
    elegidas.isEmpty ? 'Elige a quién va dirigido el tutorial' : null;

String? validarTamanoPdf(int bytes) =>
    bytes > pdfMaxBytes ? 'El PDF no puede pesar más de 20 MB' : null;
