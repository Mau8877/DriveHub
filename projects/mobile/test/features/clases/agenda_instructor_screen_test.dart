import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/providers/agenda_fecha_provider.dart';
import 'package:mobile/features/clases/screens/agenda_instructor_screen.dart';

import '../../helpers/clases_falsos.dart';

void main() {
  late ClasesServiceFalso servicio;

  setUp(() => servicio = ClasesServiceFalso());

  Future<ProviderContainer> montar(WidgetTester tester, {DateTime? dia}) async {
    final container = ProviderContainer(overrides: [clasesServiceFalso(servicio)]);
    addTearDown(container.dispose);
    if (dia != null) container.read(agendaFechaUiProvider.notifier).elegir(dia);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(theme: AppTheme.light, home: const Scaffold(body: AgendaInstructorScreen())),
      ),
    );
    await tester.pumpAndSettle();
    return container;
  }

  testWidgets('muestra las clases del día en el orden del servidor, con la hora y el estudiante', (tester) async {
    servicio.clasesDeAgenda = [
      claseDeAgendaDePrueba(codigo: 'a', horaInicio: '08:00', horaFin: '09:00', estudianteNombre: 'Ana Alfa'),
      claseDeAgendaDePrueba(codigo: 'b', horaInicio: '10:00', horaFin: '11:00', estudianteNombre: 'Beto Beta'),
      claseDeAgendaDePrueba(codigo: 'c', horaInicio: '15:00', horaFin: '16:00', estudianteNombre: 'Carla Gama'),
    ];
    await montar(tester);

    expect(find.text('08:00 – 09:00'), findsOneWidget);
    expect(find.text('Ana Alfa'), findsOneWidget);
    expect(find.text('Beto Beta'), findsOneWidget);
    expect(find.text('Carla Gama'), findsOneWidget);
    final posiciones = [for (final c in ['a', 'b', 'c']) tester.getTopLeft(find.byKey(Key('clase-$c'))).dy];
    expect(posiciones, [...posiciones]..sort());
  });

  testWidgets('pide el día de hoy y lo titula «Hoy»', (tester) async {
    await montar(tester);

    expect(servicio.pedidosDeAgenda.single, DateTime.now().copyWith(hour: 0, minute: 0, second: 0, millisecond: 0, microsecond: 0));
    expect(find.text('Hoy'), findsWidgets);
    expect(find.byKey(const Key('irAHoy')), findsNothing);
  });

  testWidgets('un día sin clases muestra el aviso vacío y no un error', (tester) async {
    await montar(tester);

    expect(find.byKey(const Key('agendaVacia')), findsOneWidget);
    expect(find.text('No tienes clases este día'), findsOneWidget);
    expect(find.byKey(const Key('errorAgenda')), findsNothing);
  });

  testWidgets('«día siguiente» y «día anterior» cambian el día y vuelven a pedir la agenda', (tester) async {
    final container = await montar(tester, dia: DateTime(2026, 11, 2));

    await tester.tap(find.byKey(const Key('diaSiguiente')));
    await tester.pumpAndSettle();
    expect(container.read(agendaFechaUiProvider), DateTime(2026, 11, 3));

    await tester.tap(find.byKey(const Key('diaAnterior')));
    await tester.tap(find.byKey(const Key('diaAnterior')));
    await tester.pumpAndSettle();
    expect(container.read(agendaFechaUiProvider), DateTime(2026, 11, 1));
    expect(servicio.pedidosDeAgenda.last, DateTime(2026, 11, 1));
    expect(find.text('01/11/2026'), findsOneWidget);
  });

  testWidgets('«Hoy» solo aparece fuera de hoy y vuelve al día de hoy', (tester) async {
    final container = await montar(tester);
    expect(find.byKey(const Key('irAHoy')), findsNothing);

    await tester.tap(find.byKey(const Key('diaSiguiente')));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('irAHoy')), findsOneWidget);
    expect(find.text('Mañana'), findsOneWidget);

    await tester.tap(find.byKey(const Key('irAHoy')));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('irAHoy')), findsNothing);
    final hoy = DateTime.now();
    expect(container.read(agendaFechaUiProvider), DateTime(hoy.year, hoy.month, hoy.day));
  });

  testWidgets('tocar el título abre el selector de fecha y elegir un día lo mira', (tester) async {
    final container = await montar(tester, dia: DateTime(2026, 11, 2));

    await tester.tap(find.byKey(const Key('elegirDia')));
    await tester.pumpAndSettle();
    expect(find.byType(DatePickerDialog), findsOneWidget);
    await tester.tap(find.text('15'));
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();

    expect(container.read(agendaFechaUiProvider), DateTime(2026, 11, 15));
    expect(servicio.pedidosDeAgenda.last, DateTime(2026, 11, 15));
  });

  testWidgets('cancelar el selector deja el mismo día', (tester) async {
    final container = await montar(tester, dia: DateTime(2026, 11, 2));

    await tester.tap(find.byKey(const Key('elegirDia')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Cancel'));
    await tester.pumpAndSettle();

    expect(container.read(agendaFechaUiProvider), DateTime(2026, 11, 2));
  });

  testWidgets('un error del backend (Identidad caída) muestra su mensaje y «Reintentar» vuelve a pedir', (tester) async {
    servicio.errorAgenda = ApiException(code: 'ERR_ACA_17', message: 'No pudimos obtener los datos de los estudiantes.');
    await montar(tester);

    expect(find.byKey(const Key('errorAgenda')), findsOneWidget);
    expect(find.text('No pudimos obtener los datos de los estudiantes.'), findsOneWidget);

    servicio.errorAgenda = null;
    servicio.clasesDeAgenda = [claseDeAgendaDePrueba(estudianteNombre: 'Ana Alfa')];
    await tester.tap(find.text('Reintentar'));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('errorAgenda')), findsNothing);
    expect(find.text('Ana Alfa'), findsOneWidget);
    expect(servicio.pedidosDeAgenda, hasLength(2));
  });

  testWidgets('sin red muestra un mensaje genérico y no da el día por vacío', (tester) async {
    servicio.errorAgenda = Exception('sin red');
    await montar(tester);

    expect(find.text('No pudimos conectarnos. Revisa tu conexión e inténtalo de nuevo.'), findsOneWidget);
    expect(find.byKey(const Key('agendaVacia')), findsNothing);
  });

  testWidgets('muestra la insignia de cada estado, con «Pendiente de cierre» para una confirmada terminada', (
    tester,
  ) async {
    servicio.clasesDeAgenda = [
      claseDeAgendaDePrueba(codigo: 'a', estado: EstadoReserva.confirmada),
      claseDeAgendaDePrueba(codigo: 'b', estado: EstadoReserva.confirmada, terminada: true),
      claseDeAgendaDePrueba(codigo: 'c', estado: EstadoReserva.completada),
      claseDeAgendaDePrueba(codigo: 'd', estado: EstadoReserva.noAsistio),
      claseDeAgendaDePrueba(codigo: 'e', estado: EstadoReserva.cancelada),
    ];
    await montar(tester);

    expect(find.text('Confirmada'), findsOneWidget);
    expect(find.text('Pendiente de cierre'), findsOneWidget);
    expect(find.text('Completada'), findsOneWidget);
    expect(find.text('No asistió'), findsOneWidget);
    expect(find.text('Cancelada'), findsOneWidget);
  });
}
