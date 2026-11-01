import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:tflite_flutter/tflite_flutter.dart';

import '../models/analisis_tablero.dart';
import 'procesar_imagen.dart';

/// El modelo no está en la app (todavía no se entrenó) o no se pudo cargar.
class ModeloNoDisponible implements Exception {
  const ModeloNoDisponible();
}

/// Reconoce la luz del tablero de una foto (CU40). Corre en el teléfono, sin internet.
abstract class ClasificadorTablero {
  /// Nombre y versión del modelo, para el historial.
  String get modelo;

  /// Las probabilidades por luz de la imagen ([ladoModelo] × [ladoModelo] RGB), de mayor a menor.
  /// Lanza [ModeloNoDisponible] si el modelo no está.
  Future<List<Deteccion>> clasificar(Float32List entrada);
}

/// Las [k] detecciones más probables, de mayor a menor confianza.
List<Deteccion> mejoresDetecciones(
  List<double> probabilidades,
  List<String> clases, {
  int k = 3,
}) {
  final todas = [
    for (var i = 0; i < probabilidades.length && i < clases.length; i++)
      Deteccion(codigo: clases[i], confianza: probabilidades[i]),
  ]..sort((a, b) => b.confianza.compareTo(a.confianza));
  return todas.take(k).toList();
}

/// El clasificador real: el `.tflite` entrenado con `ml/entrenar_tablero.py`.
///
/// * Carga el modelo y las etiquetas la primera vez y los reutiliza.
/// * El orden de las clases es el de `tablero_labels.json`, que escribe el entrenamiento.
class ClasificadorTflite implements ClasificadorTablero {
  static const _rutaModelo = 'assets/ml/tablero.tflite';
  static const _rutaEtiquetas = 'assets/ml/tablero_labels.json';

  Interpreter? _interprete;
  List<String> _clases = const [];

  @override
  String get modelo => 'tablero-v1';

  Future<void> _cargar() async {
    if (_interprete != null) return;
    try {
      final etiquetas =
          jsonDecode(await rootBundle.loadString(_rutaEtiquetas))
              as Map<String, dynamic>;
      _clases = (etiquetas['clases'] as List<dynamic>).cast<String>();
      _interprete = await Interpreter.fromAsset(_rutaModelo);
    } catch (_) {
      throw const ModeloNoDisponible();
    }
  }

  @override
  Future<List<Deteccion>> clasificar(Float32List entrada) async {
    await _cargar();
    final interprete = _interprete!;
    final salida = List.generate(1, (_) => List.filled(_clases.length, 0.0));
    interprete.run(entrada.reshape([1, ladoModelo, ladoModelo, 3]), salida);
    return mejoresDetecciones(salida.first, _clases);
  }
}
