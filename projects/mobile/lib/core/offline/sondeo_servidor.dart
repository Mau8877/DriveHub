import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

/// ¿El servidor responde? Cualquier respuesta HTTP (incluso un 404 o un 401) prueba que hay conexión: solo
/// un fallo de red o un tiempo agotado dice que no.
///
/// * Usa un `Dio` propio, sin interceptores: no debe pasar por la caché ni por la cola.
Future<bool> sondearServidor() async {
  final dio = Dio(
    BaseOptions(
      baseUrl: dotenv.env['API_BASE_URL'] ?? 'http://10.0.2.2:8080/api/v1',
      connectTimeout: const Duration(seconds: 5),
      receiveTimeout: const Duration(seconds: 5),
      validateStatus: (_) => true,
    ),
  );
  try {
    await dio.head<dynamic>('/');
    return true;
  } on DioException {
    return false;
  } finally {
    dio.close();
  }
}
