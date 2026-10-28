import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';

/// El mensaje, en español y sin términos técnicos, de un error al iniciar o enviar un simulacro (CU21).
///
/// * Un error de negocio del backend se traduce por su código (`docs/errors/academico/`); un error de red, a
/// * "no hay conexión"; cualquier otra cosa, a un mensaje general. Nunca se muestra el texto crudo del error.
String mensajeDeErrorDeSimulacro(Object error) {
  if (error is ApiException) {
    return switch (error.code) {
      'ERR_ACA_21' => 'Ese simulacro ya no está disponible. Elige otro.',
      'ERR_ACA_28' =>
        'Este simulacro todavía no tiene preguntas suficientes. Avisa a tu escuela.',
      'ERR_ACA_29' =>
        'Ya tienes un intento en curso de este simulacro. Espera a que termine su tiempo.',
      'ERR_ACA_30' => 'No encontramos ese intento.',
      'ERR_ACA_31' => 'Este intento ya terminó.',
      _ => 'No pudimos completar la operación. Inténtalo de nuevo más tarde.',
    };
  }
  if (error is DioException && _esDeConexion(error.type)) {
    return 'No hay conexión. Revisa tu internet e inténtalo de nuevo.';
  }
  return 'No pudimos completar la operación. Inténtalo de nuevo más tarde.';
}

bool _esDeConexion(DioExceptionType tipo) =>
    tipo == DioExceptionType.connectionError ||
    tipo == DioExceptionType.connectionTimeout ||
    tipo == DioExceptionType.sendTimeout ||
    tipo == DioExceptionType.receiveTimeout;

/// `mm:ss` para el cronómetro del intento.
String formatearTiempo(Duration d) {
  final total = d.isNegative ? 0 : d.inSeconds;
  final m = (total ~/ 60).toString().padLeft(2, '0');
  final s = (total % 60).toString().padLeft(2, '0');
  return '$m:$s';
}

/// El puntaje sin decimales sobrantes: `50`, `66.7`.
String formatearPuntaje(double p) =>
    p == p.roundToDouble() ? p.toStringAsFixed(0) : p.toStringAsFixed(1);
