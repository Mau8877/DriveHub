import 'dart:typed_data';

import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../models/cambiar_contrasena_request.dart';
import '../models/cambiar_correo_request.dart';
import '../models/editar_perfil_request.dart';
import '../models/perfil.dart';

/// Llamadas a la API del perfil de la cuenta propia (CU06). La persona sale de la sesión: ninguna
/// ruta lleva su código.
///
/// * Un error de negocio llega como [ApiException] (con su código `ERR_...`); el servicio no lo
/// * captura, lo muestra la pantalla.
class PerfilService {
  final Dio _dio;

  PerfilService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  Future<Perfil> obtener() async {
    final datos = await _llamar(
      () => _dio.get<Map<String, dynamic>>('/usuarios/me'),
    );
    return Perfil.fromJson(datos!);
  }

  Future<Perfil> editar(EditarPerfilRequest request) async {
    final datos = await _llamar(
      () => _dio.patch<Map<String, dynamic>>(
        '/usuarios/me',
        data: request.toJson(),
      ),
    );
    return Perfil.fromJson(datos!);
  }

  /// Sube la foto como `multipart/form-data` con la parte `archivo`.
  Future<Perfil> subirFoto(
    Uint8List bytes, {
    required String nombre,
    required String tipo,
  }) async {
    final cuerpo = FormData.fromMap({
      'archivo': MultipartFile.fromBytes(
        bytes,
        filename: nombre,
        contentType: DioMediaType.parse(tipo),
      ),
    });
    final datos = await _llamar(
      () => _dio.put<Map<String, dynamic>>('/usuarios/me/foto', data: cuerpo),
    );
    return Perfil.fromJson(datos!);
  }

  /// Idempotente: sin foto responde igual.
  Future<Perfil> quitarFoto() async {
    final datos = await _llamar(
      () => _dio.delete<Map<String, dynamic>>('/usuarios/me/foto'),
    );
    return Perfil.fromJson(datos!);
  }

  Future<void> cambiarContrasena(CambiarContrasenaRequest request) async {
    await _llamar(
      () => _dio.post<Map<String, dynamic>>(
        '/usuarios/me/cambiar-contrasena',
        data: request.toJson(),
      ),
    );
  }

  /// Envía el enlace al correo NUEVO; el correo de la cuenta no cambia hasta abrirlo.
  Future<void> solicitarCambioCorreo(CambiarCorreoRequest request) async {
    await _llamar(
      () => _dio.post<Map<String, dynamic>>(
        '/usuarios/me/cambiar-correo',
        data: request.toJson(),
      ),
    );
  }

  Future<Map<String, dynamic>?> _llamar(
    Future<Response<Map<String, dynamic>>> Function() peticion,
  ) async {
    try {
      return (await peticion()).data;
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }
}
