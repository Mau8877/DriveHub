import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/pagina.dart';
import '../models/notificacion.dart';
import '../models/preferencias_notificacion.dart';

/// Llamadas a `/notificaciones` (CU38): la bandeja de la persona, el contador y marcar avisos como leídos.
///
/// * Un error de negocio del backend llega como [ApiException] (con su código `ERR_...`: `ERR_NOT_04` aviso
///   inexistente o ajeno), no como un [DioException] crudo. Cualquier otro error (sin red, por ejemplo) se
///   relanza tal cual.
class NotificacionesService {
  final Dio _dio;

  NotificacionesService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  Future<T> _llamar<T>(Future<T> Function() llamada) async {
    try {
      return await llamada();
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }

  /// Una página de la bandeja, del aviso más reciente al más antiguo. Los filtros solo viajan si se usan.
  Future<Pagina<Notificacion>> listar({
    int page = 1,
    int pageSize = 20,
    bool soloNoLeidas = false,
    TipoNotificacion? tipo,
  }) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/notificaciones',
        queryParameters: {
          'page': page,
          'pageSize': pageSize,
          if (soloNoLeidas) 'filter[leida]': 'false',
          if (tipo != null) 'filter[tipo]': tipo.valorApi,
        },
      );
      return Pagina.fromJson(response.data!, Notificacion.fromJson);
    });
  }

  /// Cuántos avisos sin leer tiene la persona.
  Future<int> contarNoLeidas() {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/notificaciones/conteo-no-leidas',
      );
      return response.data!['noLeidas'] as int;
    });
  }

  /// Marca un aviso como leído (idempotente) y lo devuelve ya leído.
  Future<Notificacion> leer(String codigoNotificacion) {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/notificaciones/$codigoNotificacion/leer',
      );
      return Notificacion.fromJson(response.data!);
    });
  }

  /// Marca todos los avisos pendientes como leídos y devuelve cuántos eran.
  Future<int> leerTodas() {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/notificaciones/leer-todas',
      );
      return response.data!['marcadas'] as int;
    });
  }
}
