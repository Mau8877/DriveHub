import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/horarios/models/fechas.dart';
import 'package:mobile/features/horarios/models/franja.dart';
import 'package:mobile/features/horarios/screens/franjas_screen.dart';

import '../../helpers/horarios_falsos.dart';

void main() {
  late HorariosServiceFalso servicio;

  // * La pantalla compara con la hora real del teléfono: para no depender de la hora a la que
  // * corre el test se usan franjas de mañana (futuras) y de ayer (pasadas)
  final manana = soloFecha(DateTime.now().add(const Duration(days: 1)));
  final ayer = soloFecha(DateTime.now().subtract(const Duration(days: 1)));

  setUp(() => servicio = HorariosServiceFalso());

  Future<void> montar(WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [horariosServiceFalso(servicio)],
        child: const MaterialApp(home: FranjasScreen()),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('sin franjas ese día muestra el mensaje', (tester) async {
    await montar(tester);

    expect(find.text('No tienes franjas este día'), findsOneWidget);
  });

  testWidgets('muestra la ventana y el estado de cada franja', (tester) async {
    servicio.franjas = [
      franjaDePrueba(codigo: 'f1', fecha: manana),
      franjaDePrueba(codigo: 'f2', fecha: manana, horaInicio: '11:00', horaFin: '12:00', estado: EstadoFranja.reservada),
      franjaDePrueba(codigo: 'f3', fecha: manana, horaInicio: '12:00', horaFin: '13:00', estado: EstadoFranja.bloqueada),
    ];

    await montar(tester);

    expect(find.text('10:00 – 11:00'), findsOneWidget);
    expect(find.text('Libre'), findsOneWidget);
    expect(find.text('Reservada'), findsOneWidget);
    expect(find.text('Bloqueada'), findsOneWidget);
  });

  testWidgets('ofrece Bloquear en la libre, Desbloquear en la bloqueada y nada en la reservada ni en la pasada', (
    tester,
  ) async {
    servicio.franjas = [
      franjaDePrueba(codigo: 'f1', fecha: manana),
      franjaDePrueba(codigo: 'f2', fecha: manana, estado: EstadoFranja.reservada),
      franjaDePrueba(codigo: 'f3', fecha: manana, estado: EstadoFranja.bloqueada),
      franjaDePrueba(codigo: 'f4', fecha: ayer),
    ];

    await montar(tester);

    expect(find.text('Bloquear'), findsOneWidget);
    expect(find.text('Desbloquear'), findsOneWidget);
    // * De las 4 franjas solo 2 tienen acción: la reservada (f2) y la de ayer (f4) no ofrecen ninguna
  });

  testWidgets('bloquear llama al servicio con el código de la franja y avisa', (tester) async {
    servicio.franjas = [franjaDePrueba(codigo: 'f1', fecha: manana)];
    await montar(tester);

    await tester.tap(find.text('Bloquear'));
    await tester.pumpAndSettle();

    expect(servicio.llamadas, contains('bloquearFranja:f1'));
    expect(find.text('Franja bloqueada'), findsOneWidget);
  });

  testWidgets('desbloquear llama al servicio y avisa', (tester) async {
    servicio.franjas = [franjaDePrueba(codigo: 'f3', fecha: manana, estado: EstadoFranja.bloqueada)];
    await montar(tester);

    await tester.tap(find.text('Desbloquear'));
    await tester.pumpAndSettle();

    expect(servicio.llamadas, contains('desbloquearFranja:f3'));
    expect(find.text('Franja desbloqueada'), findsOneWidget);
  });

  testWidgets('si el backend rechaza, muestra su mensaje y recarga la lista', (tester) async {
    servicio.franjas = [franjaDePrueba(codigo: 'f1', fecha: manana)];
    await montar(tester);
    final cargasAntes = servicio.llamadas.where((l) => l == 'listarFranjas').length;
    servicio
      ..error = ApiException(code: 'ERR_ACA_08', message: 'La franja no está libre: solo se bloquean las libres')
      ..errorUnaVez = true;

    await tester.tap(find.text('Bloquear'));
    await tester.pumpAndSettle();

    expect(find.text('La franja no está libre: solo se bloquean las libres'), findsOneWidget);
    expect(servicio.llamadas.where((l) => l == 'listarFranjas').length, greaterThan(cargasAntes));
  });

  testWidgets('un error de red al bloquear muestra el aviso genérico', (tester) async {
    servicio.franjas = [franjaDePrueba(codigo: 'f1', fecha: manana)];
    await montar(tester);
    servicio
      ..error = StateError('sin red')
      ..errorUnaVez = true;

    await tester.tap(find.text('Bloquear'));
    await tester.pumpAndSettle();

    expect(find.text('No pudimos conectar con el servidor. Intenta de nuevo.'), findsOneWidget);
  });

  testWidgets('la flecha de día siguiente pide las franjas de mañana', (tester) async {
    await montar(tester);

    await tester.tap(find.byTooltip('Día siguiente'));
    await tester.pumpAndSettle();

    expect(servicio.ultimaFecha, manana);
  });

  testWidgets('la flecha de día anterior pide las franjas de ayer', (tester) async {
    await montar(tester);

    await tester.tap(find.byTooltip('Día anterior'));
    await tester.pumpAndSettle();

    expect(servicio.ultimaFecha, ayer);
  });

  testWidgets('si la carga falla, muestra el error y Reintentar vuelve a pedir', (tester) async {
    servicio.error = StateError('sin red');
    await montar(tester);
    expect(find.text('No pudimos conectar con el servidor. Intenta de nuevo.'), findsOneWidget);

    servicio.error = null;
    servicio.franjas = [franjaDePrueba(codigo: 'f1', fecha: manana)];
    await tester.tap(find.text('Reintentar'));
    await tester.pumpAndSettle();

    expect(find.text('10:00 – 11:00'), findsOneWidget);
  });
}
