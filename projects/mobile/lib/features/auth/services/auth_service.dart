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
