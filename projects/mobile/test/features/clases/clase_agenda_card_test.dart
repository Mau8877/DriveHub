import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/widgets/clase_agenda_card.dart';

import '../../helpers/clases_falsos.dart';

void main() {
  Future<void> montar(WidgetTester tester, ClaseAgendaCard card) {
    return tester.pumpWidget(
      MaterialApp(theme: AppTheme.light, home: Scaffold(body: card)),
    );
  }

  testWidgets('muestra el rango de horas, el estudiante y el estado', (tester) async {
    await montar(
      tester,
      ClaseAgendaCard(clase: claseDeAgendaDePrueba(horaInicio: '09:00', horaFin: '10:00', estudianteNombre: 'Ana Alfa')),
    );

    expect(find.text('09:00 – 10:00'), findsOneWidget);
    expect(find.text('Ana Alfa'), findsOneWidget);
    expect(find.text('Confirmada'), findsOneWidget);
  });

  testWidgets('una clase cancelada se atenúa con el color secundario del tema y sigue legible', (tester) async {
    await montar(
      tester,
      ClaseAgendaCard(clase: claseDeAgendaDePrueba(estudianteNombre: 'Ana Alfa', estado: EstadoReserva.cancelada)),
    );

    final secundario = AppTheme.light.colorScheme.onSurfaceVariant;
    expect(tester.widget<Text>(find.text('Ana Alfa')).style?.color, secundario);
    expect(tester.widget<Text>(find.text('10:00 – 11:00')).style?.color, secundario);
    expect(find.text('Cancelada'), findsOneWidget);
  });

  testWidgets('una clase que no está cancelada usa el color de texto normal', (tester) async {
    await montar(tester, ClaseAgendaCard(clase: claseDeAgendaDePrueba(estudianteNombre: 'Ana Alfa')));

    expect(tester.widget<Text>(find.text('Ana Alfa')).style?.color, isNot(AppTheme.light.colorScheme.onSurfaceVariant));
  });

  testWidgets('«Cancelar» solo se dibuja si la clase es cancelable y la pantalla dijo qué hacer', (tester) async {
    await montar(tester, ClaseAgendaCard(clase: claseDeAgendaDePrueba(cancelable: true)));
    expect(find.byKey(const Key('cancelar-r1')), findsNothing);

    await montar(tester, ClaseAgendaCard(clase: claseDeAgendaDePrueba(), alCancelar: () {}));
    expect(find.byKey(const Key('cancelar-r1')), findsNothing);

    var cancelaciones = 0;
    await montar(
      tester,
      ClaseAgendaCard(clase: claseDeAgendaDePrueba(cancelable: true), alCancelar: () => cancelaciones++),
    );
    await tester.tap(find.byKey(const Key('cancelar-r1')));
    expect(cancelaciones, 1);
  });

  testWidgets('el botón «Cancelar» usa el color de error del tema', (tester) async {
    await montar(tester, ClaseAgendaCard(clase: claseDeAgendaDePrueba(cancelable: true), alCancelar: () {}));

    final boton = tester.widget<TextButton>(find.byKey(const Key('cancelar-r1')));
    expect(boton.style?.foregroundColor?.resolve({}), AppTheme.light.colorScheme.error);
  });
}
