import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/horarios/models/estado_plantilla.dart';
import 'package:mobile/features/horarios/screens/horarios_screen.dart';

import '../../helpers/horarios_falsos.dart';

void main() {
  late HorariosServiceFalso servicio;

  setUp(() => servicio = HorariosServiceFalso());

  Future<void> montar(WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [horariosServiceFalso(servicio)],
        child: const MaterialApp(home: Scaffold(body: HorariosScreen())),
      ),
    );
    await tester.pumpAndSettle();
  }

  Future<void> abrirMenu(WidgetTester tester) async {
    await tester.tap(find.byTooltip('Opciones del horario'));
    await tester.pumpAndSettle();
  }

  group('pestañas', () {
    testWidgets('abre en Plantillas, con el botón de nuevo horario y sin la vista de franjas', (tester) async {
      await montar(tester);

      expect(find.byKey(const Key('pestanasHorarios')), findsOneWidget);
      expect(find.byKey(const Key('botonNuevoHorario')), findsOneWidget);
      expect(find.byTooltip('Semana anterior'), findsNothing);
    });

    testWidgets('al elegir Franjas muestra la semana y oculta el botón de nuevo horario', (tester) async {
      await montar(tester);

      await tester.tap(find.text('Franjas'));
      await tester.pumpAndSettle();

      expect(find.byTooltip('Semana anterior'), findsOneWidget);
      expect(find.byKey(const Key('botonNuevoHorario')), findsNothing);
      expect(servicio.llamadas, contains('listarFranjas'));
    });

    testWidgets('al volver a Plantillas reaparece el botón', (tester) async {
      await montar(tester);
      await tester.tap(find.text('Franjas'));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Plantillas'));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('botonNuevoHorario')), findsOneWidget);
    });
  });

  group('lista de plantillas', () {
    testWidgets('sin horarios muestra el mensaje para crear el primero', (tester) async {
      await montar(tester);

      expect(find.text('Todavía no tienes horarios. Crea el primero.'), findsOneWidget);
      expect(find.byKey(const Key('botonNuevoHorario')), findsOneWidget);
    });

    testWidgets('muestra el nombre, el estado, los días, la ventana, la clase y la vigencia', (tester) async {
      servicio.horarios = [horarioDePrueba()];

      await montar(tester);

      expect(find.text('Mañanas'), findsOneWidget);
      expect(find.text('Activa'), findsOneWidget);
      expect(find.text('08:00 – 12:00 · 60 min'), findsOneWidget);
      // * 4 franjas por día en 2 días
      expect(find.text('Desde el 26/10/2026 · 8 franjas por semana'), findsOneWidget);
      for (final letra in ['L', 'M', 'X', 'J', 'V', 'S', 'D']) {
        expect(find.text(letra), findsOneWidget);
      }
    });

    testWidgets('con vigencia final muestra el rango de fechas', (tester) async {
      servicio.horarios = [horarioDePrueba(vigenteHasta: DateTime(2026, 12, 31))];

      await montar(tester);

      expect(find.text('Del 26/10/2026 al 31/12/2026 · 8 franjas por semana'), findsOneWidget);
    });

    testWidgets('un horario pausado muestra su chip y ofrece Reanudar y Desactivar, pero no Pausar', (tester) async {
      servicio.horarios = [horarioDePrueba(estado: EstadoPlantilla.pausada)];
      await montar(tester);

      expect(find.text('Pausada'), findsOneWidget);
      await abrirMenu(tester);

      expect(find.text('Reanudar'), findsOneWidget);
      expect(find.text('Desactivar'), findsOneWidget);
      expect(find.text('Pausar'), findsNothing);
    });

    testWidgets('un horario activo ofrece Pausar y Desactivar, pero no Reanudar', (tester) async {
      servicio.horarios = [horarioDePrueba()];
      await montar(tester);

      await abrirMenu(tester);

      expect(find.text('Pausar'), findsOneWidget);
      expect(find.text('Desactivar'), findsOneWidget);
      expect(find.text('Reanudar'), findsNothing);
    });

    testWidgets('un horario inactivo muestra su chip y no tiene menú de opciones', (tester) async {
      servicio.horarios = [horarioDePrueba(estado: EstadoPlantilla.inactiva)];
      await montar(tester);

      expect(find.text('Inactivo'), findsOneWidget);
      expect(find.byTooltip('Opciones del horario'), findsNothing);
    });

    testWidgets('por defecto pide activos y pausados, y "Ver inactivos" suma los inactivos', (tester) async {
      servicio.horarios = [horarioDePrueba(), horarioDePrueba(codigo: 'h2', estado: EstadoPlantilla.inactiva)];
      await montar(tester);
      expect(servicio.ultimosEstados, {EstadoPlantilla.activa, EstadoPlantilla.pausada});

      await tester.tap(find.byKey(const Key('interruptorInactivos')));
      await tester.pumpAndSettle();

      expect(servicio.ultimosEstados, {EstadoPlantilla.activa, EstadoPlantilla.pausada, EstadoPlantilla.inactiva});
      expect(find.text('Inactivo'), findsOneWidget);
    });

    testWidgets('un error al cargar muestra el mensaje y Reintentar vuelve a pedir', (tester) async {
      servicio.error = StateError('sin red');
      servicio.errorUnaVez = true;
      await montar(tester);
      expect(find.text('No pudimos conectar con el servidor. Intenta de nuevo.'), findsOneWidget);

      servicio.horarios = [horarioDePrueba()];
      await tester.tap(find.text('Reintentar'));
      await tester.pumpAndSettle();

      expect(find.text('Mañanas'), findsOneWidget);
    });
  });

  group('pausar', () {
    testWidgets('pide confirmación y, al confirmar, llama al servicio y avisa', (tester) async {
      servicio.horarios = [horarioDePrueba()];
      await montar(tester);

      await abrirMenu(tester);
      await tester.tap(find.text('Pausar'));
      await tester.pumpAndSettle();

      expect(find.text('Pausar horario'), findsOneWidget);
      expect(
        find.text(
          'Se quitarán las franjas futuras que nadie reservó. Las reservadas se conservan y podrás reanudarlo cuando quieras.',
        ),
        findsOneWidget,
      );
      expect(servicio.llamadas, isNot(contains('pausarPlantilla:h1')));

      await tester.tap(find.widgetWithText(FilledButton, 'Pausar'));
      await tester.pumpAndSettle();

      expect(servicio.llamadas, contains('pausarPlantilla:h1'));
      expect(find.text('Horario pausado'), findsOneWidget);
    });

    testWidgets('si se cancela la confirmación, no llama al servicio', (tester) async {
      servicio.horarios = [horarioDePrueba()];
      await montar(tester);

      await abrirMenu(tester);
      await tester.tap(find.text('Pausar'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Cancelar'));
      await tester.pumpAndSettle();

      expect(servicio.llamadas.where((l) => l.startsWith('pausarPlantilla')), isEmpty);
    });

    testWidgets('un error de negocio del backend se muestra tal cual', (tester) async {
      servicio.horarios = [horarioDePrueba()];
      await montar(tester);
      servicio.error = ApiException(code: 'ERR_ACA_11', message: 'Solo se puede pausar un horario activo');

      await abrirMenu(tester);
      await tester.tap(find.text('Pausar'));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Pausar'));
      await tester.pumpAndSettle();

      expect(find.text('Solo se puede pausar un horario activo'), findsOneWidget);
    });
  });

  group('reanudar', () {
    testWidgets('no pide confirmación: llama al servicio y avisa', (tester) async {
      servicio.horarios = [horarioDePrueba(estado: EstadoPlantilla.pausada)];
      await montar(tester);

      await abrirMenu(tester);
      await tester.tap(find.text('Reanudar'));
      await tester.pumpAndSettle();

      expect(servicio.llamadas, contains('reanudarPlantilla:h1'));
      expect(find.text('Horario reanudado'), findsOneWidget);
    });

    testWidgets('si ahora se cruza con otro horario, muestra el error del backend', (tester) async {
      servicio.horarios = [horarioDePrueba(estado: EstadoPlantilla.pausada)];
      await montar(tester);
      servicio.error = ApiException(code: 'ERR_ACA_04', message: 'El horario se cruza con otro horario activo tuyo');

      await abrirMenu(tester);
      await tester.tap(find.text('Reanudar'));
      await tester.pumpAndSettle();

      expect(find.text('El horario se cruza con otro horario activo tuyo'), findsOneWidget);
      expect(find.text('Horario reanudado'), findsNothing);
    });
  });

  group('desactivar', () {
    testWidgets('pide confirmación y, al confirmar, llama al servicio y avisa', (tester) async {
      servicio.horarios = [horarioDePrueba()];
      await montar(tester);

      await abrirMenu(tester);
      await tester.tap(find.text('Desactivar'));
      await tester.pumpAndSettle();

      expect(find.text('Desactivar horario'), findsOneWidget);
      expect(servicio.llamadas.where((l) => l.startsWith('desactivarPlantilla')), isEmpty);

      await tester.tap(find.widgetWithText(FilledButton, 'Desactivar'));
      await tester.pumpAndSettle();

      expect(servicio.llamadas, contains('desactivarPlantilla:h1'));
      expect(find.text('Horario desactivado'), findsOneWidget);
    });

    testWidgets('si se cancela, no llama al servicio', (tester) async {
      servicio.horarios = [horarioDePrueba()];
      await montar(tester);

      await abrirMenu(tester);
      await tester.tap(find.text('Desactivar'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Cancelar'));
      await tester.pumpAndSettle();

      expect(servicio.llamadas.where((l) => l.startsWith('desactivarPlantilla')), isEmpty);
    });

    testWidgets('si el horario ya estaba desactivado, muestra el error del backend', (tester) async {
      servicio.horarios = [horarioDePrueba()];
      await montar(tester);
      servicio.error = ApiException(code: 'ERR_ACA_06', message: 'El horario ya está desactivado');

      await abrirMenu(tester);
      await tester.tap(find.text('Desactivar'));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Desactivar'));
      await tester.pumpAndSettle();

      expect(find.text('El horario ya está desactivado'), findsOneWidget);
    });
  });
}
