import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/pagos/models/estado_pago_visible.dart';
import 'package:mobile/features/pagos/models/motivo_reembolso.dart';
import 'package:mobile/features/pagos/services/pagos_service.dart';

import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> _detalleEnProceso() => {
  'status': 'success',
  'data': {
    'codigo': 'c1',
    'planNombre': 'Plan 10 horas',
    'horas': 10,
    'montoUsd': 38.23,
    'montoBs': 450.0,
    'estado': 'REEMBOLSO_EN_PROCESO',
    'creadoEn': '2026-05-04T16:00:00Z',
    'horasRestantes': 0,
    'reembolsable': false,
    'reembolso': null,
    'comprobantes': <Map<String, dynamic>>[],
  },
  'message': 'ok',
  'timestamp': 'x',
};

void main() {
  late PagosService servicio;

  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    servicio = PagosService();
  });

  AdaptadorEnCola responder(int status, Map<String, dynamic> cuerpo) {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(status, cuerpo)]);
    ApiClient.instance.httpClientAdapter = adaptador;
    return adaptador;
  }

  test('POST /pagos/{codigo}/reembolso con el motivo, y devuelve el detalle en proceso', () async {
    final adaptador = responder(202, _detalleEnProceso());

    final detalle = await servicio.solicitarReembolso('c1', MotivoReembolso.compraPorError);

    final peticion = adaptador.peticiones.single;
    expect(peticion.method, 'POST');
    expect(peticion.path, '/pagos/c1/reembolso');
    expect(peticion.data, {'motivo': 'COMPRA_POR_ERROR'});
    expect(detalle.estado, EstadoPagoVisible.reembolsoEnProceso);
  });

  test('con «Otro» envía el texto recortado', () async {
    final adaptador = responder(202, _detalleEnProceso());

    await servicio.solicitarReembolso('c1', MotivoReembolso.otro, motivoDetalle: '  Perdí mi trabajo ');

    expect(adaptador.peticiones.single.data, {'motivo': 'OTRO', 'motivoDetalle': 'Perdí mi trabajo'});
  });

  test('con otro motivo ignora el texto', () async {
    final adaptador = responder(202, _detalleEnProceso());

    await servicio.solicitarReembolso('c1', MotivoReembolso.mudanza, motivoDetalle: 'no se envía');

    expect(adaptador.peticiones.single.data, {'motivo': 'MUDANZA'});
  });

  test('un error de negocio llega como ApiException con su código', () async {
    responder(409, {
      'status': 'failed',
      'data': null,
      'message': 'Ya usaste horas',
      'timestamp': 'x',
      'error': 'ERR_PAG_11',
    });

    await expectLater(
      servicio.solicitarReembolso('c1', MotivoReembolso.mudanza),
      throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_PAG_11')),
    );
  });

  test('cada motivo tiene su valor de la API y su texto en español', () {
    expect(MotivoReembolso.values.map((m) => m.valorApi), [
      'CAMBIO_DE_PLANES',
      'MUDANZA',
      'SIN_TIEMPO',
      'COMPRA_POR_ERROR',
      'PROBLEMAS_ECONOMICOS',
      'INSATISFECHO',
      'OTRO',
    ]);
    expect(MotivoReembolso.otro.texto, 'Otro');
    expect(MotivoReembolso.detalleValido('abcd'), isFalse);
    expect(MotivoReembolso.detalleValido(' abcde '), isTrue);
    expect(MotivoReembolso.detalleValido('x' * 201), isFalse);
  });
}
