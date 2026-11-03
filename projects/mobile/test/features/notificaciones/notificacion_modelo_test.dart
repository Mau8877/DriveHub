import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/notificaciones/models/notificacion.dart';
import 'package:mobile/features/notificaciones/models/preferencias_notificacion.dart';
import 'package:mobile/features/notificaciones/widgets/fecha_aviso.dart';

Map<String, dynamic> _json({String tipo = 'PAGO', bool leida = false}) => {
  'codigoNotificacion': 'n1',
  'tipo': tipo,
  'titulo': 'Pago recibido',
  'mensaje': 'Recibimos tu pago de 10 horas',
  'leida': leida,
  'creadaEn': '2026-11-02T14:30:00.000Z',
};

void main() {
  group('Notificacion', () {
    test('lee el JSON de la API', () {
      final aviso = Notificacion.fromJson(_json());

      expect(aviso.codigoNotificacion, 'n1');
      expect(aviso.tipo, TipoNotificacion.pago);
      expect(aviso.etiquetaTipo, 'Pagos');
      expect(aviso.titulo, 'Pago recibido');
      expect(aviso.leida, isFalse);
      expect(aviso.creadaEn.toUtc(), DateTime.utc(2026, 11, 2, 14, 30));
    });

    test('un tipo desconocido no rompe el parseo y se muestra como «Aviso»', () {
      final aviso = Notificacion.fromJson(_json(tipo: 'PROMOCION'));

      expect(aviso.tipo, isNull);
      expect(aviso.etiquetaTipo, 'Aviso');
    });

    test('copiar cambia solo lo pedido', () {
      final aviso = Notificacion.fromJson(_json());

      final leido = aviso.copiar(leida: true);

      expect(leido.leida, isTrue);
      expect(leido.codigoNotificacion, aviso.codigoNotificacion);
      expect(leido.mensaje, aviso.mensaje);
      expect(aviso.leida, isFalse);
      expect(aviso.copiar(), aviso);
    });
  });

  group('fechaDeAviso', () {
    final ahora = DateTime(2026, 11, 2, 12, 0);

    test('menos de un minuto: ahora', () {
      expect(fechaDeAviso(DateTime(2026, 11, 2, 11, 59, 30), ahora: ahora), 'ahora');
    });

    test('menos de una hora: hace N min', () {
      expect(fechaDeAviso(DateTime(2026, 11, 2, 11, 55), ahora: ahora), 'hace 5 min');
    });

    test('hoy: hace N h', () {
      expect(fechaDeAviso(DateTime(2026, 11, 2, 9, 0), ahora: ahora), 'hace 3 h');
    });

    test('ayer', () {
      expect(fechaDeAviso(DateTime(2026, 11, 1, 23, 0), ahora: ahora), 'ayer');
    });

    test('antes de ayer: día y mes, con el año si no es el actual', () {
      expect(fechaDeAviso(DateTime(2026, 5, 4, 10), ahora: ahora), '04 may');
      expect(fechaDeAviso(DateTime(2025, 12, 31, 10), ahora: ahora), '31 dic 2025');
    });

    test('pasada la medianoche es «ayer» aunque hayan pasado menos de 24 horas', () {
      final recien = DateTime(2026, 11, 2, 0, 30);
      expect(fechaDeAviso(DateTime(2026, 11, 1, 23, 30), ahora: recien), 'ayer');
      expect(fechaDeAviso(DateTime(2026, 11, 1, 20, 0), ahora: recien), 'ayer');
    });
  });

  test('fechaYHoraDeAviso muestra la fecha completa con la hora', () {
    expect(fechaYHoraDeAviso(DateTime(2026, 5, 4, 14, 5)), '04 may 2026, 14:05');
  });
}
