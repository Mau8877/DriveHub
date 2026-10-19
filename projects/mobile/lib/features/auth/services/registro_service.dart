import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../models/registro_estudiante_request.dart';
import '../models/usuario_registrado.dart';

/// Llamada a la API del registro del estudiante (CU01).
///
/// * Un error de negocio del backend llega como [ApiException] (con su
/// * código `ERR_...`), no como un [DioException] crudo.
class RegistroService {
  final Dio _dio;

  RegistroService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  Future<UsuarioRegistrado> registrar(RegistroEstudianteRequest request) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>('/usuarios', data: request.toJson());
      return UsuarioRegistrado.fromJson(response.data!);
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }
}
