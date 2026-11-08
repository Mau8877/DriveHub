import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/offline/almacen_offline.dart';
import 'package:mobile/core/offline/modelos_offline.dart';
import 'package:mobile/core/offline/nucleo_offline.dart';

import '../../helpers/adaptador_con_red.dart';
import '../../helpers/adaptador_en_cola.dart';

void main() {
  late AlmacenEnMemoria almacen;
  late AdaptadorConRed red;
  late NucleoOffline nucleo;
  late bool servidorResponde;
  late bool conToken;
  late int cambiosDeDatos;

  NucleoOffline crearNucleo() => NucleoOffline(
    almacen: almacen,
    dio: () => ApiClient.instance,
    hayToken: () => conToken,
    sondeo: () async => servidorResponde,
    cadaSondeo: const Duration(milliseconds: 10),
  );

  setUp(() async {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    ApiClient.configurarSesion(obtenerToken: () => 'token', renovar: () async => null);
    almacen = AlmacenEnMemoria();
    red = AdaptadorConRed();
    servidorResponde = false;
    conToken = true;
    cambiosDeDatos = 0;
    ApiClient.instance.httpClientAdapter = red;
    nucleo = crearNucleo()..alCambiarDatos = () => cambiosDeDatos++;
    ApiClient.configurarOffline(nucleo);
    await nucleo.abrirSesion('u1');
  });

  tearDown(() {
    ApiClient.configurarOffline(null);
    nucleo.dispose();
  });

  Future<Response<dynamic>> leer(String ruta, {Map<String, dynamic>? query}) =>
      ApiClient.instance.get<dynamic>(ruta, queryParameters: query);

  Future<Response<dynamic>> escribir(String descripcion, {Object? cuerpo = const {'a': 1}}) =>
      ApiClient.instance.post<dynamic>('/reservas', data: cuerpo, options: OpcionesOffline.escritura(descripcion));

  group('lecturas', () {
    test('una lectura correcta se guarda y sin red se responde con lo guardado', () async {
      red.responder([RespuestaFalsa(200, exito({'saldo': 3}))]);
      await leer('/billeteras-horas/mia');
      await pumpEventQueue();

      red.conRed = false;
      final respuesta = await leer('/billeteras-horas/mia');

      expect(respuesta.data, {'saldo': 3});
      expect(respuesta.extra[OpcionesOffline.claveDesdeCache], true);
      expect(nucleo.sinConexion, true);
    });

    test('los parámetros en otro orden son la misma lectura', () async {
      red.responder([RespuestaFalsa(200, exito({'x': 1}))]);
      await leer('/franjas', query: {'page': 1, 'pageSize': 20});
      await pumpEventQueue();

      red.conRed = false;
      final respuesta = await leer('/franjas', query: {'pageSize': 20, 'page': 1});

      expect(respuesta.data, {'x': 1});
    });

    test('sin nada guardado el error de red sigue su camino', () async {
      red.conRed = false;

      await expectLater(
        leer('/billeteras-horas/mia'),
        throwsA(isA<DioException>().having((e) => e.type, 'type', DioExceptionType.connectionError)),
      );
      expect(nucleo.sinConexion, true);
    });

    test('lo guardado de una persona no se le muestra a otra', () async {
      red.responder([RespuestaFalsa(200, exito({'saldo': 3}))]);
      await leer('/billeteras-horas/mia');
      await pumpEventQueue();

      await nucleo.cerrarSesion();
      await nucleo.abrirSesion('u2');
      red.conRed = false;

      await expectLater(leer('/billeteras-horas/mia'), throwsA(isA<DioException>()));
    });

    test('las rutas de /auth/ no se guardan', () async {
      red.responder([RespuestaFalsa(200, exito({'accessToken': 'secreto'}))]);
      await leer('/auth/sesion');
      await pumpEventQueue();

      red.conRed = false;
      await expectLater(leer('/auth/sesion'), throwsA(isA<DioException>()));
    });

    test('un error del servidor no es «sin conexión» ni se responde desde lo guardado', () async {
      red.responder([RespuestaFalsa(200, exito({'saldo': 3}))]);
      await leer('/billeteras-horas/mia');
      await pumpEventQueue();

      red.responder([RespuestaFalsa(404, fallo('ERR_PAG_01'))]);
      await expectLater(leer('/billeteras-horas/mia'), throwsA(isA<DioException>()));
      expect(nucleo.sinConexion, false);
    });

    test('al cerrar sesión se borra lo guardado', () async {
      red.responder([RespuestaFalsa(200, exito({'saldo': 3}))]);
      await leer('/billeteras-horas/mia');
      await pumpEventQueue();

      await nucleo.cerrarSesion();
      await nucleo.abrirSesion('u1');
      red.conRed = false;

      await expectLater(leer('/billeteras-horas/mia'), throwsA(isA<DioException>()));
    });
  });

  group('escrituras', () {
    test('una escritura marcada, sin red, se encola y avisa con AccionEncolada', () async {
      red.conRed = false;

      await expectLater(
        escribir('Reserva de 2 clases'),
        throwsA(
          isA<DioException>().having(
            (e) => e.error,
            'error',
            isA<AccionEncolada>().having((a) => a.descripcion, 'descripcion', 'Reserva de 2 clases'),
          ),
        ),
      );

      expect(nucleo.pendientes, hasLength(1));
      expect(nucleo.pendientes.single.ruta, '/reservas');
      expect(nucleo.pendientes.single.cuerpo, {'a': 1});
      expect(red.peticiones, isEmpty);
    });

    test('una escritura sin marca no se encola: falla como siempre', () async {
      red.conRed = false;

      await expectLater(
        ApiClient.instance.post<dynamic>('/pagos', data: {'a': 1}),
        throwsA(isA<DioException>().having((e) => e.error, 'error', isNot(isA<AccionEncolada>()))),
      );
      expect(nucleo.pendientes, isEmpty);
    });

    test('sin sesión no se encola nada', () async {
      await nucleo.cerrarSesion();
      red.conRed = false;

      await expectLater(escribir('Algo'), throwsA(isA<DioException>()));
      expect(nucleo.pendientes, isEmpty);
    });

    test('con red, una escritura marcada sale normal y no queda pendiente', () async {
      red.responder([RespuestaFalsa(200, exito({'ok': true}))]);

      final respuesta = await escribir('Algo');

      expect(respuesta.data, {'ok': true});
      expect(nucleo.pendientes, isEmpty);
    });

    test('la cola sobrevive a reiniciar la app', () async {
      red.conRed = false;
      await expectLater(escribir('Primera'), throwsA(anything));

      final otro = crearNucleo();
      addTearDown(otro.dispose);
      await otro.abrirSesion('u1');

      expect(otro.pendientes.map((a) => a.descripcion), ['Primera']);
    });
  });

  group('sincronizar', () {
    Future<void> encolarTres() async {
      red.conRed = false;
      for (final nombre in ['Una', 'Dos', 'Tres']) {
        await expectLater(escribir(nombre, cuerpo: {'n': nombre}), throwsA(anything));
      }
      red.conRed = true;
    }

    test('envía las pendientes de a una y en el orden en que se hicieron', () async {
      await encolarTres();
      red.responder([RespuestaFalsa(200, exito({}))]);

      await nucleo.sincronizar();

      expect(red.peticiones.map((p) => (p.data as Map)['n']), ['Una', 'Dos', 'Tres']);
      expect(nucleo.pendientes, isEmpty);
      expect(cambiosDeDatos, 1);
    });

    test('lo que el servidor rechaza se descarta con aviso y las demás siguen', () async {
      await encolarTres();
      red.responder([
        RespuestaFalsa(200, exito({})),
        RespuestaFalsa(409, fallo('ERR_ACA_10', 'La franja ya fue reservada')),
        RespuestaFalsa(200, exito({})),
      ]);

      await nucleo.sincronizar();

      expect(red.peticiones, hasLength(3));
      expect(nucleo.pendientes, isEmpty);
      final rechazada = nucleo.estado.value.rechazadas.single;
      expect(rechazada.descripcion, 'Dos');
      expect(rechazada.codigo, 'ERR_ACA_10');
      expect(rechazada.motivo, 'La franja ya fue reservada');
    });

    test('si se cae la red a la mitad, se detiene y conserva lo que falta', () async {
      await encolarTres();
      red.responder([RespuestaFalsa(200, exito({}))]);
      // * La primera sale; antes de la segunda se pierde la red
      red.caeDespuesDe = 1;

      await nucleo.sincronizar();

      expect(red.peticiones, hasLength(1));
      expect(nucleo.pendientes.map((a) => a.descripcion), ['Dos', 'Tres']);
      expect(nucleo.estado.value.rechazadas, isEmpty);
      expect(nucleo.sinConexion, true);
    });

    test('un 5xx o un 429 no descarta: se reintenta más tarde', () async {
      await encolarTres();
      red.responder([RespuestaFalsa(503, fallo('ERR_SYS_00'))]);

      await nucleo.sincronizar();

      expect(nucleo.pendientes, hasLength(3));
      expect(nucleo.estado.value.rechazadas, isEmpty);
      expect(red.peticiones, hasLength(1));
    });

    test('un 401 (sesión vencida) no descarta nada', () async {
      await encolarTres();
      red.responder([RespuestaFalsa(401, fallo('ERR_SYS_04'))]);

      await nucleo.sincronizar();

      expect(nucleo.pendientes, hasLength(3));
      expect(nucleo.estado.value.rechazadas, isEmpty);
    });

    test('sin token no envía nada', () async {
      await encolarTres();
      conToken = false;

      await nucleo.sincronizar();

      expect(red.peticiones, isEmpty);
      expect(nucleo.pendientes, hasLength(3));
    });

    test('lo enviado desde la cola no vuelve a encolarse si falla la red', () async {
      await encolarTres();
      red.conRed = false;

      await nucleo.sincronizar();

      expect(nucleo.pendientes, hasLength(3));
    });

    test('los avisos de rechazo se guardan y se pueden cerrar', () async {
      await encolarTres();
      red.responder([RespuestaFalsa(400, fallo('ERR_SYS_01'))]);
      await nucleo.sincronizar();
      expect(nucleo.estado.value.rechazadas, hasLength(3));

      final otro = crearNucleo();
      addTearDown(otro.dispose);
      await otro.abrirSesion('u1');
      expect(otro.estado.value.rechazadas, hasLength(3));

      await otro.descartarRechazada(otro.estado.value.rechazadas.first.id);
      expect(otro.estado.value.rechazadas, hasLength(2));
    });
  });

  group('resultados para la persona', () {
    test('las acciones cumplidas quedan registradas junto con las rechazadas', () async {
      red.conRed = false;
      for (final nombre in ['Una', 'Dos']) {
        await expectLater(escribir(nombre), throwsA(anything));
      }
      red.conRed = true;
      red.responder([RespuestaFalsa(200, exito({})), RespuestaFalsa(409, fallo('ERR_ACA_10', 'Ya no está libre'))]);

      await nucleo.sincronizar();

      expect(nucleo.estado.value.completadas.map((c) => c.descripcion), ['Una']);
      expect(nucleo.estado.value.rechazadas.map((r) => r.descripcion), ['Dos']);
      expect(nucleo.estado.value.hayResultados, true);
    });

    test('los resultados sobreviven a reiniciar y se cierran todos juntos', () async {
      red.conRed = false;
      await expectLater(escribir('Una'), throwsA(anything));
      red.conRed = true;
      await nucleo.sincronizar();

      final otro = crearNucleo();
      addTearDown(otro.dispose);
      await otro.abrirSesion('u1');
      expect(otro.estado.value.completadas, hasLength(1));

      await otro.descartarResultados();
      expect(otro.estado.value.hayResultados, false);
      final tercero = crearNucleo();
      addTearDown(tercero.dispose);
      await tercero.abrirSesion('u1');
      expect(tercero.estado.value.hayResultados, false);
    });

    test('un 200 con una parte rechazada (reservar varias franjas) avisa cada una que no se cumplió', () async {
      nucleo.verificadores['reservar'] = (respuesta) => [
        for (final f in (respuesta! as Map)['rechazadas'] as List)
          ObservacionRechazo((f as Map)['mensaje'] as String, codigo: f['error'] as String),
      ];
      red.conRed = false;
      await expectLater(
        ApiClient.instance.post<dynamic>(
          '/reservas',
          data: {'franjaCodigos': ['a', 'b']},
          options: OpcionesOffline.escritura('Reserva de 2 clases', tipo: 'reservar'),
        ),
        throwsA(anything),
      );
      red.conRed = true;
      red.responder([
        RespuestaFalsa(
          200,
          exito({
            'rechazadas': [
              {'mensaje': 'La franja ya la tomó otro estudiante', 'error': 'ERR_ACA_18'},
            ],
          }),
        ),
      ]);

      await nucleo.sincronizar();

      expect(nucleo.estado.value.completadas.single.descripcion, 'Reserva de 2 clases');
      final rechazada = nucleo.estado.value.rechazadas.single;
      expect(rechazada.motivo, 'La franja ya la tomó otro estudiante');
      expect(rechazada.codigo, 'ERR_ACA_18');
      expect(rechazada.descripcion, 'Reserva de 2 clases');
    });

    test('un verificador que falla no frena la cola', () async {
      nucleo.verificadores['roto'] = (_) => throw const FormatException('respuesta rara');
      red.conRed = false;
      await expectLater(
        ApiClient.instance.post<dynamic>('/reservas', data: {'a': 1}, options: OpcionesOffline.escritura('X', tipo: 'roto')),
        throwsA(anything),
      );
      red.conRed = true;

      await nucleo.sincronizar();

      expect(nucleo.pendientes, isEmpty);
      expect(nucleo.estado.value.completadas, hasLength(1));
    });
  });

  group('conexión', () {
    test('al volver el servidor, sincroniza solo y pide refrescar los datos', () async {
      red.conRed = false;
      await expectLater(escribir('Pendiente'), throwsA(anything));
      expect(nucleo.sinConexion, true);

      red.conRed = true;
      red.responder([RespuestaFalsa(200, exito({}))]);
      servidorResponde = true;
      await Future<void>.delayed(const Duration(milliseconds: 80));

      expect(nucleo.sinConexion, false);
      expect(nucleo.pendientes, isEmpty);
      expect(red.peticiones, hasLength(1));
      expect(cambiosDeDatos, greaterThanOrEqualTo(1));
    });
  });
}
