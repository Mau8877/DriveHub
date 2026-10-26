import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/clases/models/reservas_resultado.dart';
import 'package:mobile/features/clases/providers/clases_pestana_provider.dart';
import 'package:mobile/features/clases/providers/franjas_seleccionadas_provider.dart';

import '../../helpers/clases_app.dart';
import '../../helpers/clases_falsos.dart';
import '../../helpers/pagos_falsos.dart';

void main() {
  late ClasesServiceFalso servicio;
  late PagosServiceFalso pagos;

  setUp(() {
    servicio = ClasesServiceFalso()
      ..franjas = [
        franjaDisponibleDePrueba(codigo: 'a', horaInicio: '08:00', horaFin: '09:00'),
        franjaDisponibleDePrueba(codigo: 'b', horaInicio: '09:00', horaFin: '10:00'),
        franjaDisponibleDePrueba(codigo: 'c', horaInicio: '15:00', horaFin: '16:00', instructorNombre: 'Beto Beta'),
      ];
    pagos = PagosServiceFalso()..saldo = 5;
  });

  /// Monta «Reservar», elige las franjas dadas y abre el resumen.
  Future<MontajeDeClases> elegirYAbrirResumen(WidgetTester tester, List<String> codigos) async {
    final montaje = await montarClases(tester, servicio: servicio, pagos: pagos);
    for (final codigo in codigos) {
      await tester.tap(find.byKey(Key('franja-$codigo')));
    }
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('confirmar')));
    await tester.pumpAndSettle();
    return montaje;
  }

  testWidgets('el resumen lista las clases elegidas y cuántas horas se gastarán, sin reservar nada todavía', (tester) async {
    await elegirYAbrirResumen(tester, ['a', 'c']);

    expect(find.text('Confirma tus clases'), findsOneWidget);
    expect(find.byKey(const Key('resumen-a')), findsOneWidget);
    expect(find.byKey(const Key('resumen-c')), findsOneWidget);
    expect(find.text('Beto Beta'), findsWidgets);
    expect(find.text('Gastarás 2 h de tus 5 h. Te quedarán 3 h.'), findsOneWidget);
    expect(servicio.reservasPedidas, isEmpty);
  });

  testWidgets('«Cancelar» cierra el resumen sin reservar y deja la selección', (tester) async {
    final montaje = await elegirYAbrirResumen(tester, ['a']);

    await tester.tap(find.byKey(const Key('cancelarResumen')));
    await tester.pumpAndSettle();

    expect(find.text('Confirma tus clases'), findsNothing);
    expect(servicio.reservasPedidas, isEmpty);
    expect(montaje.container.read(franjasSeleccionadasUiProvider).keys, ['a']);
  });

  testWidgets('un doble toque en «Confirmar reserva» manda una sola petición', (tester) async {
    final espera = Completer<void>();
    servicio.antesDeReservar = () => espera.future;
    await elegirYAbrirResumen(tester, ['a']);

    await tester.tap(find.byKey(const Key('confirmarReserva')));
    await tester.pump();
    await tester.tap(find.byKey(const Key('confirmarReserva')), warnIfMissed: false);
    await tester.pump();

    expect(servicio.reservasPedidas, hasLength(1));
    expect(find.byType(CircularProgressIndicator), findsOneWidget);

    espera.complete();
    await tester.pumpAndSettle();
    expect(servicio.reservasPedidas, hasLength(1));
  });

  testWidgets('con todo reservado muestra las clases y lo que le queda', (tester) async {
    servicio.alReservar = (codigos) => ReservasResultado(
      reservadas: [for (final c in codigos) reservaDePrueba(codigo: 'r-$c')],
      rechazadas: const [],
      horasDisponibles: 3,
    );
    await elegirYAbrirResumen(tester, ['a', 'b']);

    await tester.tap(find.byKey(const Key('confirmarReserva')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('tituloReservadas')), findsOneWidget);
    expect(find.text('Clases reservadas (2)'), findsOneWidget);
    expect(find.byKey(const Key('reservada-r-a')), findsOneWidget);
    expect(find.byKey(const Key('tituloRechazadas')), findsNothing);
    expect(find.text('Te quedan 3 h'), findsOneWidget);
    expect(find.byKey(const Key('verMisClases')), findsOneWidget);
    expect(servicio.reservasPedidas.single, ['a', 'b']);
  });

  testWidgets('con una franja tomada por otro dice cuál, a qué hora y por qué, y las demás quedan reservadas', (
    tester,
  ) async {
    servicio.alReservar = (codigos) => ReservasResultado(
      reservadas: [reservaDePrueba(codigo: 'r-a')],
      rechazadas: [rechazadaDePrueba(franjaCodigo: 'b')],
      horasDisponibles: 4,
    );
    await elegirYAbrirResumen(tester, ['a', 'b']);

    await tester.tap(find.byKey(const Key('confirmarReserva')));
    await tester.pumpAndSettle();

    expect(find.text('Clases reservadas (1)'), findsOneWidget);
    expect(find.text('No se pudieron reservar (1)'), findsOneWidget);
    final rechazada = find.byKey(const Key('rechazada-b'));
    expect(rechazada, findsOneWidget);
    expect(find.descendant(of: rechazada, matching: find.textContaining('11:00 – 12:00')), findsOneWidget);
    expect(find.descendant(of: rechazada, matching: find.textContaining('Ivan Alfa')), findsOneWidget);
    expect(find.descendant(of: rechazada, matching: find.textContaining('Otro estudiante ya la reservó')), findsOneWidget);
    expect(find.byKey(const Key('ningunaHoraGastada')), findsNothing);
  });

  testWidgets('si ninguna se reservó lo dice, no se gastó ninguna hora y ofrece «Entendido»', (tester) async {
    servicio.alReservar = (codigos) => ReservasResultado(
      reservadas: const [],
      rechazadas: [rechazadaDePrueba(franjaCodigo: 'a', error: 'ERR_ACA_19', mensaje: 'No tienes horas suficientes')],
      horasDisponibles: 0,
    );
    await elegirYAbrirResumen(tester, ['a']);

    await tester.tap(find.byKey(const Key('confirmarReserva')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('tituloReservadas')), findsNothing);
    expect(find.byKey(const Key('ningunaHoraGastada')), findsOneWidget);
    expect(find.text('No se descontó ninguna hora.'), findsOneWidget);
    expect(find.descendant(of: find.byKey(const Key('rechazada-a')), matching: find.textContaining('No tienes horas suficientes')), findsOneWidget);
    expect(find.byKey(const Key('verMisClases')), findsNothing);

    await tester.tap(find.byKey(const Key('entendido')));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('tituloRechazadas')), findsNothing);
  });

  testWidgets('«Ver mis clases» cierra el resultado y lleva a «Mis clases»', (tester) async {
    final montaje = await elegirYAbrirResumen(tester, ['a']);

    await tester.tap(find.byKey(const Key('confirmarReserva')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('verMisClases')));
    await tester.pumpAndSettle();

    expect(montaje.container.read(clasesPestanaUiProvider), PestanaClases.misClases);
    expect(find.byKey(const Key('tituloReservadas')), findsNothing);
  });

  testWidgets('tras confirmar, la selección queda vacía y el indicador muestra el saldo real', (tester) async {
    final montaje = await elegirYAbrirResumen(tester, ['a', 'b']);
    pagos.saldo = 3;

    await tester.tap(find.byKey(const Key('confirmarReserva')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('verMisClases')));
    await tester.pumpAndSettle();

    expect(montaje.container.read(franjasSeleccionadasUiProvider), isEmpty);
    expect(find.byKey(const Key('horasVistaPrevia')), findsNothing);
    expect(find.textContaining('3 h', findRichText: true), findsWidgets);
  });

  testWidgets('las que fallaron por un corte de Pagos se pueden reintentar: vuelven a quedar elegidas', (tester) async {
    servicio.alReservar = (codigos) => ReservasResultado(
      reservadas: [reservaDePrueba(codigo: 'r-a')],
      rechazadas: [rechazadaDePrueba(franjaCodigo: 'b', error: 'ERR_ACA_17', mensaje: 'No pudimos descontar tu hora')],
      horasDisponibles: 4,
    );
    final montaje = await elegirYAbrirResumen(tester, ['a', 'b']);

    await tester.tap(find.byKey(const Key('confirmarReserva')));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('reintentar')), findsOneWidget);

    await tester.tap(find.byKey(const Key('reintentar')));
    await tester.pumpAndSettle();

    expect(montaje.container.read(franjasSeleccionadasUiProvider).keys, ['b']);
    expect(find.text('Confirmar (1)'), findsOneWidget);
  });

  testWidgets('una franja tomada por otro no ofrece reintentar', (tester) async {
    servicio.alReservar = (codigos) => ReservasResultado(
      reservadas: const [],
      rechazadas: [rechazadaDePrueba(franjaCodigo: 'a')],
      horasDisponibles: 5,
    );
    await elegirYAbrirResumen(tester, ['a']);

    await tester.tap(find.byKey(const Key('confirmarReserva')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('reintentar')), findsNothing);
  });

  testWidgets('sin red, el resumen se mantiene con el aviso y la selección intacta', (tester) async {
    servicio.errorAlReservar = DioException(requestOptions: RequestOptions(path: '/reservas'));
    final montaje = await elegirYAbrirResumen(tester, ['a']);

    await tester.tap(find.byKey(const Key('confirmarReserva')));
    await tester.pumpAndSettle();

    expect(find.text('Confirma tus clases'), findsOneWidget);
    expect(find.byKey(const Key('errorConfirmar')), findsOneWidget);
    expect(find.textContaining('Revisa tu conexión'), findsOneWidget);
    expect(find.byKey(const Key('tituloReservadas')), findsNothing);
    expect(montaje.container.read(franjasSeleccionadasUiProvider).keys, ['a']);

    // * y se puede reintentar
    servicio.errorAlReservar = null;
    await tester.tap(find.byKey(const Key('confirmarReserva')));
    await tester.pumpAndSettle();
    expect(find.text('Clases reservadas (1)'), findsOneWidget);
  });
}
