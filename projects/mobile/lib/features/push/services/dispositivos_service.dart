import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/graphql_client.dart';
import 'documentos_graphql.dart';

/// Registro del dispositivo push de la persona en Notificaciones (CU36), por REST.
///
/// * El teléfono se registra con su token de Firebase y la plataforma `ANDROID`; el servicio devuelve el código del
///   dispositivo, que la app guarda para quitarlo al cerrar sesión.
/// * Un error de negocio llega como [ApiException]; cualquier otro (sin red) se relanza tal cual.
class DispositivosService {
  static const plataforma = 'ANDROID';

  final Dio _dio;

  DispositivosService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  /// Registra el token y devuelve el código del dispositivo.
  Future<String> registrar(String tokenFcm) async {
    try {
      final respuesta = await _dio.post<Map<String, dynamic>>(
        '/dispositivos-push',
        data: {'tokenFcm': tokenFcm, 'plataforma': plataforma},
      );
      return respuesta.data!['codigoDispositivoPush'] as String;
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }

  /// Quita el dispositivo con ese código.
  Future<void> quitar(String codigoDispositivoPush) async {
    try {
      await _dio.delete<dynamic>('/dispositivos-push/$codigoDispositivoPush');
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }
}

/// Lo mismo por GraphQL (CU45): `registrarDispositivo` y `quitarDispositivo`. Es un [DispositivosService] más, así que
/// el resto de la app no sabe cuál usa.
class DispositivosGraphqlService extends DispositivosService {
  final GraphqlClient _graphql;

  DispositivosGraphqlService({GraphqlClient? graphql}) : _graphql = graphql ?? GraphqlClient();

  @override
  Future<String> registrar(String tokenFcm) async {
    final datos = await _graphql.mutar(
      mutacionRegistrarDispositivo,
      variables: {
        'entrada': {'tokenFcm': tokenFcm, 'plataforma': DispositivosService.plataforma},
      },
    );
    return (datos['registrarDispositivo'] as Map<String, dynamic>)['codigoDispositivoPush'] as String;
  }

  @override
  Future<void> quitar(String codigoDispositivoPush) async {
    await _graphql.mutar(mutacionQuitarDispositivo, variables: {'codigo': codigoDispositivoPush});
  }
}
