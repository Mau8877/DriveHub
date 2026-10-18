import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

/// Excepción tipada para un error de negocio devuelto por el backend
/// (`status: "failed"`, ver RESPONSES_BACKEND.md) -- nunca se deja propagar
/// un [DioException] crudo, sin código, hasta la UI.
class ApiException implements Exception {
  final String code;
  final String message;

  ApiException({required this.code, required this.message});

  @override
  String toString() => 'ApiException($code): $message';
}

/// Cliente HTTP único de la app -- todo `service` de cada feature llama a
/// través de acá, nunca crea su propia instancia de [Dio] (ver
/// CONSUMO_API_MOBILE.md).
///
/// * El interceptor desempaqueta el sobre estándar del backend
/// * (`{status, data, message, error, timestamp, meta?}`): en éxito deja
/// * pasar `data` (y `meta` si vino) como si fuera la respuesta real; en
/// * error, lanza [ApiException] en vez de un [DioException] sin código de
/// * negocio.
class ApiClient {
  static Dio? _instance;

  static Dio get instance {
    _instance ??= _build();
    return _instance!;
  }

  static Dio _build() {
    final dio = Dio(
      BaseOptions(baseUrl: dotenv.env['API_BASE_URL'] ?? 'http://10.0.2.2:8080/api/v1'),
    );

    dio.interceptors.add(
      InterceptorsWrapper(
        onResponse: (response, handler) {
          final envelope = response.data as Map<String, dynamic>;
          response.data = envelope['meta'] != null
              ? {'data': envelope['data'], 'meta': envelope['meta']}
              : envelope['data'];
          handler.next(response);
        },
        onError: (error, handler) {
          final envelope = error.response?.data;
          if (envelope is Map<String, dynamic> && envelope['error'] != null) {
            handler.reject(
              DioException(
                requestOptions: error.requestOptions,
                error: ApiException(
                  code: envelope['error'] as String,
                  message: envelope['message'] as String? ?? 'Ocurrió un error inesperado',
                ),
                response: error.response,
              ),
            );
            return;
          }
          handler.next(error);
        },
      ),
    );

    if (dotenv.env['ENABLE_API_LOGS'] == 'true') {
      dio.interceptors.add(
        LogInterceptor(requestBody: false, responseBody: true),
      );
    }

    return dio;
  }
}
