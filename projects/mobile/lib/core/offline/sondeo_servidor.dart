import 'package:dio/dio.dart';

import '../network/api_client.dart';

/// ¿El servidor responde? Cualquier respuesta HTTP (incluso un 404 o un 401) prueba que hay conexión: solo
/// un fallo de red o un tiempo agotado dice que no.
///
/// * Usa un `Dio` propio, sin interceptores: no debe pasar por la caché ni por la cola.
Future<bool> sondearServidor() async {
  final dio = Dio(
    BaseOptions(
      baseUrl: ApiClient.baseUrlActual,
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
