import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/models/hora_de_cancelacion.dart';
import 'package:mobile/features/clases/models/resultado_cancelacion_instructor.dart';
import 'package:mobile/features/clases/screens/agenda_instructor_screen.dart';
import 'package:mobile/features/clases/widgets/mensajes_de_cancelacion_instructor.dart';

import '../../helpers/clases_falsos.dart';

void main() {
  late ClasesServiceFalso servicio;

  setUp(() {
    servicio = ClasesServiceFalso()
      ..clasesDeAgenda = [
        claseDeAgendaDePrueba(codigo: 'a', cancelable: true, estudianteNombre: 'Ana Alfa'),
      ];
  });

  Future<void> montar(WidgetTester tester) async {
    final container = ProviderContainer(overrides: [clasesServiceFalso(servicio)]);
    addTearDown(container.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(theme: AppTheme.light, home: const Scaffold(body: AgendaInstructorScreen())),
      ),
    );
    await tester.pumpAndSettle();
  }

  Future<void> abrirElDialogo(WidgetTester tester) async {
    await tester.tap(find.byKey(const Key('cancelar-a')));
    await tester.pumpAndSettle();
  }

  Future<void> confirmar(WidgetTester tester) async {
    await tester.tap(find.byKey(const Key('confirmarCancelacion')));
    await tester.pumpAndSettle();
  }

  group('quién ofrece «Cancelar»', () {
    testWidgets('solo las clases cancelables', (tester) async {
      servicio.clasesDeAgenda = [
        claseDeAgendaDePrueba(codigo: 'a', cancelable: true),
        claseDeAgendaDePrueba(codigo: 'b', terminada: true),
        claseDeAgendaDePrueba(codigo: 'c', estado: EstadoReserva.cancelada),
        claseDeAgendaDePrueba(codigo: 'd', estado: EstadoReserva.completada),
        claseDeAgendaDePrueba(codigo: 'e', estado: EstadoReserva.noAsistio),
      ];
      await montar(tester);

      expect(find.byKey(const Key('cancelar-a')), findsOneWidget);
      for (final codigo in ['b', 'c', 'd', 'e']) {
        expect(find.byKey(Key('cancelar-$codigo')), findsNothing);
      }
    });
  });

  group('la confirmación', () {
    testWidgets('muestra la clase y avisa que se devuelve la hora y que la franja se bloquea, sin cancelar nada', (
      tester,
    ) async {
      await montar(tester);

      await abrirElDialogo(tester);

      expect(find.text('¿Cancelar esta clase?'), findsOneWidget);
      expect(find.byKey(const Key('claseACancelar')), findsOneWidget);
      expect(find.text('Ana Alfa'), findsWidgets);
      expect(find.textContaining('Se le devolverá 1 hora al estudiante.'), findsOneWidget);
      expect(find.textContaining('La franja quedará bloqueada.'), findsOneWidget);
      expect(servicio.cancelacionesDeInstructorPedidas, isEmpty);
    });

    testWidgets('«Volver» cierra el diálogo sin cancelar', (tester) async {
      await montar(tester);
      await abrirElDialogo(tester);

      await tester.tap(find.byKey(const Key('volverDeCancelar')));
      await tester.pumpAndSettle();

      expect(find.text('¿Cancelar esta clase?'), findsNothing);
      expect(servicio.cancelacionesDeInstructorPedidas, isEmpty);
    });
  });

  group('cancelar', () {
    testWidgets('con la hora devuelta cierra, avisa y la clase queda «Cancelada» en su día', (tester) async {
      await montar(tester);
      await abrirElDialogo(tester);
      servicio.clasesDeAgenda = [claseDeAgendaDePrueba(codigo: 'a', estado: EstadoReserva.cancelada)];

      await confirmar(tester);

      expect(servicio.cancelacionesDeInstructorPedidas, ['a']);
      expect(find.text('¿Cancelar esta clase?'), findsNothing);
      expect(find.text('Clase cancelada. Se devolvió la hora al estudiante.'), findsOneWidget);
      expect(find.text('Cancelada'), findsOneWidget);
      expect(find.byKey(const Key('cancelar-a')), findsNothing);
    });

    testWidgets('con Pagos caído avisa que la hora se devolverá en unos minutos', (tester) async {
      servicio.alCancelarComoInstructor = (codigo) => ResultadoCancelacionInstructor(
        clase: claseDeAgendaDePrueba(codigo: codigo, estado: EstadoReserva.cancelada),
        horaDevuelta: HoraDeCancelacion.enProceso,
      );
      await montar(tester);
      await abrirElDialogo(tester);

      await confirmar(tester);

      expect(find.text('Clase cancelada. La hora del estudiante se devolverá en unos minutos.'), findsOneWidget);
    });

    testWidgets('un doble toque en confirmar manda una sola petición y deshabilita los botones', (tester) async {
      final espera = Completer<void>();
      servicio.antesDeCancelarComoInstructor = () => espera.future;
      await montar(tester);
      await abrirElDialogo(tester);

      await tester.tap(find.byKey(const Key('confirmarCancelacion')));
      await tester.pump();
      await tester.tap(find.byKey(const Key('confirmarCancelacion')), warnIfMissed: false);
      await tester.pump();

      expect(servicio.cancelacionesDeInstructorPedidas, hasLength(1));
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
      expect(tester.widget<TextButton>(find.byKey(const Key('volverDeCancelar'))).onPressed, isNull);

      espera.complete();
      await tester.pumpAndSettle();
      expect(servicio.cancelacionesDeInstructorPedidas, hasLength(1));
    });
  });

  group('errores', () {
    testWidgets('sin conexión el diálogo se queda abierto con el aviso, y «Reintentar» vuelve a probar', (tester) async {
      servicio.errorAlCancelarComoInstructor = DioException(
        requestOptions: RequestOptions(path: '/reservas/a/cancelar-por-instructor'),
      );
      await montar(tester);
      await abrirElDialogo(tester);

      await confirmar(tester);

      expect(find.text('¿Cancelar esta clase?'), findsOneWidget);
      expect(find.byKey(const Key('errorCancelar')), findsOneWidget);
      expect(find.text('Reintentar'), findsOneWidget);

      servicio.errorAlCancelarComoInstructor = null;
      await confirmar(tester);

      expect(servicio.cancelacionesDeInstructorPedidas, ['a', 'a']);
      expect(find.text('¿Cancelar esta clase?'), findsNothing);
      expect(find.text('Clase cancelada. Se devolvió la hora al estudiante.'), findsOneWidget);
    });

    for (final codigo in ['ERR_ACA_36', 'ERR_ACA_35']) {
      testWidgets('$codigo cierra el diálogo con el mensaje del servidor y refresca la agenda', (tester) async {
        servicio.errorAlCancelarComoInstructor = ApiException(code: codigo, message: 'Esa clase ya no se puede cancelar');
        await montar(tester);
        await abrirElDialogo(tester);
        servicio.pedidosDeAgenda.clear();

        await confirmar(tester);

        expect(find.text('¿Cancelar esta clase?'), findsNothing);
        expect(find.text('Esa clase ya no se puede cancelar'), findsOneWidget);
        expect(servicio.pedidosDeAgenda, hasLength(1));
      });
    }

    testWidgets('otro error de negocio se muestra en el diálogo, que sigue abierto con «Cancelar clase»', (tester) async {
      servicio.errorAlCancelarComoInstructor = ApiException(code: 'ERR_ACA_17', message: 'Un servicio no responde');
      await montar(tester);
      await abrirElDialogo(tester);

      await confirmar(tester);

      expect(find.text('¿Cancelar esta clase?'), findsOneWidget);
      expect(find.text('Un servicio no responde'), findsOneWidget);
      expect(find.text('Cancelar clase'), findsOneWidget);
      expect(find.text('Reintentar'), findsNothing);
    });
  });

  group('textos', () {
    test('el aviso previo es fijo: devuelve la hora y bloquea la franja', () {
      expect(avisoPrevioDeCancelacionInstructor, contains('Se le devolverá 1 hora al estudiante.'));
      expect(avisoPrevioDeCancelacionInstructor, contains('La franja quedará bloqueada.'));
    });

    test('el aviso final dice qué pasó con la hora del estudiante', () {
      expect(
        avisoDeResultadoDeCancelacionInstructor(HoraDeCancelacion.devuelta),
        'Clase cancelada. Se devolvió la hora al estudiante.',
      );
      expect(
        avisoDeResultadoDeCancelacionInstructor(HoraDeCancelacion.enProceso),
        'Clase cancelada. La hora del estudiante se devolverá en unos minutos.',
      );
      expect(avisoDeResultadoDeCancelacionInstructor(HoraDeCancelacion.perdida), 'Clase cancelada.');
    });

    test('solo ERR_ACA_35 y ERR_ACA_36 cierran el diálogo', () {
      expect(codigosQueCierranLaCancelacionDelInstructor, {'ERR_ACA_35', 'ERR_ACA_36'});
    });
  });
}
