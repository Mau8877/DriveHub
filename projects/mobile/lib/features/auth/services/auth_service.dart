import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../models/login_request.dart';
import '../models/sesion_respuesta.dart';

/// Llamadas HTTP de la sesión móvil (CU02).
///
/// * Un error de negocio del backend llega como [ApiException] (con su
/// * código `ERR_...`), no como un [DioException] crudo.
class AuthService {
  final Dio _dio;

  AuthService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  Future<SesionRespuesta> login(String email, String password) {
    return _post(
      '/auth/login',
      LoginRequest(email: email, password: password).toJson(),
    );
  }

  Future<SesionRespuesta> renovar(String refreshToken) {
    return _post('/auth/refresh', {
      'dispositivo': 'MOVIL',
      'refreshToken': refreshToken,
    });
  }

  /// Cierra la sesión de este dispositivo (CU03). El backend responde 200
  /// aunque la sesión ya no exista, así que no hay respuesta que parsear.
  Future<void> cerrar(String refreshToken) async {
    try {
      await _dio.post<Object?>(
        '/auth/logout',
        data: {'dispositivo': 'MOVIL', 'refreshToken': refreshToken},
      );
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }

  Future<SesionRespuesta> _post(
    String ruta,
    Map<String, dynamic> cuerpo,
  ) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        ruta,
        data: cuerpo,
      );
      return SesionRespuesta.fromJson(response.data!);
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }
}
