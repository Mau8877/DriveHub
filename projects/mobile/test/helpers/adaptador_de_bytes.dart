import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';

/// Adaptador de Dio para tests de descargas (CU30): responde con bytes en crudo, como un archivo, y guarda las
/// peticiones que recibió.
class AdaptadorDeBytes implements HttpClientAdapter {
  final int status;
  final Uint8List cuerpo;
  final String tipo;
  final List<RequestOptions> peticiones = [];

  AdaptadorDeBytes.pdf(List<int> bytes)
    : status = 200,
      cuerpo = Uint8List.fromList(bytes),
      tipo = 'application/pdf';

  /// Un error del backend: el sobre JSON de siempre, con su código.
  AdaptadorDeBytes.error(this.status, String codigo, [String mensaje = 'mensaje'])
    : cuerpo = Uint8List.fromList(
        utf8.encode(
          jsonEncode({'status': 'failed', 'data': null, 'message': mensaje, 'timestamp': 'x', 'error': codigo}),
        ),
      ),
      tipo = Headers.jsonContentType;

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<Uint8List>? requestStream, Future<void>? cancelFuture) async {
    peticiones.add(options);
    return ResponseBody.fromBytes(cuerpo, status, headers: {
      Headers.contentTypeHeader: [tipo],
    });
  }

  @override
  void close({bool force = false}) {}
}
