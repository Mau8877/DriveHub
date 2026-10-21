import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../models/crear_plantilla_horario_request.dart';
import '../models/fechas.dart';
import '../models/franja.dart';
import '../models/pagina.dart';
import '../models/plantilla_horario.dart';
import '../models/plantilla_horario_creada.dart';

/// Llamadas a la API de los horarios del instructor (CU12): horarios (`plantillas-horario`) y franjas.
///
/// * Un error de negocio del backend llega como [ApiException] (con su código `ERR_...`),
/// * no como un [DioException] crudo. Cualquier otro error (sin red, por ejemplo) se relanza tal cual.
/// * Los corchetes de `filter[...]` los codifica Dio solo.
class HorariosService {
  final Dio _dio;

  HorariosService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  Future<T> _llamar<T>(Future<T> Function() llamada) async {
    try {
      return await llamada();
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }

  /// Los horarios del instructor. `activa` nulo = activos e inactivos.
  Future<Pagina<PlantillaHorario>> listarPlantillas({bool? activa, int page = 1, int pageSize = 50}) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/plantillas-horario',
        queryParameters: {'page': page, 'pageSize': pageSize, 'filter[activa]': ?activa},
      );
      return Pagina.fromJson(response.data!, PlantillaHorario.fromJson);
    });
  }

  Future<PlantillaHorarioCreada> crearPlantilla(CrearPlantillaHorarioRequest request) {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>('/plantillas-horario', data: request.toJson());
      return PlantillaHorarioCreada.fromJson(response.data!);
    });
  }

  Future<void> desactivarPlantilla(String codigo) {
    return _llamar(() async {
      await _dio.post<dynamic>('/plantillas-horario/$codigo/desactivar');
    });
  }

  /// Las franjas del instructor. Sin `fecha`, desde hoy en adelante.
  Future<Pagina<Franja>> listarFranjas({DateTime? fecha, EstadoFranja? estado, int page = 1, int pageSize = 50}) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/franjas',
        queryParameters: {
          'page': page,
          'pageSize': pageSize,
          if (fecha != null) 'filter[fecha]': fechaParaApi(fecha),
          if (estado != null) 'filter[estado]': estado.valorApi,
        },
      );
      return Pagina.fromJson(response.data!, Franja.fromJson);
    });
  }

  Future<Franja> bloquearFranja(String codigo) {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>('/franjas/$codigo/bloquear');
      return Franja.fromJson(response.data!);
    });
  }

  Future<Franja> desbloquearFranja(String codigo) {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>('/franjas/$codigo/desbloquear');
      return Franja.fromJson(response.data!);
    });
  }
}
