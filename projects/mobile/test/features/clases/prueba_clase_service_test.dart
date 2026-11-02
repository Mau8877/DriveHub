import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/clases/models/evaluar_clase_request.dart';
import 'package:mobile/features/clases/models/tipo_prueba.dart';
import 'package:mobile/features/clases/services/clases_service.dart';

import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> _pruebaJson({String tipo = 'MANEJO'}) => {
  'codigo': 'p1',
  'claseCodigo': 'c1',
  'tipo': tipo,
  'criterios': [
    {'codigo': 'espejos', 'descripcion': 'Posición y uso de espejos', 'puntaje': 8, 'maximo': 10},
  ],
  'puntajeTotal': 80.0,
  'resultado': 'APROBADO',
  'observaciones': null,
  'fecha': '2026-11-02',
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

  const pedido = EvaluarClaseRequest(
    claseCodigo: 'c1',
    tipo: TipoPrueba.manejo,
    criterios: [PuntajeDeCriterio(codigo: 'espejos', puntaje: 8)],
  );

  group('prueba de clase (CU18)', () {
    test('catalogoDePruebas pide los criterios al servidor', () async {
      final adaptador = responder(
        200,
        exito({
          'tipos': [
            {
              'tipo': 'MANEJO',
              'etiqueta': 'Manejo',
              'criterios': [
                {'codigo': 'espejos', 'descripcion': 'Posición y uso de espejos', 'maximo': 10.0},
              ],
            },
          ],
        }),
      );

      final catalogo = await servicio.catalogoDePruebas();

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'GET');
      expect(peticion.path, '/pruebas-clase/criterios');
      expect(catalogo.tipos.single.criterios.single.codigo, 'espejos');
    });

    test('evaluarClase manda la clase, el tipo y los puntajes, y entrega la prueba', () async {
      final adaptador = responder(201, exito(_pruebaJson()));

      final prueba = await servicio.evaluarClase(pedido);

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'POST');
      expect(peticion.path, '/pruebas-clase');
      expect(peticion.data, {
        'claseCodigo': 'c1',
        'tipo': 'MANEJO',
        'criterios': [
          {'codigo': 'espejos', 'puntaje': 8.0},
        ],
      });
      expect(prueba.codigo, 'p1');
      expect(prueba.puntajeTotal, 80);
    });

    test('pruebasDeClase filtra por la clase, con el filtro entre corchetes, y entrega la lista', () async {
      final adaptador = responder(200, {
        'status': 'success',
        'data': [_pruebaJson(), _pruebaJson(tipo: 'CIUDAD')],
        'message': 'ok',
        'timestamp': 'x',
        'meta': {'page': 1, 'pageSize': 20, 'totalItems': 2, 'totalPages': 1},
      });

      final pruebas = await servicio.pruebasDeClase('c1');

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'GET');
      expect(peticion.path, '/pruebas-clase');
      expect(peticion.queryParameters, {'filter[claseCodigo]': 'c1', 'pageSize': 20});
      expect(pruebas.map((p) => p.tipo), [TipoPrueba.manejo, TipoPrueba.ciudad]);
    });

    test('un error de negocio llega como ApiException con su código', () async {
      for (final codigo in ['ERR_ACA_39', 'ERR_ACA_40', 'ERR_ACA_41']) {
        responder(codigo == 'ERR_ACA_41' ? 400 : 409, fallo(codigo, 'No se pudo'));

        await expectLater(
          servicio.evaluarClase(pedido),
          throwsA(isA<ApiException>().having((e) => e.code, 'code', codigo)),
        );
      }
    });
  });
}
