import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/pagina.dart';
import '../models/hora_alumno.dart';
import '../models/horas_alumno_detalle.dart';

/// Llamadas a la API de las horas de los alumnos del instructor (CU29): el listado con búsqueda y el detalle.
///
/// * Un error de negocio del backend llega como [ApiException] (con su código `ERR_...`: `ERR_PAG_12` alumno no
///   asignado, `ERR_PAG_15` servicio no disponible), no como un [DioException] crudo. Cualquier otro error (sin
///   red, por ejemplo) se relanza tal cual.
class HorasAlumnosService {
  final Dio _dio;

  HorasAlumnosService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  Future<T> _llamar<T>(Future<T> Function() llamada) async {
    try {
      return await llamada();
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }

  /// Los alumnos del instructor, por apellidos y nombres. [search] busca dentro del nombre completo; la búsqueda
  /// la hace el backend.
  Future<Pagina<HoraAlumno>> listar({
    String? search,
    int page = 1,
    int pageSize = 20,
  }) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/horas-alumnos',
        queryParameters: {
          'page': page,
          'pageSize': pageSize,
          if (search != null && search.trim().isNotEmpty) 'search': search.trim(),
        },
      );
      return Pagina.fromJson(response.data!, HoraAlumno.fromJson);
    });
  }

  /// El detalle de un alumno. Uno ajeno o inexistente llega como [ApiException] `ERR_PAG_12`.
  Future<HorasAlumnoDetalle> detalle(String estudianteCodigo) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/horas-alumnos/$estudianteCodigo',
      );
      return HorasAlumnoDetalle.fromJson(response.data!);
    });
  }
}
