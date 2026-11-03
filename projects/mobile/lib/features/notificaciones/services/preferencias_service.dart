import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../models/preferencias_notificacion.dart';

/// Llamadas a `/preferencias-notificacion` (CU37): consultar y guardar las de la persona con sesión.
///
/// * Un error de negocio del backend llega como [ApiException] (con su código `ERR_...`), no como un
///   [DioException] crudo. Cualquier otro error (sin red, por ejemplo) se relanza tal cual.
class PreferenciasService {
  final Dio _dio;

  PreferenciasService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  /// Lo guardado, o los valores por defecto si la persona nunca configuró nada.
  Future<PreferenciasNotificacion> obtener() async {
    final datos = await _llamar(
      () => _dio.get<Map<String, dynamic>>('/preferencias-notificacion'),
    );
    return PreferenciasNotificacion.fromJson(datos);
  }

  /// Guarda la configuración completa; [email] es el correo de la cuenta (el de su perfil).
  Future<PreferenciasNotificacion> guardar(
    PreferenciasNotificacion preferencias, {
    required String email,
  }) async {
    final datos = await _llamar(
      () => _dio.put<Map<String, dynamic>>(
        '/preferencias-notificacion',
        data: preferencias.toJson(email: email),
      ),
    );
    return PreferenciasNotificacion.fromJson(datos);
  }

  Future<Map<String, dynamic>> _llamar(
    Future<Response<Map<String, dynamic>>> Function() peticion,
  ) async {
    try {
      return (await peticion()).data!;
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }
}
