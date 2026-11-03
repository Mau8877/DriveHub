import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/clases/models/catalogo_pruebas.dart';
import 'package:mobile/features/clases/models/clase_agenda.dart';
import 'package:mobile/features/clases/models/evaluar_clase_request.dart';
import 'package:mobile/features/clases/models/prueba_clase.dart';
import 'package:mobile/features/clases/models/tipo_prueba.dart';

Map<String, dynamic> _agendaJson({Map<String, dynamic> extra = const {}}) => {
  'codigo': 'r1',
  'fecha': '2026-11-02',
  'horaInicio': '10:00',
  'horaFin': '11:00',
  'estudianteCodigo': 'e1',
  'estudianteNombre': 'Eva Estudiante',
  'estado': 'COMPLETADA',
  ...extra,
};

Map<String, dynamic> _pruebaJson({String? observaciones, String resultado = 'APROBADO'}) => {
  'codigo': 'p1',
  'claseCodigo': 'c1',
  'tipo': 'ESTACIONAMIENTO',
  'criterios': [
    {'codigo': 'entrada', 'descripcion': 'Maniobra de entrada', 'puntaje': 7.5, 'maximo': 10.0},
    {'codigo': 'posicion_final', 'descripcion': 'Posición final', 'puntaje': 10, 'maximo': 10},
  ],
  'puntajeTotal': 87.5,
  'resultado': resultado,
  'observaciones': observaciones,
  'fecha': '2026-11-02',
};

void main() {
  group('TipoPrueba', () {
    test('traduce el valor de la API y un valor desconocido cae en manejo', () {
      expect(TipoPrueba.fromJson('CIUDAD'), TipoPrueba.ciudad);
      expect(TipoPrueba.fromJson('ESTACIONAMIENTO').etiqueta, 'Estacionamiento');
      expect(TipoPrueba.fromJson('OTRO'), TipoPrueba.manejo);
      expect(TipoPrueba.ciudad.valorApi, 'CIUDAD');
    });
  });

  group('CatalogoPruebas', () {
    test('fromJson lee los tipos con sus criterios y máximos', () {
      final catalogo = CatalogoPruebas.fromJson({
        'tipos': [
          {
            'tipo': 'MANEJO',
            'etiqueta': 'Manejo',
            'criterios': [
              {'codigo': 'espejos', 'descripcion': 'Posición y uso de espejos', 'maximo': 10.0},
              {'codigo': 'volante', 'descripcion': 'Control del volante', 'maximo': 10},
            ],
          },
          {'tipo': 'CIUDAD', 'etiqueta': 'Ciudad', 'criterios': <Map<String, dynamic>>[]},
        ],
      });

      expect(catalogo.tipos, hasLength(2));
      expect(catalogo.de(TipoPrueba.manejo)!.criterios.map((c) => c.codigo), ['espejos', 'volante']);
      expect(catalogo.de(TipoPrueba.manejo)!.criterios.first.maximo, 10);
      expect(catalogo.de(TipoPrueba.ciudad)!.etiqueta, 'Ciudad');
      expect(catalogo.de(TipoPrueba.estacionamiento), isNull);
    });
  });

  group('EvaluarClaseRequest', () {
    test('toJson manda la clase, el tipo y cada criterio con su puntaje', () {
      const pedido = EvaluarClaseRequest(
        claseCodigo: 'c1',
        tipo: TipoPrueba.manejo,
        criterios: [
          PuntajeDeCriterio(codigo: 'espejos', puntaje: 8),
          PuntajeDeCriterio(codigo: 'volante', puntaje: 7.5),
        ],
        observaciones: '  Muy bien  ',
      );

      expect(pedido.toJson(), {
        'claseCodigo': 'c1',
        'tipo': 'MANEJO',
        'criterios': [
          {'codigo': 'espejos', 'puntaje': 8.0},
          {'codigo': 'volante', 'puntaje': 7.5},
        ],
        'observaciones': 'Muy bien',
      });
    });

    test('las observaciones nulas o en blanco no viajan', () {
      for (final texto in [null, '', '   ']) {
        final pedido = EvaluarClaseRequest(
          claseCodigo: 'c1',
          tipo: TipoPrueba.ciudad,
          criterios: const [PuntajeDeCriterio(codigo: 'giros', puntaje: 5)],
          observaciones: texto,
        );
        expect(pedido.toJson().containsKey('observaciones'), isFalse);
      }
    });
  });

  group('PruebaClase', () {
    test('fromJson lee el tipo, los criterios, el total y el resultado', () {
      final prueba = PruebaClase.fromJson(_pruebaJson(observaciones: 'Buena entrada'));

      expect(prueba.codigo, 'p1');
      expect(prueba.claseCodigo, 'c1');
      expect(prueba.tipo, TipoPrueba.estacionamiento);
      expect(prueba.criterios, hasLength(2));
      expect(prueba.criterios.first.puntaje, 7.5);
      expect(prueba.criterios.last.puntaje, 10);
      expect(prueba.puntajeTotal, 87.5);
      expect(prueba.resultado, ResultadoPrueba.aprobado);
      expect(prueba.aprobada, isTrue);
      expect(prueba.observaciones, 'Buena entrada');
      expect(prueba.fecha, DateTime(2026, 11, 2));
    });

    test('las observaciones pueden faltar y un resultado desconocido nunca se lee como aprobado', () {
      final prueba = PruebaClase.fromJson(_pruebaJson(resultado: 'RARO'));

      expect(prueba.observaciones, isNull);
      expect(prueba.resultado, ResultadoPrueba.reprobado);
      expect(prueba.aprobada, isFalse);
    });
  });

  group('ClaseAgenda con evaluable y conPrueba (CU18)', () {
    test('los lee cuando vienen', () {
      final clase = ClaseAgenda.fromJson(_agendaJson(extra: {'evaluable': true, 'conPrueba': true}));

      expect(clase.evaluable, isTrue);
      expect(clase.conPrueba, isTrue);
    });

    test('un backend que no los manda se lee como falso', () {
      final clase = ClaseAgenda.fromJson(_agendaJson());

      expect(clase.evaluable, isFalse);
      expect(clase.conPrueba, isFalse);
    });
  });
}
