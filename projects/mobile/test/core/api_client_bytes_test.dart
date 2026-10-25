import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';

import '../helpers/adaptador_de_bytes.dart';
import '../helpers/adaptador_en_cola.dart';

void main() {
  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
  });

  test('una respuesta binaria (un PDF) pasa tal cual, sin desempaquetar', () async {
    final adaptador = AdaptadorDeBytes.pdf([37, 80, 68, 70, 1, 2, 3]);
    ApiClient.instance.httpClientAdapter = adaptador;

    final respuesta = await ApiClient.instance.get<List<int>>(
      '/comprobantes/c1/pdf',
      options: Options(responseType: ResponseType.bytes),
    );

    expect(respuesta.data, [37, 80, 68, 70, 1, 2, 3]);
  });

  test('sigue desempaquetando el sobre JSON de las respuestas normales', () async {
    ApiClient.instance.httpClientAdapter = AdaptadorEnCola([RespuestaFalsa(200, exito({'x': 1}))]);

    final respuesta = await ApiClient.instance.get<dynamic>('/estudiantes');

    expect(respuesta.data, {'x': 1});
  });

  test('un error de una descarga (que pide bytes) llega como ApiException con su código', () async {
    ApiClient.instance.httpClientAdapter = AdaptadorDeBytes.error(409, 'ERR_PAG_14', 'en preparación');

    await expectLater(
      ApiClient.instance.get<List<int>>(
        '/comprobantes/c1/pdf',
        options: Options(responseType: ResponseType.bytes),
      ),
      throwsA(
        isA<DioException>().having(
          (e) => e.error,
          'error',
          isA<ApiException>().having((a) => a.code, 'code', 'ERR_PAG_14').having((a) => a.message, 'message', 'en preparación'),
        ),
      ),
    );
  });

  test('un error que no es un sobre JSON sigue siendo un DioException normal', () async {
    ApiClient.instance.httpClientAdapter = _SinSobre();

    await expectLater(
      ApiClient.instance.get<List<int>>('/x', options: Options(responseType: ResponseType.bytes)),
      throwsA(isA<DioException>().having((e) => e.error, 'error', isNot(isA<ApiException>()))),
    );
  });
}

class _SinSobre extends AdaptadorDeBytes {
  _SinSobre() : super.pdf([1, 2, 3]);

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<Uint8List>? requestStream, Future<void>? cancelFuture) async =>
      ResponseBody.fromBytes(Uint8List.fromList([60, 104, 116, 109, 108, 62]), 502);
}
