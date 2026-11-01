import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../models/prueba.dart';

/// Llamadas a la API de las sesiones con la IA (CU39).
///
/// * La prueba ya se evaluó en el teléfono: acá solo se guarda el resultado.
/// * Un error de negocio llega como [ApiException] (con su código `ERR_...`), no como un [DioException] crudo.
///   Cualquier otro error (sin red, por ejemplo) se relanza tal cual.
class SesionesService {
  final Dio _dio;

  SesionesService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  /// Guarda una prueba terminada y devuelve el código de la sesión creada.
  Future<String> guardarPrueba(
    ResultadoPrueba resultado, {
    String? categoria,
    String? escenario,
  }) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/sesiones-ia',
        data: {
          'categoria': ?categoria,
          'escenario': ?escenario,
          'puntaje': resultado.puntaje,
          'retroalimentacion': resultado.retroalimentacion,
          'modelo': resultado.modelo,
          'duracionSeg': resultado.duracionSeg,
          'pasos': [
            for (final p in resultado.pasos)
              {
                'orden': p.orden,
                'instruccion': p.paso.instruccion,
                'aprobado': p.aprobado,
                'tiempoMs': p.tiempoMs,
              },
          ],
        },
      );
      return response.data!['codigo'] as String;
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }
}
