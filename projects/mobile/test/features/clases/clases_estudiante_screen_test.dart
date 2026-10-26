import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/models/vista_reservas.dart';
import 'package:mobile/features/clases/screens/clases_screen.dart';

import '../../helpers/clases_falsos.dart';
import '../../helpers/pagos_falsos.dart';
import '../../helpers/sesion_falsa.dart';

void main() {
  late ClasesServiceFalso servicio;
  late PagosServiceFalso pagos;

  final hoy = DateTime.now();
  final manana = DateTime(hoy.year, hoy.month, hoy.day + 1);
  final enCinco = DateTime(hoy.year, hoy.month, hoy.day + 5);

  setUp(() {
    servicio = ClasesServiceFalso();
    pagos = PagosServiceFalso()..saldo = 5;
  });

  Future<ProviderContainer> montar(WidgetTester tester, {List<String> roles = const ['ESTUDIANTE']}) async {
    final container = ProviderContainer(
      overrides: [
        clasesServiceFalso(servicio),
        pagosServiceFalso(pagos),
        sesionFalsa(
          SesionFalsa(
            EstadoSesion.conSesion(UsuarioSesion(codigo: 'u1', nombres: 'Ana', apellidos: 'Pérez', roles: roles)),
          ),
        ),
      ],
    );
    addTearDown(container.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(theme: AppTheme.light, home: const Scaffold(body: ClasesScreen())),
      ),
    );
    await tester.pumpAndSettle();
    return container;
  }

  testWidgets('el estudiante ve sus horas y las tres pestañas', (tester) async {
    await montar(tester);

    expect(find.byKey(const Key('bannerHoras')), findsOneWidget);
    expect(find.text('Mis clases'), findsOneWidget);
    expect(find.text('Reservar'), findsOneWidget);
    expect(find.text('Historial'), findsOneWidget);
  });

  testWidgets('con clases por venir abre en «Mis clases», agrupadas por día y de la más próxima a la más lejana', (
    tester,
  ) async {
    servicio.proximas = [
      reservaDePrueba(codigo: 'a', fecha: manana, horaInicio: '08:00', horaFin: '09:00', instructorNombre: 'Ana Alfa'),
      reservaDePrueba(codigo: 'b', fecha: manana, horaInicio: '10:00', horaFin: '11:00', instructorNombre: 'Beto Beta'),
      reservaDePrueba(codigo: 'c', fecha: enCinco, horaInicio: '15:00', horaFin: '16:00'),
    ];
    await montar(tester);

    expect(find.text('Mañana'), findsOneWidget);
    expect(find.text('08:00 – 09:00'), findsOneWidget);
    expect(find.text('Ana Alfa'), findsOneWidget);
    expect(find.text('Beto Beta'), findsOneWidget);
    expect(find.text('15:00 – 16:00'), findsOneWidget);
    // * el orden lo pone el backend y la pantalla lo respeta: a, b, c de arriba hacia abajo
    final posiciones = [
      for (final c in ['a', 'b', 'c']) tester.getTopLeft(find.byKey(Key('reserva-$c'))).dy,
    ];
    expect(posiciones, [...posiciones]..sort());
    // * en «Mis clases» no hace falta la insignia de estado
    expect(find.text('Confirmada'), findsNothing);
  });

  testWidgets('sin clases por venir abre en «Reservar»', (tester) async {
    await montar(tester);

    expect(find.byKey(const Key('tiraDeDias')), findsOneWidget);
  });

  testWidgets('«Mis clases» vacía ofrece ir a reservar', (tester) async {
    final container = await montar(tester);
    await tester.tap(find.text('Mis clases'));
    await tester.pumpAndSettle();

    expect(find.text('Aún no tienes clases reservadas'), findsOneWidget);
    await tester.tap(find.byKey(const Key('irAReservar')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('tiraDeDias')), findsOneWidget);
    expect(container, isNotNull);
  });

  testWidgets('el historial muestra cada estado en español', (tester) async {
    servicio.historial = [
      reservaDePrueba(codigo: 'h1', estado: EstadoReserva.completada),
      reservaDePrueba(codigo: 'h2', estado: EstadoReserva.noAsistio),
      reservaDePrueba(codigo: 'h3', estado: EstadoReserva.cancelada),
      reservaDePrueba(codigo: 'h4', estado: EstadoReserva.confirmada, terminada: true),
    ];
    await montar(tester);
    await tester.tap(find.text('Historial'));
    await tester.pumpAndSettle();

    expect(find.text('Realizada'), findsOneWidget);
    expect(find.text('No asistió'), findsOneWidget);
    expect(find.text('Cancelada'), findsOneWidget);
    expect(find.text('Pendiente de cierre'), findsOneWidget);
    expect(servicio.pedidosDeReservas, contains(VistaReservas.historial));
  });

  testWidgets('el historial vacío lo explica', (tester) async {
    await montar(tester);
    await tester.tap(find.text('Historial'));
    await tester.pumpAndSettle();

    expect(find.text('Todavía no tienes clases pasadas'), findsOneWidget);
  });

  testWidgets('un error de red muestra el aviso y «Reintentar» vuelve a pedir', (tester) async {
    servicio.errorReservas = Exception('sin red');
    await montar(tester);
    await tester.tap(find.text('Historial'));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('errorClases-historial')), findsOneWidget);
    expect(find.textContaining('Revisa tu conexión'), findsOneWidget);

    servicio.errorReservas = null;
    servicio.historial = [reservaDePrueba(codigo: 'h1', estado: EstadoReserva.completada)];
    await tester.tap(find.text('Reintentar'));
    await tester.pumpAndSettle();

    expect(find.text('Realizada'), findsOneWidget);
  });

  testWidgets('un error del backend muestra su mensaje en español', (tester) async {
    servicio.errorReservas = ApiException(code: 'ERR_ACA_17', message: 'No pudimos obtener los datos de los instructores.');
    await montar(tester);
    await tester.tap(find.text('Historial'));
    await tester.pumpAndSettle();

    expect(find.text('No pudimos obtener los datos de los instructores.'), findsOneWidget);
  });

  testWidgets('el instructor sigue viendo su agenda y no las pestañas del estudiante', (tester) async {
    await montar(tester, roles: const ['INSTRUCTOR']);

    expect(find.text('No tienes clases hoy'), findsOneWidget);
    expect(find.byKey(const Key('pestanasClases')), findsNothing);
    expect(find.byKey(const Key('bannerHoras')), findsNothing);
  });
}
