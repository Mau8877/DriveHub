import 'dart:typed_data';

import 'package:dio/dio.dart';

import 'adaptador_en_cola.dart';

/// Adaptador de Dio que puede perder y recuperar la red: con [conRed] en `false` toda petición falla como un
/// teléfono sin internet (`connectionError`); con `true` responde en orden como [AdaptadorEnCola].
class AdaptadorConRed implements HttpClientAdapter {
  bool conRed;

  /// Si se indica, la red se cae sola cuando ya llegaron tantas peticiones al servidor.
  int? caeDespuesDe;
  final AdaptadorEnCola _respuestas;

  AdaptadorConRed({this.conRed = true, List<RespuestaFalsa>? respuestas})
    : _respuestas = AdaptadorEnCola(respuestas ?? [RespuestaFalsa(200, exito({}))]);

  /// Las peticiones que SÍ llegaron al servidor, en orden.
  List<RequestOptions> get peticiones => _respuestas.peticiones;

  /// Reemplaza las respuestas pendientes (la última se repite).
  void responder(List<RespuestaFalsa> respuestas) {
    _respuestas.respuestas
      ..clear()
      ..addAll(respuestas);
  }

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) {
    final limite = caeDespuesDe;
    if (limite != null && peticiones.length >= limite) conRed = false;
    if (!conRed) {
      throw DioException.connectionError(requestOptions: options, reason: 'sin red');
    }
    return _respuestas.fetch(options, requestStream, cancelFuture);
  }

  @override
  void close({bool force = false}) {}
}
