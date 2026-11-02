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

  testWidgets('«Cerrar clase» solo se dibuja si la clase es cerrable y la pantalla dijo qué hacer', (tester) async {
    await montar(tester, ClaseAgendaCard(clase: claseDeAgendaDePrueba(cerrable: true)));
    expect(find.byKey(const Key('cerrar-r1')), findsNothing);

    await montar(tester, ClaseAgendaCard(clase: claseDeAgendaDePrueba(), alCerrar: () {}));
    expect(find.byKey(const Key('cerrar-r1')), findsNothing);

    var cierres = 0;
    await montar(
      tester,
      ClaseAgendaCard(clase: claseDeAgendaDePrueba(cerrable: true), alCerrar: () => cierres++),
    );
    expect(find.text('Cerrar clase'), findsOneWidget);
    await tester.tap(find.byKey(const Key('cerrar-r1')));
    expect(cierres, 1);
  });

  testWidgets('«Editar» solo se dibuja si la clase ya tiene su código y la pantalla dijo qué hacer', (tester) async {
    await montar(
      tester,
      ClaseAgendaCard(clase: claseDeAgendaDePrueba(estado: EstadoReserva.completada, claseCodigo: 'c1')),
    );
    expect(find.byKey(const Key('editar-r1')), findsNothing);

    await montar(
      tester,
      ClaseAgendaCard(clase: claseDeAgendaDePrueba(estado: EstadoReserva.completada), alEditar: () {}),
    );
    expect(find.byKey(const Key('editar-r1')), findsNothing);

    var ediciones = 0;
    await montar(
      tester,
      ClaseAgendaCard(
        clase: claseDeAgendaDePrueba(estado: EstadoReserva.noAsistio, claseCodigo: 'c1'),
        alEditar: () => ediciones++,
      ),
    );
    expect(find.text('Editar'), findsOneWidget);
    await tester.tap(find.byKey(const Key('editar-r1')));
    expect(ediciones, 1);
  });

  testWidgets('una clase ofrece a lo sumo una acción: cancelar, cerrar o editar', (tester) async {
    final casos = {
      'cancelar': claseDeAgendaDePrueba(cancelable: true),
      'cerrar': claseDeAgendaDePrueba(cerrable: true),
      'editar': claseDeAgendaDePrueba(estado: EstadoReserva.completada, claseCodigo: 'c1'),
    };
    for (final MapEntry(key: ofrecida, value: clase) in casos.entries) {
      await montar(tester, ClaseAgendaCard(clase: clase, alCancelar: () {}, alCerrar: () {}, alEditar: () {}));

      for (final accion in casos.keys) {
        expect(
          find.byKey(Key('$accion-r1')),
          accion == ofrecida ? findsOneWidget : findsNothing,
          reason: 'una clase para «$ofrecida» no debe ofrecer «$accion»',
        );
      }
    }
  });

  testWidgets('las clases sin ninguna acción no dibujan botones', (tester) async {
    for (final clase in [
      claseDeAgendaDePrueba(estado: EstadoReserva.cancelada),
      claseDeAgendaDePrueba(estado: EstadoReserva.completada),
      claseDeAgendaDePrueba(terminada: true),
    ]) {
      await montar(tester, ClaseAgendaCard(clase: clase, alCancelar: () {}, alCerrar: () {}, alEditar: () {}));

      expect(find.byType(TextButton), findsNothing);
      expect(find.byType(FilledButton), findsNothing);
    }
  });
}
