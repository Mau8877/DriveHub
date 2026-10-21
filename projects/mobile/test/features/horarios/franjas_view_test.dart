import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/horarios/models/fechas.dart';
import 'package:mobile/features/horarios/models/franja.dart';
import 'package:mobile/features/horarios/screens/franjas_view.dart';

import '../../helpers/horarios_falsos.dart';

void main() {
  late HorariosServiceFalso servicio;

  // * La vista compara con la hora real del teléfono: para no depender de la hora a la que corre el
  // * test se usan franjas de mañana (futuras) y de ayer (pasadas)
  final manana = soloFecha(DateTime.now().add(const Duration(days: 1)));
  final ayer = soloFecha(DateTime.now().subtract(const Duration(days: 1)));

  setUp(() => servicio = HorariosServiceFalso());

  Future<void> montar(WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [horariosServiceFalso(servicio)],
        child: const MaterialApp(home: Scaffold(body: FranjasView())),
      ),
    );
    await tester.pumpAndSettle();
  }

  Franja franja(String codigo, EstadoFranja estado, {DateTime? fecha, String inicio = '10:00', String fin = '11:00'}) =>
      franjaDePrueba(codigo: codigo, estado: estado, fecha: fecha ?? manana, horaInicio: inicio, horaFin: fin);

  testWidgets('sin franjas en la semana muestra el mensaje', (tester) async {
    await montar(tester);

    expect(find.text('Aún no tienes franjas. Crea un horario para generarlas.'), findsOneWidget);
  });

  testWidgets('muestra la semana y pide del lunes al domingo', (tester) async {
    await montar(tester);

    final lunes = lunesDeLaSemana(DateTime.now());
    expect(find.text('Semana ${etiquetaDeSemana(lunes)}'), findsOneWidget);
    expect(servicio.ultimoDesde, lunes);
    expect(servicio.ultimoHasta, sumarDias(lunes, 6));
  });

  testWidgets('muestra la ventana y el estado de cada franja, y el resumen por estado', (tester) async {
    servicio.franjas = [
      franja('f1', EstadoFranja.libre),
      franja('f2', EstadoFranja.libre, inicio: '11:00', fin: '12:00'),
      franja('f3', EstadoFranja.reservada, inicio: '12:00', fin: '13:00'),
      franja('f4', EstadoFranja.bloqueada, inicio: '13:00', fin: '14:00'),
    ];

    await montar(tester);

    expect(find.text('10:00 – 11:00'), findsOneWidget);
    expect(find.text('2 Libres'), findsOneWidget);
    expect(find.text('1 Reservadas'), findsOneWidget);
    expect(find.text('1 Bloqueadas'), findsOneWidget);
    expect(find.text('Libre'), findsNWidgets(2));
    expect(find.text('Reservada'), findsOneWidget);
    expect(find.text('Bloqueada'), findsOneWidget);
  });

  testWidgets('agrupa las franjas por día con su subtítulo', (tester) async {
    final pasado = manana.add(const Duration(days: 1));
    servicio.franjas = [franja('f1', EstadoFranja.libre), franja('f2', EstadoFranja.libre, fecha: pasado)];

    await montar(tester);

    expect(find.text(diaConNumero(manana)), findsOneWidget);
    expect(find.text(diaConNumero(pasado)), findsOneWidget);
  });

  testWidgets('ofrece Bloquear en la libre, Desbloquear en la bloqueada y nada en la reservada ni en la pasada', (
    tester,
  ) async {
    servicio.franjas = [
      franja('f1', EstadoFranja.libre),
      franja('f2', EstadoFranja.reservada),
      franja('f3', EstadoFranja.bloqueada),
      franja('f4', EstadoFranja.libre, fecha: ayer),
    ];

    await montar(tester);

    expect(find.text('Bloquear'), findsOneWidget);
    expect(find.text('Desbloquear'), findsOneWidget);
  });

  testWidgets('bloquear una franja llama al servicio y avisa', (tester) async {
    servicio.franjas = [franja('f1', EstadoFranja.libre)];
    await montar(tester);

    await tester.tap(find.text('Bloquear'));
    await tester.pumpAndSettle();

    expect(servicio.llamadas, contains('bloquearFranja:f1'));
    expect(find.text('Franja bloqueada'), findsOneWidget);
  });

  testWidgets('desbloquear una franja llama al servicio y avisa', (tester) async {
    servicio.franjas = [franja('f3', EstadoFranja.bloqueada)];
    await montar(tester);

    await tester.tap(find.text('Desbloquear'));
    await tester.pumpAndSettle();

    expect(servicio.llamadas, contains('desbloquearFranja:f3'));
    expect(find.text('Franja desbloqueada'), findsOneWidget);
  });

  testWidgets('un error de negocio al bloquear se muestra y recarga las franjas', (tester) async {
    servicio.franjas = [franja('f1', EstadoFranja.libre)];
    await montar(tester);
    final cargasAntes = servicio.llamadas.where((l) => l == 'listarFranjas').length;
    servicio.error = ApiException(code: 'ERR_ACA_08', message: 'La franja no está libre: solo se bloquean las libres');
    servicio.errorUnaVez = true;

    await tester.tap(find.text('Bloquear'));
    await tester.pumpAndSettle();

    expect(find.text('La franja no está libre: solo se bloquean las libres'), findsOneWidget);
    expect(servicio.llamadas.where((l) => l == 'listarFranjas').length, greaterThan(cargasAntes));
  });

  group('un día completo', () {
    testWidgets('con franjas libres ofrece Bloquear día y, si se confirma, llama al servicio y dice cuántas', (
      tester,
    ) async {
      servicio.franjas = [franja('f1', EstadoFranja.libre), franja('f2', EstadoFranja.reservada, inicio: '12:00')];
      servicio.franjasDelDia = 3;
      await montar(tester);
      final clave = fechaParaApi(manana);

      await tester.tap(find.byKey(Key('bloquearDia-$clave')));
      await tester.pumpAndSettle();
      expect(find.text('Bloquear el día'), findsOneWidget);
      expect(servicio.llamadas.where((l) => l.startsWith('bloquearDia')), isEmpty);

      await tester.tap(find.widgetWithText(FilledButton, 'Bloquear'));
      await tester.pumpAndSettle();

      expect(servicio.llamadas, contains('bloquearDia:$clave'));
      expect(find.text('3 franjas bloqueadas'), findsOneWidget);
    });

    testWidgets('si no había franjas libres, informa que no hubo cambios', (tester) async {
      servicio.franjas = [franja('f1', EstadoFranja.libre)];
      servicio.franjasDelDia = 0;
      await montar(tester);

      await tester.tap(find.byKey(Key('bloquearDia-${fechaParaApi(manana)}')));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Bloquear'));
      await tester.pumpAndSettle();

      expect(find.text('No había franjas libres'), findsOneWidget);
    });

    testWidgets('con una sola franja usa el singular', (tester) async {
      servicio.franjas = [franja('f1', EstadoFranja.libre)];
      servicio.franjasDelDia = 1;
      await montar(tester);

      await tester.tap(find.byKey(Key('bloquearDia-${fechaParaApi(manana)}')));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Bloquear'));
      await tester.pumpAndSettle();

      expect(find.text('1 franja bloqueada'), findsOneWidget);
    });

    testWidgets('si se cancela la confirmación, no llama al servicio', (tester) async {
      servicio.franjas = [franja('f1', EstadoFranja.libre)];
      await montar(tester);

      await tester.tap(find.byKey(Key('bloquearDia-${fechaParaApi(manana)}')));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Volver'));
      await tester.pumpAndSettle();

      expect(servicio.llamadas.where((l) => l.startsWith('bloquearDia')), isEmpty);
    });

    testWidgets('con franjas bloqueadas ofrece Desbloquear día', (tester) async {
      servicio.franjas = [franja('f1', EstadoFranja.bloqueada), franja('f2', EstadoFranja.bloqueada, inicio: '12:00')];
      servicio.franjasDelDia = 2;
      await montar(tester);
      final clave = fechaParaApi(manana);

      expect(find.byKey(Key('bloquearDia-$clave')), findsNothing);
      await tester.tap(find.byKey(Key('desbloquearDia-$clave')));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Desbloquear'));
      await tester.pumpAndSettle();

      expect(servicio.llamadas, contains('desbloquearDia:$clave'));
      expect(find.text('2 franjas desbloqueadas'), findsOneWidget);
    });

    testWidgets('un día con libres y bloqueadas ofrece los dos botones', (tester) async {
      servicio.franjas = [franja('f1', EstadoFranja.libre), franja('f2', EstadoFranja.bloqueada, inicio: '12:00')];
      await montar(tester);
      final clave = fechaParaApi(manana);

      expect(find.byKey(Key('bloquearDia-$clave')), findsOneWidget);
      expect(find.byKey(Key('desbloquearDia-$clave')), findsOneWidget);
    });

    testWidgets('un día reservado por completo no ofrece ninguno', (tester) async {
      servicio.franjas = [franja('f1', EstadoFranja.reservada)];
      await montar(tester);
      final clave = fechaParaApi(manana);

      expect(find.byKey(Key('bloquearDia-$clave')), findsNothing);
      expect(find.byKey(Key('desbloquearDia-$clave')), findsNothing);
    });

    testWidgets('un día pasado no ofrece bloquear ni desbloquear', (tester) async {
      servicio.franjas = [franja('f1', EstadoFranja.libre, fecha: ayer)];
      await montar(tester);
      final clave = fechaParaApi(ayer);

      expect(find.byKey(Key('bloquearDia-$clave')), findsNothing);
    });

    testWidgets('un error de negocio del backend se muestra tal cual', (tester) async {
      servicio.franjas = [franja('f1', EstadoFranja.libre)];
      await montar(tester);
      servicio.error = ApiException(code: 'ERR_ACA_10', message: 'El día ya pasó');
      servicio.errorUnaVez = true;

      await tester.tap(find.byKey(Key('bloquearDia-${fechaParaApi(manana)}')));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Bloquear'));
      await tester.pumpAndSettle();

      expect(find.text('El día ya pasó'), findsOneWidget);
    });
  });

  testWidgets('un error de red al cargar muestra el aviso y Reintentar vuelve a pedir', (tester) async {
    servicio.error = StateError('sin red');
    servicio.errorUnaVez = true;
    await montar(tester);
    expect(find.text('No pudimos conectar con el servidor. Intenta de nuevo.'), findsOneWidget);

    servicio.franjas = [franja('f1', EstadoFranja.libre)];
    await tester.tap(find.text('Reintentar'));
    await tester.pumpAndSettle();

    expect(find.text('10:00 – 11:00'), findsOneWidget);
  });

  testWidgets('las flechas cambian de semana y piden las franjas de la nueva semana', (tester) async {
    await montar(tester);
    final lunes = lunesDeLaSemana(DateTime.now());

    await tester.tap(find.byTooltip('Semana siguiente'));
    await tester.pumpAndSettle();

    expect(servicio.ultimoDesde, sumarDias(lunes, 7));
    expect(servicio.ultimoHasta, sumarDias(lunes, 13));
    expect(find.text('Semana ${etiquetaDeSemana(sumarDias(lunes, 7))}'), findsOneWidget);

    await tester.tap(find.byTooltip('Semana anterior'));
    await tester.tap(find.byTooltip('Semana anterior'));
    await tester.pumpAndSettle();

    expect(servicio.ultimoDesde, sumarDias(lunes, -7));
  });
}
