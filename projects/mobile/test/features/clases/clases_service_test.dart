import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/models/momento_dia.dart';
import 'package:mobile/features/clases/models/vista_reservas.dart';
import 'package:mobile/features/clases/services/clases_service.dart';

import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> _exitoLista(List<Map<String, dynamic>> data) => {
  'status': 'success',
  'data': data,
  'message': 'ok',
  'timestamp': 'x',
  'meta': {'page': 1, 'pageSize': 100, 'totalItems': data.length, 'totalPages': 1},
};

Map<String, dynamic> _franjaJson({String codigo = 'f1'}) => {
  'codigo': codigo,
  'fecha': '2026-11-02',
  'horaInicio': '10:00',
  'horaFin': '11:00',
  'instructorCodigo': 'i1',
  'instructorNombre': 'Ivan Alfa',
};

Map<String, dynamic> _reservaJson() => {
  'codigo': 'r1',
  'fecha': '2026-11-02',
  'horaInicio': '10:00',
  'horaFin': '11:00',
  'instructorCodigo': 'i1',
  'instructorNombre': 'Ivan Alfa',
  'estado': 'CONFIRMADA',
  'terminada': false,
};

void main() {
  late ClasesService servicio;

  AdaptadorEnCola responder(int status, Map<String, dynamic> cuerpo) {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(status, cuerpo)]);
    ApiClient.instance.httpClientAdapter = adaptador;
    return adaptador;
  }

  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    servicio = ClasesService();
  });

  group('franjasDisponibles', () {
    test('sin filtros solo manda la paginación', () async {
      final adaptador = responder(200, _exitoLista([_franjaJson()]));

      final pagina = await servicio.franjasDisponibles();

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'GET');
      expect(peticion.path, '/franjas/disponibles');
      expect(peticion.queryParameters, {'page': 1, 'pageSize': 100});
      expect(pagina.items.single.instructorNombre, 'Ivan Alfa');
      expect(pagina.totalItems, 1);
    });

    test('manda el día, el momento y el instructor', () async {
      final adaptador = responder(200, _exitoLista([]));

      await servicio.franjasDisponibles(
        fecha: DateTime(2026, 11, 2),
        momento: MomentoDia.tarde,
        instructorCodigo: 'i1',
        page: 2,
        pageSize: 10,
      );

      expect(adaptador.peticiones.single.queryParameters, {
        'page': 2,
        'pageSize': 10,
        'filter[fecha]': '2026-11-02',
        'filter[horaDesde]': '12:00',
        'filter[horaHasta]': '18:00',
        'filter[instructor]': 'i1',
      });
    });

    test('la noche manda solo la hora desde, y los filtros nulos no viajan', () async {
      final adaptador = responder(200, _exitoLista([]));

      await servicio.franjasDisponibles(momento: MomentoDia.noche);

      final parametros = adaptador.peticiones.single.queryParameters;
      expect(parametros['filter[horaDesde]'], '18:00');
      expect(parametros.containsKey('filter[horaHasta]'), isFalse);
      expect(parametros.containsKey('filter[fecha]'), isFalse);
      expect(parametros.containsKey('filter[instructor]'), isFalse);
    });

    test('un error del backend llega como ApiException con su código', () async {
      responder(503, fallo('ERR_ACA_17', 'No pudimos obtener los datos'));

      await expectLater(
        servicio.franjasDisponibles(),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_17')),
      );
    });
  });

  group('instructoresDisponibles', () {
    test('pide los instructores y arma la lista', () async {
      final adaptador = responder(200, _exitoLista([
        {'codigo': 'i1', 'nombre': 'Ana Alfa'},
        {'codigo': 'i2', 'nombre': 'Beto Beta'},
      ]));

      final instructores = await servicio.instructoresDisponibles(fecha: DateTime(2026, 11, 2));

      final peticion = adaptador.peticiones.single;
      expect(peticion.path, '/franjas/instructores');
      expect(peticion.queryParameters, {'filter[fecha]': '2026-11-02'});
      expect(instructores.map((i) => i.nombre), ['Ana Alfa', 'Beto Beta']);
    });

    test('sin fecha no manda filtros', () async {
      final adaptador = responder(200, _exitoLista([]));

      expect(await servicio.instructoresDisponibles(), isEmpty);
      expect(adaptador.peticiones.single.queryParameters, isEmpty);
    });
  });

  group('reservar', () {
    test('manda los códigos y entrega las reservadas y las rechazadas', () async {
      final adaptador = responder(200, exito({
        'reservadas': [_reservaJson()],
        'rechazadas': [
          {
            'franjaCodigo': 'f2',
            'fecha': '2026-11-02',
            'horaInicio': '11:00',
            'horaFin': '12:00',
            'instructorNombre': 'Ivan Alfa',
            'error': 'ERR_ACA_18',
            'mensaje': 'La franja ya no está disponible',
          },
        ],
        'horasDisponibles': 4,
      }));

      final resultado = await servicio.reservar(['f1', 'f2']);

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'POST');
      expect(peticion.path, '/reservas');
      expect(peticion.data, {'franjaCodigos': ['f1', 'f2']});
      expect(resultado.reservadas.single.estado, EstadoReserva.confirmada);
      expect(resultado.rechazadas.single.laTomoOtro, isTrue);
      expect(resultado.horasDisponibles, 4);
    });

    test('un rechazo parcial no es un error, pero un 400 sí lo es', () async {
      responder(400, fallo('ERR_SYS_01', 'Elige al menos una clase'));

      await expectLater(
        servicio.reservar([]),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_SYS_01')),
      );
    });
  });

  group('reservas', () {
    test('pide las próximas por defecto con la paginación', () async {
      final adaptador = responder(200, _exitoLista([_reservaJson()]));

      final pagina = await servicio.reservas(vista: VistaReservas.proximas);

      final peticion = adaptador.peticiones.single;
      expect(peticion.path, '/reservas');
      expect(peticion.queryParameters, {'page': 1, 'pageSize': 50, 'filter[vista]': 'proximas'});
      expect(pagina.items.single.codigo, 'r1');
    });

    test('pide el historial', () async {
      final adaptador = responder(200, _exitoLista([]));

      await servicio.reservas(vista: VistaReservas.historial, page: 2, pageSize: 20);

      expect(adaptador.peticiones.single.queryParameters, {
        'page': 2,
        'pageSize': 20,
        'filter[vista]': 'historial',
      });
    });
  });
}
