import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/models/hora_de_cancelacion.dart';
import 'package:mobile/features/clases/models/resultado_cancelacion.dart';
import 'package:mobile/features/clases/widgets/acciones_reserva.dart';
import 'package:mobile/features/clases/widgets/formato_clases.dart';
import 'package:mobile/features/clases/widgets/mensajes_de_cancelacion.dart';

import '../../helpers/clases_app.dart';
import '../../helpers/clases_falsos.dart';
import '../../helpers/pagos_falsos.dart';

void main() {
  late ClasesServiceFalso servicio;
  late PagosServiceFalso pagos;

  final hoy = DateTime.now();
  final enCinco = DateTime(hoy.year, hoy.month, hoy.day + 5);

  setUp(() {
    servicio = ClasesServiceFalso();
    pagos = PagosServiceFalso()..saldo = 5;
  });

  Future<MontajeDeClases> montarConClase(WidgetTester tester, {bool aTiempo = true}) async {
    servicio.proximas = [reservaDePrueba(codigo: 'a', fecha: enCinco, cancelacionATiempo: aTiempo)];
    return montarClases(tester, servicio: servicio, pagos: pagos);
  }

  Future<void> abrirElDialogo(WidgetTester tester) async {
    await tester.tap(find.byKey(const Key('cancelar-a')));
    await tester.pumpAndSettle();
  }

  group('acciones de «Mis clases»', () {
    testWidgets('cada clase ofrece Cancelar, y Reprogramar solo si está a tiempo', (tester) async {
      servicio.proximas = [
        reservaDePrueba(codigo: 'a', fecha: enCinco, cancelacionATiempo: true),
        reservaDePrueba(codigo: 'b', fecha: enCinco, horaInicio: '15:00', horaFin: '16:00'),
      ];
      await montarClases(tester, servicio: servicio, pagos: pagos);

      expect(find.byKey(const Key('cancelar-a')), findsOneWidget);
      expect(find.byKey(const Key('reprogramar-a')), findsOneWidget);
      expect(find.byKey(const Key('cancelar-b')), findsOneWidget);
      expect(find.byKey(const Key('reprogramar-b')), findsNothing);
    });

    testWidgets('el historial no ofrece acciones', (tester) async {
      servicio.historial = [reservaDePrueba(codigo: 'h', estado: EstadoReserva.cancelada, fecha: enCinco)];
      servicio.proximas = [];
      await montarClases(tester, servicio: servicio, pagos: pagos);
      await tester.tap(find.text('Historial'));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('reserva-h')), findsOneWidget);
      expect(find.byKey(const Key('cancelar-h')), findsNothing);
    });

    testWidgets('una clase que ya empezó no ofrece ninguna acción', (tester) async {
      final reserva = reservaDePrueba(
        codigo: 'x',
        fecha: DateTime(2020, 1, 1),
        horaInicio: '08:00',
        cancelacionATiempo: true,
      );
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: AccionesReserva(reserva: reserva, alCancelar: () {}, alReprogramar: () {}),
          ),
        ),
      );

      expect(find.byKey(const Key('cancelar-x')), findsNothing);
      expect(find.byKey(const Key('reprogramar-x')), findsNothing);
    });

    testWidgets('sin qué hacer al reprogramar, el botón no se dibuja aunque esté a tiempo', (tester) async {
      final reserva = reservaDePrueba(codigo: 'x', fecha: enCinco, cancelacionATiempo: true);
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: AccionesReserva(reserva: reserva, alCancelar: () {}),
          ),
        ),
      );

      expect(find.byKey(const Key('cancelar-x')), findsOneWidget);
      expect(find.byKey(const Key('reprogramar-x')), findsNothing);
    });
  });

  group('cancelar a tiempo', () {
    testWidgets('el diálogo muestra la clase y dice que se devolverá la hora, sin cancelar nada todavía', (tester) async {
      await montarConClase(tester);

      await abrirElDialogo(tester);

      expect(find.text('¿Cancelar esta clase?'), findsOneWidget);
      expect(find.text('Se te devolverá 1 hora.'), findsOneWidget);
      expect(find.text('Ivan Alfa'), findsWidgets);
      expect(servicio.cancelacionesPedidas, isEmpty);
    });

    testWidgets('confirmar cancela, cierra el diálogo, avisa que se devolvió la hora y la clase sale de la lista', (
      tester,
    ) async {
      await montarConClase(tester);
      await abrirElDialogo(tester);
      servicio.proximas = [];

      await tester.tap(find.byKey(const Key('confirmarCancelacion')));
      await tester.pumpAndSettle();

      expect(servicio.cancelacionesPedidas, ['a']);
      expect(find.text('¿Cancelar esta clase?'), findsNothing);
      expect(find.text('Clase cancelada. Se te devolvió 1 hora.'), findsOneWidget);
      expect(find.byKey(const Key('reserva-a')), findsNothing);
    });

    testWidgets('«Volver» cierra el diálogo sin cancelar', (tester) async {
      await montarConClase(tester);
      await abrirElDialogo(tester);

      await tester.tap(find.byKey(const Key('volverDeCancelar')));
      await tester.pumpAndSettle();

      expect(find.text('¿Cancelar esta clase?'), findsNothing);
      expect(servicio.cancelacionesPedidas, isEmpty);
      expect(find.byKey(const Key('reserva-a')), findsOneWidget);
    });
  });

  group('cancelar tarde o con Pagos caído', () {
    testWidgets('sin tiempo, el diálogo advierte que se pierde la hora y el aviso final lo confirma', (tester) async {
      await montarConClase(tester, aTiempo: false);
      servicio.alCancelar = (codigo) => ResultadoCancelacion(
        reserva: reservaDePrueba(codigo: codigo, estado: EstadoReserva.cancelada),
        horaDevuelta: HoraDeCancelacion.perdida,
        horasDisponibles: 3,
      );

      await abrirElDialogo(tester);
      expect(find.text('Cancelas con menos de 12 horas: perderás la hora.'), findsOneWidget);
      await tester.tap(find.byKey(const Key('confirmarCancelacion')));
      await tester.pumpAndSettle();

      expect(find.text('Clase cancelada. No se devolvió la hora.'), findsOneWidget);
    });

    testWidgets('con la hora en proceso avisa que se devolverá en unos minutos', (tester) async {
      await montarConClase(tester);
      servicio.alCancelar = (codigo) => ResultadoCancelacion(
        reserva: reservaDePrueba(codigo: codigo, estado: EstadoReserva.cancelada),
        horaDevuelta: HoraDeCancelacion.enProceso,
        horasDisponibles: null,
      );

      await abrirElDialogo(tester);
      await tester.tap(find.byKey(const Key('confirmarCancelacion')));
      await tester.pumpAndSettle();

      expect(find.text('Clase cancelada. Tu hora se devolverá en unos minutos.'), findsOneWidget);
    });
  });

  group('errores y doble toque', () {
    testWidgets('un doble toque en confirmar manda una sola petición y deshabilita los botones', (tester) async {
      final espera = Completer<void>();
      servicio.antesDeCancelar = () => espera.future;
      await montarConClase(tester);
      await abrirElDialogo(tester);

      await tester.tap(find.byKey(const Key('confirmarCancelacion')));
      await tester.pump();
      await tester.tap(find.byKey(const Key('confirmarCancelacion')), warnIfMissed: false);
      await tester.pump();

      expect(servicio.cancelacionesPedidas, hasLength(1));
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
      expect(tester.widget<TextButton>(find.byKey(const Key('volverDeCancelar'))).onPressed, isNull);

      espera.complete();
      await tester.pumpAndSettle();
      expect(servicio.cancelacionesPedidas, hasLength(1));
    });

    testWidgets('sin conexión el diálogo se queda abierto con el aviso, y «Reintentar» vuelve a probar', (tester) async {
      servicio.errorAlCancelar = DioException(requestOptions: RequestOptions(path: '/reservas/a/cancelar'));
      await montarConClase(tester);
      await abrirElDialogo(tester);

      await tester.tap(find.byKey(const Key('confirmarCancelacion')));
      await tester.pumpAndSettle();

      expect(find.text('¿Cancelar esta clase?'), findsOneWidget);
      expect(find.byKey(const Key('errorCancelar')), findsOneWidget);
      expect(find.text('Reintentar'), findsOneWidget);

      servicio.errorAlCancelar = null;
      await tester.tap(find.byKey(const Key('confirmarCancelacion')));
      await tester.pumpAndSettle();

      expect(servicio.cancelacionesPedidas, ['a', 'a']);
      expect(find.text('¿Cancelar esta clase?'), findsNothing);
      expect(find.text('Clase cancelada. Se te devolvió 1 hora.'), findsOneWidget);
    });

    testWidgets('si el servidor dice que la clase ya no se puede cancelar, cierra con su mensaje', (tester) async {
      servicio.errorAlCancelar = ApiException(code: 'ERR_ACA_33', message: 'Esa clase ya no se puede cancelar');
      await montarConClase(tester);
      await abrirElDialogo(tester);

      await tester.tap(find.byKey(const Key('confirmarCancelacion')));
      await tester.pumpAndSettle();

      expect(find.text('¿Cancelar esta clase?'), findsNothing);
      expect(find.text('Esa clase ya no se puede cancelar'), findsOneWidget);
    });

    testWidgets('otro error de negocio se muestra en el diálogo, que sigue abierto', (tester) async {
      servicio.errorAlCancelar = ApiException(code: 'ERR_ACA_17', message: 'Un servicio no responde');
      await montarConClase(tester);
      await abrirElDialogo(tester);

      await tester.tap(find.byKey(const Key('confirmarCancelacion')));
      await tester.pumpAndSettle();

      expect(find.text('¿Cancelar esta clase?'), findsOneWidget);
      expect(find.text('Un servicio no responde'), findsOneWidget);
    });
  });

  group('textos y fechas', () {
    test('el aviso previo depende de si está a tiempo', () {
      expect(avisoPrevioDeCancelacion(aTiempo: true), 'Se te devolverá 1 hora.');
      expect(avisoPrevioDeCancelacion(aTiempo: false), 'Cancelas con menos de 12 horas: perderás la hora.');
    });

    test('el aviso final dice qué pasó con la hora', () {
      expect(avisoDeResultadoDeCancelacion(HoraDeCancelacion.devuelta), contains('devolvió 1 hora'));
      expect(avisoDeResultadoDeCancelacion(HoraDeCancelacion.perdida), contains('No se devolvió'));
      expect(avisoDeResultadoDeCancelacion(HoraDeCancelacion.enProceso), contains('unos minutos'));
    });

    test('laClaseYaEmpezo compara la fecha y la hora de inicio con la hora del teléfono', () {
      final reserva = reservaDePrueba(fecha: DateTime(2026, 11, 2), horaInicio: '10:00');

      expect(laClaseYaEmpezo(reserva, DateTime(2026, 11, 2, 9, 59)), isFalse);
      expect(laClaseYaEmpezo(reserva, DateTime(2026, 11, 2, 10, 0)), isTrue);
      expect(laClaseYaEmpezo(reserva, DateTime(2026, 11, 3, 8, 0)), isTrue);
      expect(laClaseYaEmpezo(reserva, DateTime(2026, 11, 1, 23, 0)), isFalse);
    });
  });
}
