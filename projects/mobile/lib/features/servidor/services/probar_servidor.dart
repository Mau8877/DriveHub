import 'package:dio/dio.dart';

/// Qué encontró la prueba de conexión.
enum ResultadoPrueba {
  /// Respondió el gateway de DriveHub (se reconoce por su sobre de respuesta).
  servidorDriveHub,

  /// Algo respondió, pero no parece un servidor de DriveHub (otra dirección, otro puerto o un portal de red).
  respondeOtraCosa,

  /// Nada respondió: dirección mal escrita, servidor apagado, o el teléfono no está en esa red.
  sinRespuesta,
}

/// Comprueba si en [direccionApi] hay un servidor de DriveHub.
///
/// * Pide la raíz de la API sin sesión: el gateway contesta (404 o 401) con su sobre estándar, y eso lo distingue de
///   cualquier otro servidor que también responda.
/// * No lanza: un fallo de red es [ResultadoPrueba.sinRespuesta].
/// * Usa un `Dio` propio, sin interceptores: no debe llevar sesión ni pasar por la caché o la cola offline.
Future<ResultadoPrueba> probarServidor(String direccionApi, {Dio? dio}) async {
  final cliente =
      dio ??
      Dio(
        BaseOptions(
          baseUrl: direccionApi,
          connectTimeout: const Duration(seconds: 5),
          receiveTimeout: const Duration(seconds: 5),
          validateStatus: (_) => true,
        ),
      );
  try {
    // * Un 404 o un 401 es justo lo que se espera de la raíz: no es un fallo de la prueba
    final respuesta = await cliente.get<dynamic>('/', options: Options(validateStatus: (_) => true));
    return _esSobreDeDriveHub(respuesta.data) ? ResultadoPrueba.servidorDriveHub : ResultadoPrueba.respondeOtraCosa;
  } on DioException {
    return ResultadoPrueba.sinRespuesta;
  } finally {
    if (dio == null) cliente.close();
  }
}

/// El sobre estándar de la API (`{ status, data, message, timestamp }`, ver RESPONSES_BACKEND.md).
bool _esSobreDeDriveHub(Object? cuerpo) =>
    cuerpo is Map && (cuerpo['status'] == 'success' || cuerpo['status'] == 'failed') && cuerpo['timestamp'] != null;
