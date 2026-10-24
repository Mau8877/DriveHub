import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/horarios/validators/horarios_validators.dart';

void main() {
  final hoy = DateTime(2026, 10, 26);

  group('validarDuracion', () {
    test('acepta los límites 15 y 240 y un valor intermedio', () {
      expect(validarDuracion('15'), isNull);
      expect(validarDuracion('60'), isNull);
      expect(validarDuracion('240'), isNull);
      expect(validarDuracion(' 90 '), isNull);
    });

    test('rechaza 14 y 241', () {
      expect(validarDuracion('14'), 'La duración debe estar entre 15 y 240 minutos');
      expect(validarDuracion('241'), 'La duración debe estar entre 15 y 240 minutos');
    });

    test('rechaza vacío, nulo y lo que no es un entero', () {
      expect(validarDuracion(''), 'La duración de la clase es obligatoria');
      expect(validarDuracion(null), 'La duración de la clase es obligatoria');
      expect(validarDuracion('abc'), 'La duración debe ser un número entero de minutos');
      expect(validarDuracion('60.5'), 'La duración debe ser un número entero de minutos');
    });
  });

  group('validarHoraInicio', () {
    test('acepta HH:mm y rechaza vacío o con otro formato', () {
      expect(validarHoraInicio('08:00'), isNull);
      expect(validarHoraInicio('23:59'), isNull);
      expect(validarHoraInicio(''), 'La hora de inicio es obligatoria');
      expect(validarHoraInicio(null), 'La hora de inicio es obligatoria');
      expect(validarHoraInicio('8:00'), 'La hora de inicio debe tener el formato HH:mm');
      expect(validarHoraInicio('24:00'), 'La hora de inicio debe tener el formato HH:mm');
    });
  });

  group('validarHoraFin', () {
    test('acepta una hora de fin posterior', () {
      expect(validarHoraFin('08:00', '12:00'), isNull);
      expect(validarHoraFin('08:00', '08:01'), isNull);
    });

    test('rechaza una hora de fin igual o anterior a la de inicio', () {
      expect(validarHoraFin('12:00', '12:00'), 'La hora de fin debe ser posterior a la de inicio');
      expect(validarHoraFin('12:00', '08:00'), 'La hora de fin debe ser posterior a la de inicio');
    });

    test('rechaza vacío o con otro formato', () {
      expect(validarHoraFin('08:00', ''), 'La hora de fin es obligatoria');
      expect(validarHoraFin('08:00', null), 'La hora de fin es obligatoria');
      expect(validarHoraFin('08:00', '9:00'), 'La hora de fin debe tener el formato HH:mm');
    });

    test('sin hora de inicio válida no compara (el otro campo ya muestra su error)', () {
      expect(validarHoraFin('', '12:00'), isNull);
      expect(validarHoraFin(null, '12:00'), isNull);
    });
  });

  group('validarVentana', () {
    test('un tramo de 4 horas con clases de 60 minutos es válido', () {
      expect(validarVentana('08:00', '12:00', '60'), isNull);
    });

    test('un tramo de 4 horas con clases de 90 minutos no es múltiplo', () {
      expect(
        validarVentana('08:00', '12:00', '90'),
        'El tramo entre inicio y fin debe ser un múltiplo de la duración de la clase',
      );
    });

    test('un tramo igual a una sola clase es válido', () {
      expect(validarVentana('08:00', '08:15', '15'), isNull);
    });

    test('no se evalúa si falta o es inválida alguna de las tres cosas (para no repetir mensajes)', () {
      expect(validarVentana('', '12:00', '90'), isNull);
      expect(validarVentana('08:00', '', '90'), isNull);
      expect(validarVentana('08:00', '12:00', ''), isNull);
      expect(validarVentana('08:00', '12:00', 'abc'), isNull);
      expect(validarVentana('12:00', '08:00', '90'), isNull);
      expect(validarVentana('08:00', '12:00', '10'), isNull);
    });
  });

  group('validarVigenteDesde', () {
    test('hoy y el futuro son válidos; nulo también', () {
      expect(validarVigenteDesde(hoy, hoy: hoy), isNull);
      expect(validarVigenteDesde(DateTime(2026, 11, 2), hoy: hoy), isNull);
      expect(validarVigenteDesde(null, hoy: hoy), isNull);
    });

    test('anterior a hoy es inválido, sin importar la hora del día', () {
      expect(
        validarVigenteDesde(DateTime(2026, 10, 25, 23, 59), hoy: hoy),
        'La fecha de inicio no puede ser anterior a hoy',
      );
    });

    test('hoy a cualquier hora es válido', () {
      expect(validarVigenteDesde(DateTime(2026, 10, 26, 0, 0), hoy: DateTime(2026, 10, 26, 18, 0)), isNull);
    });
  });

  group('validarVigenteHasta', () {
    test('nulo es válido (sin fin)', () {
      expect(validarVigenteHasta(null, DateTime(2026, 11, 2), hoy: hoy), isNull);
    });

    test('igual a desde es válido; posterior también', () {
      expect(validarVigenteHasta(DateTime(2026, 11, 2), DateTime(2026, 11, 2), hoy: hoy), isNull);
      expect(validarVigenteHasta(DateTime(2026, 12, 1), DateTime(2026, 11, 2), hoy: hoy), isNull);
    });

    test('anterior a desde es inválido', () {
      expect(
        validarVigenteHasta(DateTime(2026, 11, 1), DateTime(2026, 11, 2), hoy: hoy),
        'La fecha en que termina el horario no puede ser anterior a la de inicio',
      );
    });

    test('sin desde, no puede ser anterior a hoy', () {
      expect(validarVigenteHasta(DateTime(2026, 10, 25), null, hoy: hoy), isNotNull);
      expect(validarVigenteHasta(DateTime(2026, 10, 26), null, hoy: hoy), isNull);
    });
  });

  group('validarNombre', () {
    test('acepta un nombre normal y uno de exactamente 60 caracteres', () {
      expect(validarNombre('Mañanas entre semana'), isNull);
      expect(validarNombre('a' * 60), isNull);
    });

    test('rechaza vacío, solo espacios y nulo', () {
      expect(validarNombre(''), 'El nombre es obligatorio');
      expect(validarNombre('   '), 'El nombre es obligatorio');
      expect(validarNombre(null), 'El nombre es obligatorio');
    });

    test('rechaza más de 60 caracteres', () {
      expect(validarNombre('a' * 61), 'El nombre no puede pasar de 60 caracteres');
    });
  });

  group('validarDias', () {
    test('exige al menos un día', () {
      expect(validarDias(<int>{}), 'Elige al menos un día de la semana');
      expect(validarDias(<int>{3}), isNull);
      expect(validarDias(<int>{1, 2, 3, 4, 5}), isNull);
    });
  });

  group('vistaPreviaFranjas', () {
    test('con datos válidos devuelve las franjas por día y por semana', () {
      final vista = vistaPreviaFranjas(horaInicio: '08:00', horaFin: '12:00', duracionMin: 60, cantidadDias: 5);

      expect(vista, (porDia: 4, porSemana: 20));
    });

    test('con otra duración recalcula', () {
      final vista = vistaPreviaFranjas(horaInicio: '08:00', horaFin: '11:00', duracionMin: 90, cantidadDias: 2);

      expect(vista, (porDia: 2, porSemana: 4));
    });

    test('devuelve null si no hay días', () {
      expect(vistaPreviaFranjas(horaInicio: '08:00', horaFin: '12:00', duracionMin: 60, cantidadDias: 0), isNull);
    });

    test('devuelve null si falta una hora o tiene un formato inválido', () {
      expect(vistaPreviaFranjas(horaInicio: '', horaFin: '12:00', duracionMin: 60, cantidadDias: 1), isNull);
      expect(vistaPreviaFranjas(horaInicio: '08:00', horaFin: null, duracionMin: 60, cantidadDias: 1), isNull);
      expect(vistaPreviaFranjas(horaInicio: '8:00', horaFin: '12:00', duracionMin: 60, cantidadDias: 1), isNull);
    });

    test('devuelve null si la hora de fin no es posterior a la de inicio', () {
      expect(vistaPreviaFranjas(horaInicio: '12:00', horaFin: '12:00', duracionMin: 60, cantidadDias: 1), isNull);
      expect(vistaPreviaFranjas(horaInicio: '12:00', horaFin: '08:00', duracionMin: 60, cantidadDias: 1), isNull);
    });

    test('devuelve null si la duración no divide la ventana o está fuera de rango', () {
      expect(vistaPreviaFranjas(horaInicio: '08:00', horaFin: '09:00', duracionMin: 45, cantidadDias: 1), isNull);
      expect(vistaPreviaFranjas(horaInicio: '08:00', horaFin: '12:00', duracionMin: 10, cantidadDias: 1), isNull);
      expect(vistaPreviaFranjas(horaInicio: '08:00', horaFin: '12:00', duracionMin: null, cantidadDias: 1), isNull);
    });
  });
}
