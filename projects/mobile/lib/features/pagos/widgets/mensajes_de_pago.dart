import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';

/// El mensaje, en español y sin términos técnicos, de un error al iniciar la compra (CU26).
///
/// * Un error de negocio del backend se traduce por su código (`docs/errors/pagos/`); un error de red, a
/// * "no hay conexión"; cualquier otra cosa, a un mensaje general. Nunca se muestra el texto crudo del error.
String mensajeDeErrorDeCompra(Object error) {
  if (error is ApiException) {
    return switch (error.code) {
      'ERR_PAG_05' => 'Ese plan ya no está disponible. Elige otro.',
      'ERR_PAG_06' || 'ERR_PAG_15' => 'No pudimos conectar con el pago. Inténtalo de nuevo más tarde.',
      'ERR_PAG_17' => 'Tu compra ya se está procesando. Espera unos segundos e inténtalo de nuevo.',
      'ERR_PAG_18' => 'No pudimos repetir esa compra. Vuelve a elegir el plan.',
      _ => 'No pudimos iniciar la compra. Inténtalo de nuevo más tarde.',
    };
  }
  if (error is DioException && _esDeConexion(error.type)) {
    return 'No hay conexión. Revisa tu internet e inténtalo de nuevo.';
  }
  return 'No pudimos iniciar la compra. Inténtalo de nuevo más tarde.';
}

bool _esDeConexion(DioExceptionType tipo) =>
    tipo == DioExceptionType.connectionError ||
    tipo == DioExceptionType.connectionTimeout ||
    tipo == DioExceptionType.sendTimeout ||
    tipo == DioExceptionType.receiveTimeout;
