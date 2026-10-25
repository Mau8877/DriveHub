import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/pagos/services/comprobantes_service.dart';

import '../../helpers/adaptador_de_bytes.dart';

void main() {
  late ComprobantesService servicio;

  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    servicio = ComprobantesService();
  });

  test('pide GET /comprobantes/{codigo}/pdf como bytes y devuelve esos bytes', () async {
    final adaptador = AdaptadorDeBytes.pdf([37, 80, 68, 70, 9, 8, 7]);
    ApiClient.instance.httpClientAdapter = adaptador;

    final bytes = await servicio.descargarPdf('c-123');

    final peticion = adaptador.peticiones.single;
    expect(peticion.method, 'GET');
    expect(peticion.path, '/comprobantes/c-123/pdf');
    expect(peticion.responseType, ResponseType.bytes);
    expect(bytes, [37, 80, 68, 70, 9, 8, 7]);
  });

  test('ERR_PAG_14 (en preparación) llega como ApiException', () async {
    ApiClient.instance.httpClientAdapter = AdaptadorDeBytes.error(409, 'ERR_PAG_14');

    await expectLater(
      servicio.descargarPdf('c1'),
      throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_PAG_14')),
    );
  });

  test('ERR_PAG_13 (no existe o no es tuyo) llega como ApiException', () async {
    ApiClient.instance.httpClientAdapter = AdaptadorDeBytes.error(404, 'ERR_PAG_13');

    await expectLater(
      servicio.descargarPdf('c1'),
      throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_PAG_13')),
    );
  });

  test('un error de red se relanza tal cual', () async {
    ApiClient.instance.httpClientAdapter = _SinRed();

    await expectLater(servicio.descargarPdf('c1'), throwsA(isA<DioException>()));
  });
}

class _SinRed extends AdaptadorDeBytes {
  _SinRed() : super.pdf([1]);

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<Uint8List>? requestStream, Future<void>? cancelFuture) async {
    throw DioException.connectionError(requestOptions: options, reason: 'sin red');
  }
}
