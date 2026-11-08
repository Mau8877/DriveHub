import 'package:dio/dio.dart';

import 'api_client.dart';

/// Las marcas que `ApiClient` lee en las peticiones de GraphQL.
class OpcionesGraphql {
  /// La respuesta no es el sobre estándar `{ status, data, ... }`: `ApiClient` la deja pasar tal cual.
  static const clave = 'graphql';

  const OpcionesGraphql._();
}

/// Cliente de GraphQL (CU45): una sola ruta, `POST /graphql`, que recibe el texto de la consulta y sus variables.
///
/// * Usa el `ApiClient` de siempre, así que lleva el Bearer, renueva la sesión ante un 401 del gateway y entra en
///   el modo offline igual que una llamada REST.
/// * Una **consulta** ([consultar]) se guarda en el teléfono y, sin conexión, se responde con lo último guardado. Una
///   **mutación** ([mutar]) nunca se guarda ni se encola: sin conexión falla.
/// * Un error de negocio llega como [ApiException] con su código `ERR_...` (viene en `extensions.code`), igual que
///   en REST; un fallo de red se relanza como [DioException] para que el modo offline y las pantallas lo traten igual.
/// ! GraphQL responde 200 aunque haya un error de negocio: se revisa `errors` además del estado HTTP.
class GraphqlClient {
  final Dio _dio;

  GraphqlClient({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  /// Una consulta (solo lee). Devuelve el contenido de `data`.
  Future<Map<String, dynamic>> consultar(String documento, {Map<String, dynamic>? variables}) =>
      _ejecutar(documento, variables, esConsulta: true);

  /// Una mutación (cambia algo). Devuelve el contenido de `data`.
  Future<Map<String, dynamic>> mutar(String documento, {Map<String, dynamic>? variables}) =>
      _ejecutar(documento, variables, esConsulta: false);

  Future<Map<String, dynamic>> _ejecutar(
    String documento,
    Map<String, dynamic>? variables, {
    required bool esConsulta,
  }) async {
    final Response<Map<String, dynamic>> respuesta;
    try {
      respuesta = await _dio.post<Map<String, dynamic>>(
        '/graphql',
        data: {
          'query': documento,
          if (variables != null && variables.isNotEmpty) 'variables': variables,
        },
        options: Options(
          extra: {
            OpcionesGraphql.clave: true,
            if (esConsulta) OpcionesOffline.claveConsultaGraphql: true,
          },
        ),
      );
    } on DioException catch (e) {
      // * Una consulta inválida (400) o rechazada también trae `errors` en el cuerpo
      final error = _errorDe(e.response?.data);
      if (error != null) throw error;
      rethrow;
    }

    final cuerpo = respuesta.data;
    final error = _errorDe(cuerpo);
    if (error != null) throw error;

    final datos = cuerpo?['data'];
    if (datos is! Map<String, dynamic>) {
      throw ApiException(code: 'ERR_SYS_00', message: 'Ocurrió un error inesperado');
    }
    return datos;
  }

  /// El primer error de la respuesta como [ApiException], o `null` si no trae errores.
  static ApiException? _errorDe(Object? cuerpo) {
    if (cuerpo is! Map) return null;
    final errores = cuerpo['errors'];
    if (errores is! List || errores.isEmpty) return null;
    final primero = errores.first;
    if (primero is! Map) return ApiException(code: 'ERR_SYS_00', message: 'Ocurrió un error inesperado');
    final extensiones = primero['extensions'];
    final codigo = extensiones is Map ? extensiones['code'] : null;
    return ApiException(
      code: codigo is String ? codigo : 'ERR_SYS_00',
      message: primero['message'] is String ? primero['message'] as String : 'Ocurrió un error inesperado',
    );
  }
}
