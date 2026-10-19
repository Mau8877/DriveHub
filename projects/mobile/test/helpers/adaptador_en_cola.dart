import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';

/// Respuesta simulada del backend: status HTTP y cuerpo (el sobre estándar).
class RespuestaFalsa {
  final int status;
  final Map<String, dynamic> cuerpo;

  const RespuestaFalsa(this.status, this.cuerpo);
}

/// Adaptador de Dio para tests: responde en orden (la última se repite) y
/// guarda las peticiones que recibió.
class AdaptadorEnCola implements HttpClientAdapter {
  final List<RespuestaFalsa> respuestas;
  final List<RequestOptions> peticiones = [];

  AdaptadorEnCola(this.respuestas);

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    peticiones.add(options);
    final r = respuestas.length > 1 ? respuestas.removeAt(0) : respuestas.first;
    return ResponseBody.fromString(
      jsonEncode(r.cuerpo),
      r.status,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

Map<String, dynamic> exito(Map<String, dynamic> data) =>
    {'status': 'success', 'data': data, 'message': 'ok', 'timestamp': 'x'};

Map<String, dynamic> fallo(String codigo, [String mensaje = 'mensaje']) =>
    {'status': 'failed', 'data': null, 'message': mensaje, 'timestamp': 'x', 'error': codigo};

Map<String, dynamic> sesionJson(String access, String refresh, {int expira = 900}) => exito({
      'accessToken': access,
      'refreshToken': refresh,
      'expiraEnSegundos': expira,
      'usuario': {
        'codigo': 'u1',
        'nombres': 'Ana',
        'apellidos': 'Pérez',
        'roles': ['ESTUDIANTE'],
        'escuelaCodigo': 'e1',
      },
    });
