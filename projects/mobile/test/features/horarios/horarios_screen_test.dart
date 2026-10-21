import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
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

  testWidgets('sin horarios muestra el mensaje para crear el primero', (tester) async {
    await montar(tester);

    expect(find.text('Todavía no tienes horarios. Crea el primero.'), findsOneWidget);
  });

  testWidgets('muestra cada horario con el día, la ventana, la clase y la vigencia', (tester) async {
    servicio.horarios = [horarioDePrueba(), horarioDePrueba(codigo: 'h2', dia: 3)];

    await montar(tester);

    expect(find.text('Lunes · 08:00 – 12:00'), findsOneWidget);
    expect(find.text('Miércoles · 08:00 – 12:00'), findsOneWidget);
    expect(find.text('Clases de 60 min · desde 26/10/2026'), findsNWidgets(2));
  });

  testWidgets('por defecto pide solo los activos y "Ver inactivos" pide todos', (tester) async {
    servicio.horarios = [horarioDePrueba(), horarioDePrueba(codigo: 'h2', activa: false)];
    await montar(tester);
    expect(servicio.ultimaActiva, isTrue);

    await tester.tap(find.byKey(const Key('interruptorInactivos')));
    await tester.pumpAndSettle();

    expect(servicio.ultimaActiva, isNull);
    expect(find.text('Inactivo'), findsOneWidget);
  });

  testWidgets('desactivar pide confirmación y, al confirmar, llama al servicio y avisa', (tester) async {
    servicio.horarios = [horarioDePrueba()];
    await montar(tester);

    await tester.tap(find.byTooltip('Opciones del horario'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Desactivar'));
    await tester.pumpAndSettle();
    expect(find.text('Se quitarán las franjas futuras que nadie reservó. Las reservadas se conservan.'), findsOneWidget);
    expect(servicio.llamadas, isNot(contains('desactivarPlantilla:h1')));

    await tester.tap(find.widgetWithText(FilledButton, 'Desactivar'));
    await tester.pumpAndSettle();

    expect(servicio.llamadas, contains('desactivarPlantilla:h1'));
    expect(find.text('Horario desactivado'), findsOneWidget);
  });

  testWidgets('cancelar la confirmación no desactiva nada', (tester) async {
    servicio.horarios = [horarioDePrueba()];
    await montar(tester);

    await tester.tap(find.byTooltip('Opciones del horario'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Desactivar'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Cancelar'));
    await tester.pumpAndSettle();

    expect(servicio.llamadas.where((l) => l.startsWith('desactivarPlantilla')), isEmpty);
  });

  testWidgets('si desactivar falla, muestra el mensaje del backend', (tester) async {
    servicio.horarios = [horarioDePrueba()];
    await montar(tester);
    servicio
      ..error = ApiException(code: 'ERR_ACA_06', message: 'El horario ya está desactivado')
      ..errorUnaVez = true;

    await tester.tap(find.byTooltip('Opciones del horario'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Desactivar'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Desactivar'));
    await tester.pumpAndSettle();

    expect(find.text('El horario ya está desactivado'), findsOneWidget);
  });

  testWidgets('un horario inactivo no ofrece desactivar', (tester) async {
    servicio.horarios = [horarioDePrueba(activa: false)];
    await montar(tester);
    await tester.tap(find.byKey(const Key('interruptorInactivos')));
    await tester.pumpAndSettle();

    expect(find.byTooltip('Opciones del horario'), findsNothing);
    expect(find.text('Inactivo'), findsOneWidget);
  });

  testWidgets('si la carga falla, muestra el error y Reintentar vuelve a pedir', (tester) async {
    servicio.error = StateError('sin red');
    await montar(tester);
    expect(find.text('No pudimos conectar con el servidor. Intenta de nuevo.'), findsOneWidget);

    servicio.error = null;
    servicio.horarios = [horarioDePrueba()];
    await tester.tap(find.text('Reintentar'));
    await tester.pumpAndSettle();

    expect(find.text('Lunes · 08:00 – 12:00'), findsOneWidget);
  });
}
