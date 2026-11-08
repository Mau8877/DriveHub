import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/network/transporte.dart';
import 'package:mobile/features/notificaciones/models/preferencias_notificacion.dart';
import 'package:mobile/features/notificaciones/providers/bandeja_providers.dart';
import 'package:mobile/features/notificaciones/providers/notificaciones_providers.dart';
import 'package:mobile/features/notificaciones/providers/transporte_provider.dart';
import 'package:mobile/features/notificaciones/services/notificaciones_graphql_service.dart';
import 'package:mobile/features/notificaciones/services/preferencias_graphql_service.dart';

import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> _aviso(String codigo, {bool leida = false}) => {
  'codigoNotificacion': codigo,
  'tipo': 'PAGO',
  'titulo': 'Pago recibido',
  'mensaje': 'Recibimos tu pago',
  'leida': leida,
  'creadaEn': '2026-11-02T14:30:00.000Z',
};

Map<String, dynamic> _prefs({String? desde, String? hasta, bool push = true}) => {
  'canales': {'email': true, 'push': push, 'inApp': true},
  'tipos': {'RESERVA': true, 'PAGO': true, 'EXAMEN': false, 'RECORDATORIO': true},
  'silencioDesde': desde,
  'silencioHasta': hasta,
  'email': 'ana@gmail.com',
  'actualizadoEn': '2026-11-02T14:30:00.000Z',
};

Map<String, dynamic> _datos(Map<String, dynamic> data) => {'data': data};

Map<String, dynamic> _error(String codigo, String mensaje) => {
  'errors': [
    {
      'message': mensaje,
      'extensions': {'code': codigo},
    },
  ],
  'data': null,
};

void main() {
  AdaptadorEnCola responder(Map<String, dynamic> cuerpo, {int status = 200}) {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(status, cuerpo)]);
    ApiClient.instance.httpClientAdapter = adaptador;
    return adaptador;
  }

  Map<String, dynamic> variablesDe(AdaptadorEnCola a) =>
      (a.peticiones.single.data as Map<String, dynamic>)['variables'] as Map<String, dynamic>;

  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    ApiClient.configurarSesion(obtenerToken: () => 'token', renovar: () async => null);
  });

  group('NotificacionesGraphqlService', () {
    late NotificacionesGraphqlService servicio;
    setUp(() => servicio = NotificacionesGraphqlService());

    test('listar pide la bandeja con la paginación y devuelve la página con su meta', () async {
      final adaptador = responder(
        _datos({
          'notificaciones': {
            'items': [_aviso('n1'), _aviso('n2', leida: true)],
            'meta': {'page': 2, 'pageSize': 5, 'totalItems': 7, 'totalPages': 2},
          },
        }),
      );

      final pagina = await servicio.listar(page: 2, pageSize: 5);

      expect(pagina.items.map((n) => n.codigoNotificacion), ['n1', 'n2']);
      expect(pagina.items[1].leida, true);
      expect((pagina.page, pagina.pageSize, pagina.totalItems, pagina.totalPages), (2, 5, 7, 2));
      expect(variablesDe(adaptador), {'page': 2, 'pageSize': 5, 'leida': null, 'tipo': null});
    });

    test('listar manda los filtros solo si se usan', () async {
      final adaptador = responder(
        _datos({
          'notificaciones': {
            'items': <dynamic>[],
            'meta': {'page': 1, 'pageSize': 20, 'totalItems': 0, 'totalPages': 1},
          },
        }),
      );

      await servicio.listar(soloNoLeidas: true, tipo: TipoNotificacion.pago);

      expect(variablesDe(adaptador), {'page': 1, 'pageSize': 20, 'leida': false, 'tipo': 'PAGO'});
    });

    test('listar de una bandeja vacía devuelve una página sin avisos', () async {
      responder(
        _datos({
          'notificaciones': {
            'items': <dynamic>[],
            'meta': {'page': 1, 'pageSize': 20, 'totalItems': 0, 'totalPages': 1},
          },
        }),
      );

      final pagina = await servicio.listar();

      expect(pagina.items, isEmpty);
      expect(pagina.totalPages, 1);
    });

    test('un tipo de aviso que la app no conoce no rompe el listado', () async {
      responder(
        _datos({
          'notificaciones': {
            'items': [
              {..._aviso('n1'), 'tipo': 'NUEVO'},
            ],
            'meta': {'page': 1, 'pageSize': 20, 'totalItems': 1, 'totalPages': 1},
          },
        }),
      );

      final pagina = await servicio.listar();

      expect(pagina.items.single.tipo, isNull);
      expect(pagina.items.single.etiquetaTipo, 'Aviso');
    });

    test('contarNoLeidas devuelve el número', () async {
      responder(_datos({'conteoNoLeidas': 3}));

      expect(await servicio.contarNoLeidas(), 3);
    });

    test('leer manda el código y devuelve el aviso ya leído', () async {
      final adaptador = responder(_datos({'marcarLeida': _aviso('n1', leida: true)}));

      final aviso = await servicio.leer('n1');

      expect(aviso.leida, true);
      expect(variablesDe(adaptador), {'codigo': 'n1'});
    });

    test('leerTodas devuelve cuántos eran', () async {
      responder(_datos({'marcarTodasLeidas': 2}));

      expect(await servicio.leerTodas(), 2);
    });

    test('un aviso inexistente llega como ApiException ERR_NOT_04, igual que por REST', () async {
      responder(_error('ERR_NOT_04', 'El aviso no existe'));

      await expectLater(
        servicio.leer('nx'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_NOT_04')),
      );
    });

    test('un tamaño de página inválido llega como ApiException ERR_SYS_01', () async {
      responder(_error('ERR_SYS_01', 'El tamaño de página debe ser de 1 a 100'));

      await expectLater(
        servicio.listar(pageSize: 101),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_SYS_01')),
      );
    });
  });

  group('PreferenciasGraphqlService', () {
    late PreferenciasGraphqlService servicio;
    setUp(() => servicio = PreferenciasGraphqlService());

    test('obtener devuelve las preferencias guardadas', () async {
      responder(_datos({'preferencias': _prefs(desde: '22:00', hasta: '07:00')}));

      final prefs = await servicio.obtener();

      expect(prefs.push, true);
      expect(prefs.tipos[TipoNotificacion.examen], false);
      expect((prefs.silencioDesde, prefs.silencioHasta), ('22:00', '07:00'));
    });

    test('obtener de una persona que nunca guardó entrega la configuración por defecto', () async {
      responder(
        _datos({
          'preferencias': {
            'canales': {'email': true, 'push': true, 'inApp': true},
            'tipos': {'RESERVA': true, 'PAGO': true, 'EXAMEN': true, 'RECORDATORIO': true},
            'silencioDesde': null,
            'silencioHasta': null,
            'email': null,
            'actualizadoEn': null,
          },
        }),
      );

      final prefs = await servicio.obtener();

      expect(prefs, PreferenciasNotificacion.porDefecto());
    });

    test('guardar manda la configuración completa con el correo, con la forma del PUT', () async {
      final adaptador = responder(_datos({'guardarPreferencias': _prefs(push: false)}));
      final aGuardar = PreferenciasNotificacion.porDefecto().conCanal(push: false);

      final guardadas = await servicio.guardar(aGuardar, email: 'ana@gmail.com');

      expect(variablesDe(adaptador), {'entrada': aGuardar.toJson(email: 'ana@gmail.com')});
      expect(guardadas.push, false);
    });

    test('guardar sin franja manda null en las dos horas (sin silencio)', () async {
      final adaptador = responder(_datos({'guardarPreferencias': _prefs()}));

      await servicio.guardar(PreferenciasNotificacion.porDefecto(), email: 'ana@gmail.com');

      final entrada = variablesDe(adaptador)['entrada'] as Map<String, dynamic>;
      expect(entrada['silencioDesde'], isNull);
      expect(entrada['silencioHasta'], isNull);
      expect(entrada['email'], 'ana@gmail.com');
    });

    test('una franja incompleta llega como ApiException ERR_NOT_03', () async {
      responder(_error('ERR_NOT_03', 'Elige las dos horas de silencio, o ninguna'));

      await expectLater(
        servicio.guardar(PreferenciasNotificacion.porDefecto(), email: 'a@b.co'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_NOT_03')),
      );
    });
  });

  group('transporte', () {
    test('sin valor o con un valor desconocido se usa GraphQL', () {
      expect(transporteDesde(null), Transporte.graphql);
      expect(transporteDesde(''), Transporte.graphql);
      expect(transporteDesde('otro'), Transporte.graphql);
      expect(transporteDesde('graphql'), Transporte.graphql);
    });

    test('rest vuelve a las rutas de siempre, sin importar mayúsculas ni espacios', () {
      expect(transporteDesde('rest'), Transporte.rest);
      expect(transporteDesde(' REST '), Transporte.rest);
    });

    test('el provider lee NOTIFICACIONES_TRANSPORTE del .env', () {
      dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1\nNOTIFICACIONES_TRANSPORTE=rest');
      final container = ProviderContainer();
      addTearDown(container.dispose);

      expect(container.read(transporteNotificacionesProvider), Transporte.rest);
    });

    test('sin la variable en el .env, el provider da GraphQL', () {
      final container = ProviderContainer();
      addTearDown(container.dispose);

      expect(container.read(transporteNotificacionesProvider), Transporte.graphql);
    });

    test('con GraphQL, los providers entregan los servicios de GraphQL', () {
      final container = ProviderContainer(
        overrides: [transporteNotificacionesProvider.overrideWithValue(Transporte.graphql)],
      );
      addTearDown(container.dispose);

      expect(container.read(notificacionesServiceProvider), isA<NotificacionesGraphqlService>());
      expect(container.read(preferenciasServiceProvider), isA<PreferenciasGraphqlService>());
    });

    test('con rest, los providers entregan los servicios de REST de siempre', () {
      final container = ProviderContainer(
        overrides: [transporteNotificacionesProvider.overrideWithValue(Transporte.rest)],
      );
      addTearDown(container.dispose);

      expect(container.read(notificacionesServiceProvider), isNot(isA<NotificacionesGraphqlService>()));
      expect(container.read(preferenciasServiceProvider), isNot(isA<PreferenciasGraphqlService>()));
    });
  });
}
