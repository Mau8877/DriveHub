import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/pagina.dart';
import '../models/crear_plantilla_horario_request.dart';
import '../models/dia_bloqueado.dart';
import '../models/estado_plantilla.dart';
import '../models/fechas.dart';
import '../models/franja.dart';
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

  /// Los horarios del instructor en esos estados. Sin estados, el backend devuelve activos y pausados.
  Future<Pagina<PlantillaHorario>> listarPlantillas({Set<EstadoPlantilla>? estados, int page = 1, int pageSize = 50}) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/plantillas-horario',
        queryParameters: {
          'page': page,
          'pageSize': pageSize,
          if (estados != null && estados.isNotEmpty) 'filter[estado]': [for (final e in estados) e.valorApi],
        },
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

  Future<void> pausarPlantilla(String codigo) {
    return _llamar(() async {
      await _dio.post<dynamic>('/plantillas-horario/$codigo/pausar');
    });
  }

  Future<void> reanudarPlantilla(String codigo) {
    return _llamar(() async {
      await _dio.post<dynamic>('/plantillas-horario/$codigo/reanudar');
    });
  }

  Future<void> desactivarPlantilla(String codigo) {
    return _llamar(() async {
      await _dio.post<dynamic>('/plantillas-horario/$codigo/desactivar');
    });
  }

  /// Las franjas del instructor. Con [desde] y [hasta] (hasta 7 días), ese rango; con las dos iguales, un
  /// solo día; sin ninguna, desde hoy en adelante.
  Future<Pagina<Franja>> listarFranjas({
    DateTime? desde,
    DateTime? hasta,
    EstadoFranja? estado,
    int page = 1,
    int pageSize = 50,
  }) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/franjas',
        queryParameters: {
          'page': page,
          'pageSize': pageSize,
          if (desde != null) 'filter[fechaDesde]': fechaParaApi(desde),
          if (hasta != null) 'filter[fechaHasta]': fechaParaApi(hasta),
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

  /// Bloquea las franjas libres del día que todavía no empezaron. Cero franjas no es un error.
  Future<DiaBloqueado> bloquearDia(DateTime fecha) {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>('/franjas/dias/${fechaParaApi(fecha)}/bloquear');
      return DiaBloqueado.fromJson(response.data!);
    });
  }

  /// Vuelve a libres las franjas bloqueadas del día que todavía no empezaron.
  Future<DiaBloqueado> desbloquearDia(DateTime fecha) {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>('/franjas/dias/${fechaParaApi(fecha)}/desbloquear');
      return DiaBloqueado.fromJson(response.data!);
    });
  }
}
