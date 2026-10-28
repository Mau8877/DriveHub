import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../models/analisis_tablero.dart';
import '../models/ayuda_tablero.dart';

/// Llamadas a la API de las ayudas del tablero (CU40, CU42).
///
/// * El análisis ya se hizo en el teléfono: acá solo se guarda el resultado en el historial.
/// * Un error de negocio llega como [ApiException] (con su código `ERR_...`), no como un
///   [DioException] crudo. Cualquier otro error (sin red, por ejemplo) se relanza tal cual.
class AyudasTableroService {
  final Dio _dio;

  AyudasTableroService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  /// Guarda el análisis (y su miniatura, si la hay) como `multipart/form-data`: el campo `datos` con el
  /// JSON del resultado y la parte `miniatura`.
  Future<AyudaTablero> crear(AnalisisTablero analisis) async {
    final principal = analisis.principal;
    final datos = {
      'indicadores': [
        for (final l in analisis.luces)
          {
            'codigo': l.luz.codigo,
            'nombre': l.luz.nombre,
            'confianza': l.confianza,
            'severidad': l.luz.severidad.valorApi,
          },
      ],
      'respuesta': _recortar(principal?.luz.explicacion ?? '', 800),
      'modelo': analisis.modelo,
      'latenciaMs': analisis.latenciaMs,
    };
    final miniatura = analisis.miniatura;
    final cuerpo = FormData.fromMap({
      'datos': jsonEncode(datos),
      if (miniatura != null)
        'miniatura': MultipartFile.fromBytes(
          Uint8List.fromList(miniatura),
          filename: 'tablero.jpg',
          contentType: DioMediaType.parse('image/jpeg'),
        ),
    });
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/ayudas-tablero',
        data: cuerpo,
      );
      return AyudaTablero.fromJson(response.data!);
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }

  static String _recortar(String texto, int maximo) =>
      texto.length <= maximo ? texto : '${texto.substring(0, maximo - 1)}…';
}
