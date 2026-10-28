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
  final Dio _descargas;

  /// [descargas] baja los PDF del almacén de archivos; es un cliente aparte y sin interceptores a propósito: el
  /// PDF no está en el gateway y el token de la sesión no debe viajar a otro servidor.
  ManualesService({Dio? dio, Dio? descargas})
    : _dio = dio ?? ApiClient.instance,
      _descargas = descargas ?? Dio();

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

  /// Los bytes del PDF del manual, desde su dirección pública.
  Future<List<int>> descargarPdf(String url) async {
    final response = await _descargas.get<List<int>>(
      url,
      options: Options(responseType: ResponseType.bytes),
    );
    return response.data!;
  }
}
