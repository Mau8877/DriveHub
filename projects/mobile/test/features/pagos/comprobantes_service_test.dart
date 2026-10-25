import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/pagos/services/comprobantes_service.dart';

import '../../helpers/adaptador_de_bytes.dart';

void main() {
  late Directory temporal;
  late ComprobantesService servicio;

  setUp(() async {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    temporal = await Directory.systemTemp.createTemp('comprobantes_test');
    servicio = ComprobantesService(directorioTemporal: () async => temporal);
  });

  tearDown(() async {
    if (await temporal.exists()) await temporal.delete(recursive: true);
  });

  test('pide GET /comprobantes/{codigo}/pdf como bytes y guarda el archivo con esos bytes', () async {
    final adaptador = AdaptadorDeBytes.pdf([37, 80, 68, 70, 9, 8, 7]);
    ApiClient.instance.httpClientAdapter = adaptador;

    final archivo = await servicio.descargarPdf('c-123');

    final peticion = adaptador.peticiones.single;
    expect(peticion.method, 'GET');
    expect(peticion.path, '/comprobantes/c-123/pdf');
    expect(peticion.responseType, ResponseType.bytes);
    expect(archivo.path, endsWith('comprobante-c-123.pdf'));
    expect(archivo.parent.path, temporal.path);
    expect(await archivo.readAsBytes(), [37, 80, 68, 70, 9, 8, 7]);
  });

  test('descargarlo otra vez sobrescribe el archivo', () async {
    ApiClient.instance.httpClientAdapter = AdaptadorDeBytes.pdf([1, 2, 3, 4, 5]);
    await servicio.descargarPdf('c1');

    ApiClient.instance.httpClientAdapter = AdaptadorDeBytes.pdf([9]);
    final archivo = await servicio.descargarPdf('c1');

    expect(await archivo.readAsBytes(), [9]);
    expect(temporal.listSync(), hasLength(1));
  });

  test('ERR_PAG_14 (en preparación) llega como ApiException y no deja archivo', () async {
    ApiClient.instance.httpClientAdapter = AdaptadorDeBytes.error(409, 'ERR_PAG_14');

    await expectLater(
      servicio.descargarPdf('c1'),
      throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_PAG_14')),
    );
    expect(temporal.listSync(), isEmpty);
  });

  test('ERR_PAG_13 (no existe o no es tuyo) llega como ApiException', () async {
    ApiClient.instance.httpClientAdapter = AdaptadorDeBytes.error(404, 'ERR_PAG_13');

    await expectLater(
      servicio.descargarPdf('c1'),
      throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_PAG_13')),
    );
  });
}
