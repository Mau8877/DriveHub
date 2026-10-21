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

  // * La sesión la maneja la feature auth; `core/` solo recibe dos funciones para no importar `features/`
  static String? Function() _obtenerToken = () => null;
  static Future<String?> Function() _renovar = () async => null;

  /// Conecta el cliente con la sesión: el token que se manda como Bearer y cómo
  /// renovarla ante un 401. La llama `main()` una vez al arrancar.
  static void configurarSesion({
    required String? Function() obtenerToken,
    required Future<String?> Function() renovar,
  }) {
    _obtenerToken = obtenerToken;
    _renovar = renovar;
  }

  static Dio get instance {
    _instance ??= _build();
    return _instance!;
  }

  static Dio _build() {
    final dio = Dio(
      BaseOptions(
        baseUrl: dotenv.env['API_BASE_URL'] ?? 'http://10.0.2.2:8080/api/v1',
      ),
    );

    // * Primero: ve el 401 crudo del backend, antes de que el desempaquetado lo convierta en ApiException.
    // * Si repite la petición, la respuesta ya sale desempaquetada de la segunda pasada por la cadena.
    dio.interceptors.add(_interceptorDeSesion(dio));
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
                  message:
                      envelope['message'] as String? ??
                      'Ocurrió un error inesperado',
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

  /// Agrega el Bearer y, ante un 401 de una petición de negocio, renueva la
  /// sesión UNA vez y repite la petición. Las rutas `/auth/` no llevan Bearer
  /// ni se reintentan, salvo `/auth/verificar-correo` (CU04b): pide el correo de la
  /// cuenta con sesión, así que sin Bearer el backend la rechaza con 401.
  static Interceptor _interceptorDeSesion(Dio dio) {
    bool esDeAuth(RequestOptions options) =>
        options.path.startsWith('/auth/') &&
        options.path != '/auth/verificar-correo';

    return InterceptorsWrapper(
      onRequest: (options, handler) {
        final token = _obtenerToken();
        if (token != null && !esDeAuth(options)) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        handler.next(options);
      },
      onError: (error, handler) async {
        final options = error.requestOptions;
        final reintentable =
            error.response?.statusCode == 401 &&
            options.extra['reintentada'] != true &&
            !esDeAuth(options);
        if (!reintentable) {
          handler.next(error);
          return;
        }
        final tokenNuevo = await _renovar();
        if (tokenNuevo == null) {
          handler.next(error);
          return;
        }
        options.extra['reintentada'] = true;
        try {
          handler.resolve(await dio.fetch<dynamic>(options));
        } on DioException catch (e) {
          handler.reject(e);
        }
      },
    );
  }
}
