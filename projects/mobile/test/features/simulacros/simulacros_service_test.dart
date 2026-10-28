import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/simulacros/services/simulacros_service.dart';

import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> _sobre(Object? data, {Map<String, dynamic>? meta}) => {
  'status': 'success',
  'data': data,
  'message': 'ok',
  'timestamp': 'x',
  'meta': ?meta,
};

Map<String, dynamic> _resultadoJson() => {
  'codigo': 'i1',
  'simulacroNombre': 'Básico',
  'estado': 'FINALIZADO',
  'iniciadoEn': '2026-11-02T16:00:00Z',
  'finalizadoEn': '2026-11-02T16:10:00Z',
  'aciertos': 1,
  'total': 1,
  'puntaje': 100.0,
  'aprobado': true,
  'puntajeAprobacion': 70.0,
  'preguntas': [
    {
      'codigo': 'p1',
      'enunciado': '¿PARE?',
      'tipo': 'OPCION_UNICA',
      'opciones': [
        {'id': 'A', 'texto': 'Detenerse', 'correcta': true},
      ],
      'elegidas': ['A'],
      'correcta': true,
      'explicacion': null,
    },
  ],
};

void main() {
  late SimulacrosService servicio;

  AdaptadorEnCola responder(int status, Map<String, dynamic> cuerpo) {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(status, cuerpo)]);
    ApiClient.instance.httpClientAdapter = adaptador;
    return adaptador;
  }

  setUp(() {
    dotenv.loadFromString(
      envString: 'API_BASE_URL=http://localhost:8080/api/v1',
    );
    servicio = SimulacrosService();
  });

  test('disponibles pide los simulacros de la escuela y los lee', () async {
    final adaptador = responder(
      200,
      _sobre([
        {
          'codigo': 's1',
          'nombre': 'Básico',
          'categoria': 'B',
          'duracionMin': 20,
          'nroPreguntas': 5,
          'puntajeAprobacion': 70,
        },
      ]),
    );

    final lista = await servicio.disponibles();

    expect(adaptador.peticiones.single.method, 'GET');
    expect(adaptador.peticiones.single.path, '/simulacros/disponibles');
    expect(lista.single.nombre, 'Básico');
    expect(lista.single.puntajeAprobacion, 70.0);
  });

  test(
    'iniciar manda el simulacro y lee las preguntas sin respuestas correctas',
    () async {
      final adaptador = responder(
        201,
        _sobre({
          'codigo': 'i1',
          'simulacroNombre': 'Básico',
          'duracionMin': 20,
          'iniciadoEn': '2026-11-02T16:00:00Z',
          'venceEn': '2026-11-02T16:20:00Z',
          'preguntas': [
            {
              'codigo': 'p1',
              'enunciado': '¿PARE?',
              'tipo': 'VERDADERO_FALSO',
              'opciones': [
                {'id': 'A', 'texto': 'Verdadero'},
                {'id': 'B', 'texto': 'Falso'},
              ],
            },
          ],
        }),
      );

      final intento = await servicio.iniciar('s1');

      expect(adaptador.peticiones.single.method, 'POST');
      expect(adaptador.peticiones.single.path, '/intentos-simulacro');
      expect(adaptador.peticiones.single.data, {'simulacroCodigo': 's1'});
      expect(intento.preguntas.single.opciones.map((o) => o.id), ['A', 'B']);
      expect(intento.preguntas.single.tipo.admiteVarias, isFalse);
    },
  );

  test(
    'finalizar manda solo lo marcado, con las opciones ordenadas, y lee la corrección',
    () async {
      final adaptador = responder(200, _sobre(_resultadoJson()));

      final resultado = await servicio.finalizar('i1', {
        'p1': {'B', 'A'},
      });

      expect(
        adaptador.peticiones.single.path,
        '/intentos-simulacro/i1/finalizar',
      );
      expect(adaptador.peticiones.single.data, {
        'respuestas': [
          {
            'preguntaCodigo': 'p1',
            'elegidas': ['A', 'B'],
          },
        ],
      });
      expect(resultado.aprobado, isTrue);
      expect(resultado.preguntas.single.opciones.single.correcta, isTrue);
      expect(resultado.preguntas.single.explicacion, isNull);
    },
  );

  test('historial pide la página y lee los resúmenes', () async {
    final adaptador = responder(
      200,
      _sobre(
        [
          {
            'codigo': 'i1',
            'simulacroNombre': 'Básico',
            'estado': 'EXPIRADO',
            'iniciadoEn': '2026-11-02T16:00:00Z',
            'finalizadoEn': null,
            'aciertos': 0,
            'total': 5,
            'puntaje': 0,
            'aprobado': false,
          },
        ],
        meta: {'page': 1, 'pageSize': 50, 'totalItems': 1, 'totalPages': 1},
      ),
    );

    final pagina = await servicio.historial();

    expect(adaptador.peticiones.single.queryParameters, {
      'page': 1,
      'pageSize': 50,
    });
    expect(pagina.items.single.expirado, isTrue);
    expect(pagina.totalItems, 1);
  });

  test('un error de negocio llega como ApiException con su código', () async {
    responder(409, {
      'status': 'failed',
      'data': null,
      'message': 'Ya tienes un intento en curso',
      'timestamp': 'x',
      'error': 'ERR_ACA_29',
    });

    await expectLater(
      servicio.iniciar('s1'),
      throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_29')),
    );
  });
}
