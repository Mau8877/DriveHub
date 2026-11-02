import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/notificaciones/models/preferencias_notificacion.dart';
import 'package:mobile/features/notificaciones/validators/franja_silencio_validator.dart';

Map<String, dynamic> _json({
  String? desde = '22:00',
  String? hasta = '06:00',
}) => {
  'canales': {'email': false, 'push': true, 'inApp': true},
  'tipos': {
    'RESERVA': true,
    'PAGO': false,
    'EXAMEN': true,
    'RECORDATORIO': false,
  },
  'silencioDesde': desde,
  'silencioHasta': hasta,
  'email': 'ana@gmail.com',
  'actualizadoEn': '2026-11-02T10:00:00.000Z',
};

void main() {
  group('PreferenciasNotificacion.fromJson', () {
    test('lee los canales, los tipos y la franja', () {
      final p = PreferenciasNotificacion.fromJson(_json());

      expect(p.correo, isFalse);
      expect(p.push, isTrue);
      expect(p.enApp, isTrue);
      expect(p.tipos[TipoNotificacion.pago], isFalse);
      expect(p.tipos[TipoNotificacion.recordatorio], isFalse);
      expect(p.tipos[TipoNotificacion.reserva], isTrue);
      expect(p.silencioDesde, '22:00');
      expect(p.silencioHasta, '06:00');
      expect(p.tieneSilencio, isTrue);
    });

    test('una franja ausente queda en null y no hay silencio', () {
      final p = PreferenciasNotificacion.fromJson(
        _json(desde: null, hasta: null),
      );

      expect(p.silencioDesde, isNull);
      expect(p.silencioHasta, isNull);
      expect(p.tieneSilencio, isFalse);
    });

    test('un tipo que el servidor no manda se toma como activado', () {
      final json = _json();
      (json['tipos'] as Map<String, dynamic>).remove('EXAMEN');

      expect(
        PreferenciasNotificacion.fromJson(json).tipos[TipoNotificacion.examen],
        isTrue,
      );
    });
  });

  group('toJson', () {
    test(
      'manda la configuración completa con los nombres exactos de la API',
      () {
        final json = PreferenciasNotificacion.fromJson(
          _json(),
        ).toJson(email: 'ana@gmail.com');

        expect(json, {
          'canales': {'email': false, 'push': true, 'inApp': true},
          'tipos': {
            'RESERVA': true,
            'PAGO': false,
            'EXAMEN': true,
            'RECORDATORIO': false,
          },
          'silencioDesde': '22:00',
          'silencioHasta': '06:00',
          'email': 'ana@gmail.com',
        });
        expect(json.containsKey('actualizadoEn'), isFalse);
      },
    );

    test('sin franja manda null en las dos horas', () {
      final json = PreferenciasNotificacion.porDefecto().toJson(
        email: 'ana@gmail.com',
      );

      expect(json['silencioDesde'], isNull);
      expect(json['silencioHasta'], isNull);
    });
  });

  group('copias', () {
    test('por defecto: todo activado y sin silencio', () {
      final p = PreferenciasNotificacion.porDefecto();

      expect([p.correo, p.push, p.enApp], everyElement(isTrue));
      expect(p.tipos.values, everyElement(isTrue));
      expect(p.tipos.length, TipoNotificacion.values.length);
      expect(p.tieneSilencio, isFalse);
    });

    test(
      'conCanal, conTipo y conSilencio no tocan lo demás ni el original',
      () {
        final base = PreferenciasNotificacion.porDefecto();

        final cambiada = base
            .conCanal(push: false)
            .conTipo(TipoNotificacion.pago, false)
            .conSilencio(desde: '22:00', hasta: '06:00');

        expect(cambiada.push, isFalse);
        expect(cambiada.correo, isTrue);
        expect(cambiada.tipos[TipoNotificacion.pago], isFalse);
        expect(cambiada.tipos[TipoNotificacion.examen], isTrue);
        expect(cambiada.silencioDesde, '22:00');
        expect(base.push, isTrue);
        expect(base.tieneSilencio, isFalse);
      },
    );

    test('conSilencio sin horas quita el silencio', () {
      final conSilencio = PreferenciasNotificacion.porDefecto().conSilencio(
        desde: '22:00',
        hasta: '06:00',
      );

      expect(conSilencio.conSilencio().tieneSilencio, isFalse);
    });

    test(
      'dos preferencias iguales son iguales y cambiar algo las distingue',
      () {
        final a = PreferenciasNotificacion.porDefecto();
        final b = PreferenciasNotificacion.porDefecto();

        expect(a, b);
        expect(a.hashCode, b.hashCode);
        expect(a.conTipo(TipoNotificacion.pago, false), isNot(a));
        expect(a.conSilencio(desde: '22:00', hasta: '06:00'), isNot(a));
      },
    );
  });

  group('validarFranjaSilencio', () {
    test('sin horas es válida', () {
      expect(validarFranjaSilencio(null, null), isNull);
    });

    test(
      'dos horas distintas son válidas, también si cruzan la medianoche',
      () {
        expect(validarFranjaSilencio('13:00', '15:30'), isNull);
        expect(validarFranjaSilencio('22:00', '06:00'), isNull);
      },
    );

    test('una sola hora no es válida', () {
      expect(
        validarFranjaSilencio('22:00', null),
        'Elige las dos horas de silencio, o ninguna',
      );
      expect(
        validarFranjaSilencio(null, '06:00'),
        'Elige las dos horas de silencio, o ninguna',
      );
    });

    test('dos horas iguales no son válidas', () {
      expect(
        validarFranjaSilencio('10:00', '10:00'),
        'La hora de inicio y la de fin del silencio no pueden ser iguales',
      );
    });
  });
}
