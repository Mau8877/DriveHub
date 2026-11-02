import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/clases/models/asistencia_clase.dart';
import 'package:mobile/features/clases/widgets/mensajes_de_cierre.dart';
import 'package:mobile/features/clases/widgets/validadores_clase.dart';

void main() {
  group('validarTema', () {
    test('admite hasta 120 caracteres, y vacío o nulo', () {
      expect(validarTema(null), isNull);
      expect(validarTema(''), isNull);
      expect(validarTema('a' * 120), isNull);
    });

    test('con 121 caracteres avisa', () {
      expect(validarTema('a' * 121), 'El tema admite hasta 120 caracteres');
    });

    test('mide el texto sin recortar, como el backend', () {
      expect(validarTema('${'a' * 120} '), isNotNull);
    });
  });

  group('validarObservaciones', () {
    test('admite hasta 500 caracteres', () {
      expect(validarObservaciones(null), isNull);
      expect(validarObservaciones('b' * 500), isNull);
    });

    test('con 501 caracteres avisa', () {
      expect(validarObservaciones('b' * 501), 'Las observaciones admiten hasta 500 caracteres');
    });
  });

  group('validarKilometraje', () {
    test('vacío, en blanco o nulo valen: es opcional', () {
      expect(validarKilometraje(null), isNull);
      expect(validarKilometraje(''), isNull);
      expect(validarKilometraje('   '), isNull);
    });

    test('acepta de 0 a 9 999 999', () {
      expect(validarKilometraje('0'), isNull);
      expect(validarKilometraje('1250'), isNull);
      expect(validarKilometraje('9999999'), isNull);
    });

    test('rechaza lo que no son solo dígitos, y lo que pasa del máximo', () {
      expect(validarKilometraje('-1'), 'Escribe solo números');
      expect(validarKilometraje('12,5'), 'Escribe solo números');
      expect(validarKilometraje('abc'), 'Escribe solo números');
      expect(validarKilometraje('10000000'), 'El kilometraje es demasiado grande');
    });
  });

  group('validarKmFinal', () {
    test('el final no puede ser menor que el inicial', () {
      expect(validarKmFinal('200', '199'), 'El kilometraje final no puede ser menor que el inicial');
    });

    test('iguales, mayores, o uno solo, valen', () {
      expect(validarKmFinal('200', '200'), isNull);
      expect(validarKmFinal('200', '350'), isNull);
      expect(validarKmFinal('', '50'), isNull);
      expect(validarKmFinal('200', ''), isNull);
      expect(validarKmFinal(null, null), isNull);
    });

    test('también cumple las reglas de un kilometraje', () {
      expect(validarKmFinal('', '10000000'), 'El kilometraje es demasiado grande');
      expect(validarKmFinal('', 'x'), 'Escribe solo números');
    });
  });

  test('kilometrajeDe lee el número o devuelve nulo', () {
    expect(kilometrajeDe('142'), 142);
    expect(kilometrajeDe(' 7 '), 7);
    expect(kilometrajeDe(''), isNull);
    expect(kilometrajeDe(null), isNull);
    expect(kilometrajeDe('1,5'), isNull);
  });

  group('mensajes de cierre', () {
    test('el aviso de la inasistencia dice que se pierde la hora', () {
      expect(avisoDeInasistencia, 'El estudiante pierde la hora de esta clase.');
    });

    test('el resultado de cerrar depende de la asistencia', () {
      expect(avisoDeResultadoDeCierre(AsistenciaClase.presente), 'Clase cerrada. Quedó completada.');
      expect(avisoDeResultadoDeCierre(AsistenciaClase.tarde), 'Clase cerrada. Quedó completada.');
      expect(
        avisoDeResultadoDeCierre(AsistenciaClase.ausente),
        'Clase cerrada. Quedó registrada la inasistencia.',
      );
    });

    test('los demás textos y los códigos que cierran la hoja', () {
      expect(avisoDeCorreccionGuardada, 'Cambios guardados.');
      expect(avisoKilometrajeNoSeBorra, contains('No se puede borrar el kilometraje'));
      expect(codigosQueCierranElCierre, {'ERR_ACA_35', 'ERR_ACA_37'});
    });
  });
}
