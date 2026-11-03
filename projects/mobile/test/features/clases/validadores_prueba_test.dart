import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/clases/models/catalogo_pruebas.dart';
import 'package:mobile/features/clases/models/prueba_clase.dart';
import 'package:mobile/features/clases/widgets/validadores_prueba.dart';

CriterioDelCatalogo _criterio(String codigo) =>
    CriterioDelCatalogo(codigo: codigo, descripcion: 'Criterio $codigo', maximo: 10);

void main() {
  final cinco = [
    for (final c in ['a', 'b', 'c', 'd', 'e']) _criterio(c),
  ];

  group('totalDePrueba', () {
    test('todo en el máximo es 100, todo en cero es 0 y todo en siete es 70', () {
      expect(totalDePrueba({for (final c in cinco) c.codigo: 10.0}, cinco), 100);
      expect(totalDePrueba({for (final c in cinco) c.codigo: 0.0}, cinco), 0);
      expect(totalDePrueba({for (final c in cinco) c.codigo: 7.0}, cinco), 70);
    });

    test('es nulo mientras falte puntuar un criterio', () {
      final puntajes = <String, double?>{for (final c in cinco) c.codigo: 5.0}..['c'] = null;

      expect(totalDePrueba(puntajes, cinco), isNull);
      expect(totalDePrueba({}, cinco), isNull);
      expect(totalDePrueba({}, const []), isNull);
    });

    test('se redondea a dos decimales', () {
      final tres = [_criterio('a'), _criterio('b'), _criterio('c')];

      // * 21,5 de 30 = 71,666... -> 71,67
      expect(totalDePrueba({'a': 7.0, 'b': 7.0, 'c': 7.5}, tres), 71.67);
    });
  });

  group('resultadoDePrueba', () {
    test('aprueba desde 70 y reprueba por debajo', () {
      expect(resultadoDePrueba(70), ResultadoPrueba.aprobado);
      expect(resultadoDePrueba(100), ResultadoPrueba.aprobado);
      expect(resultadoDePrueba(69.99), ResultadoPrueba.reprobado);
      expect(resultadoDePrueba(0), ResultadoPrueba.reprobado);
    });
  });

  group('criterios pendientes', () {
    test('nombra el primero sin puntuar', () {
      final puntajes = <String, double?>{'a': 5.0, 'b': null, 'c': null};

      expect(primerCriterioSinPuntuar(puntajes, cinco), 'Criterio b');
      expect(validarCriteriosDePrueba(puntajes, cinco), 'Falta puntuar: Criterio b');
    });

    test('sin pendientes no hay mensaje', () {
      final puntajes = <String, double?>{for (final c in cinco) c.codigo: 5.0};

      expect(primerCriterioSinPuntuar(puntajes, cinco), isNull);
      expect(validarCriteriosDePrueba(puntajes, cinco), isNull);
    });
  });

  group('validarObservacionesDePrueba', () {
    test('admite hasta 500 caracteres, sin recortar, y vacío vale', () {
      expect(validarObservacionesDePrueba(null), isNull);
      expect(validarObservacionesDePrueba(''), isNull);
      expect(validarObservacionesDePrueba('a' * 500), isNull);
      expect(validarObservacionesDePrueba('a' * 501), 'Las observaciones admiten hasta 500 caracteres');
      expect(validarObservacionesDePrueba('${' ' * 400}${'a' * 101}'), isNotNull);
    });
  });

  group('formatoPuntaje', () {
    test('sin ceros sobrantes y con coma decimal', () {
      expect(formatoPuntaje(8), '8');
      expect(formatoPuntaje(100), '100');
      expect(formatoPuntaje(7.5), '7,5');
      expect(formatoPuntaje(72.5), '72,5');
      expect(formatoPuntaje(71.67), '71,67');
      expect(formatoPuntaje(0), '0');
    });
  });
}
