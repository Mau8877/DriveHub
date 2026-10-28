import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/pagina.dart';
import '../models/resultado.dart';

/// Llamadas a la API de los resultados y notas del estudiante (CU23).
///
/// * Un error de negocio del backend llega como [ApiException] (con su código `ERR_...`), no como un
/// * [DioException] crudo. Cualquier otro error (sin red, por ejemplo) se relanza tal cual.
class ResultadosService {
  final Dio _dio;

  ResultadosService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  /// Las notas del estudiante, de la más reciente a la más antigua; [origen] filtra por tipo de evaluación.
  Future<Pagina<Resultado>> mios({
    OrigenResultado? origen,
    int page = 1,
    int pageSize = 50,
  }) => _lista('/resultados/mios', origen, page, pageSize);

  /// Las notas de los alumnos asignados al instructor, con el nombre de cada uno (CU23).
  Future<Pagina<Resultado>> deMisAlumnos({
    OrigenResultado? origen,
    int page = 1,
    int pageSize = 50,
  }) => _lista('/resultados/de-mis-alumnos', origen, page, pageSize);

  Future<Pagina<Resultado>> _lista(
    String ruta,
    OrigenResultado? origen,
    int page,
    int pageSize,
  ) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        ruta,
        queryParameters: {
          'page': page,
          'pageSize': pageSize,
          'filter[origen]': ?origen?.valorApi,
        },
      );
      return Pagina.fromJson(response.data!, Resultado.fromJson);
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }
}
