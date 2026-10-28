import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/pagina.dart';
import '../models/manual.dart';

/// Llamadas a la API de los manuales publicados (CU34).
///
/// * Un error de negocio del backend llega como [ApiException] (con su código `ERR_...`), no como un
/// * [DioException] crudo. Cualquier otro error (sin red, por ejemplo) se relanza tal cual.
class ManualesService {
  final Dio _dio;

  ManualesService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  /// Los manuales publicados de la escuela, del más reciente al más antiguo.
  Future<Pagina<Manual>> publicados({
    String? busqueda,
    String? categoria,
    int page = 1,
    int pageSize = 50,
  }) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/manuales/publicados',
        queryParameters: {
          'page': page,
          'pageSize': pageSize,
          if (busqueda != null && busqueda.trim().isNotEmpty)
            'search': busqueda.trim(),
          'filter[categoria]': ?categoria,
        },
      );
      return Pagina.fromJson(response.data!, Manual.fromJson);
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }
}
