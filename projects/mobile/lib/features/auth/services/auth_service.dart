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

  /// Pide el correo con el enlace para verificar la dirección de la cuenta
  /// (CU04b). Exige sesión: el Bearer lo agrega el interceptor del [ApiClient].
  Future<void> solicitarVerificacionCorreo() async {
    try {
      await _dio.post<Object?>('/auth/verificar-correo');
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }

  /// Pide el enlace para recuperar la contraseña (CU04). Es público y responde
  /// siempre lo mismo, exista o no la cuenta: no hay nada que parsear.
  Future<void> recuperar(String email) async {
    try {
      await _dio.post<Object?>('/auth/recuperar', data: {'email': email});
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
