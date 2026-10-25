import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';

/// El mensaje, en español y sin términos técnicos, de un error al pedir el reembolso (CU31).
///
/// * Un error de negocio se traduce por su código (`docs/errors/pagos/`); un error de red, a "no hay
///   conexión"; cualquier otra cosa, a un mensaje general. Nunca se muestra el texto crudo del error.
String mensajeDeErrorDeReembolso(Object error) {
  if (error is ApiException) {
    return switch (error.code) {
      'ERR_PAG_11' =>
        'Ya usaste horas de esta compra, no se puede reembolsar.',
      'ERR_PAG_10' => 'Esta compra ya no se puede reembolsar.',
      'ERR_PAG_07' => 'Esa compra no existe.',
      'ERR_PAG_06' || 'ERR_PAG_15' =>
        'No pudimos conectar con el pago, inténtalo más tarde.',
      _ => 'No pudimos pedir el reembolso. Inténtalo de nuevo más tarde.',
    };
  }
  if (error is DioException && _esDeConexion(error.type)) {
    return 'No hay conexión. Revisa tu internet e inténtalo de nuevo.';
  }
  return 'No pudimos pedir el reembolso. Inténtalo de nuevo más tarde.';
}

/// Si el error significa que la compra cambió y conviene volver a pedir su detalle.
bool errorDeReembolsoPideRefrescar(Object error) =>
    error is ApiException &&
    (error.code == 'ERR_PAG_10' || error.code == 'ERR_PAG_11');

bool _esDeConexion(DioExceptionType tipo) =>
    tipo == DioExceptionType.connectionError ||
    tipo == DioExceptionType.connectionTimeout ||
    tipo == DioExceptionType.sendTimeout ||
    tipo == DioExceptionType.receiveTimeout;
