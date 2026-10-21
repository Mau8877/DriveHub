import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/horarios/screens/horario_form_screen.dart';

import '../../helpers/horarios_falsos.dart';

/// El formulario de un horario nuevo: lo que tiene lógica (validación y errores del backend).
void main() {
  late HorariosServiceFalso servicio;

  setUp(() => servicio = HorariosServiceFalso());

  Future<void> montar(WidgetTester tester) async {
    final router = GoRouter(
      routes: [
        GoRoute(
          path: '/',
          builder: (_, _) => Scaffold(
            body: Builder(
              builder: (contexto) => TextButton(
                onPressed: () => contexto.push('/schedules/new'),
                child: const Text('abrir'),
              ),
            ),
          ),
        ),
        GoRoute(path: '/schedules/new', builder: (_, _) => const HorarioFormScreen()),
      ],
    );
    await tester.pumpWidget(
      ProviderScope(
        overrides: [horariosServiceFalso(servicio)],
        child: MaterialApp.router(routerConfig: router),
      ),
    );
    await tester.tap(find.text('abrir'));
    await tester.pumpAndSettle();
  }

  /// Elige una hora con el selector: toca el campo y acepta la hora que propone.
  Future<void> aceptarHora(WidgetTester tester, Key campo) async {
    await tester.ensureVisible(find.byKey(campo));
    await tester.tap(find.byKey(campo));
    await tester.pumpAndSettle();
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();
  }

  testWidgets('con los campos vacíos, Guardar muestra los mensajes y no llama al servicio', (tester) async {
    await montar(tester);

    await tester.ensureVisible(find.byKey(const Key('botonGuardarHorario')));
    await tester.tap(find.byKey(const Key('botonGuardarHorario')));
    await tester.pumpAndSettle();

    expect(find.text('La hora de inicio es obligatoria'), findsOneWidget);
    expect(find.text('La hora de fin es obligatoria'), findsOneWidget);
    expect(servicio.llamadas, isEmpty);
  });

  testWidgets('una duración fuera de rango muestra su mensaje', (tester) async {
    await montar(tester);

    await tester.enterText(find.byKey(const Key('campoDuracion')), '5');
    await tester.ensureVisible(find.byKey(const Key('botonGuardarHorario')));
    await tester.tap(find.byKey(const Key('botonGuardarHorario')));
    await tester.pumpAndSettle();

    expect(find.text('La duración debe estar entre 15 y 240 minutos'), findsOneWidget);
    expect(servicio.llamadas, isEmpty);
  });

  testWidgets('un tramo que no es múltiplo de la duración muestra su mensaje', (tester) async {
    await montar(tester);
    await aceptarHora(tester, const Key('campoHoraInicio'));
    await aceptarHora(tester, const Key('campoHoraFin'));
    await tester.enterText(find.byKey(const Key('campoDuracion')), '45');

    await tester.ensureVisible(find.byKey(const Key('botonGuardarHorario')));
    await tester.tap(find.byKey(const Key('botonGuardarHorario')));
    await tester.pumpAndSettle();

    // * 08:00 a 09:00 (la hora de fin propuesta es inicio + 60) no es múltiplo de 45
    expect(
      find.text('El tramo entre inicio y fin debe ser un múltiplo de la duración de la clase'),
      findsWidgets,
    );
    expect(servicio.llamadas, isEmpty);
  });

  testWidgets('con datos válidos llama al servicio con el pedido esperado y vuelve con el aviso', (tester) async {
    await montar(tester);
    await aceptarHora(tester, const Key('campoHoraInicio'));
    await aceptarHora(tester, const Key('campoHoraFin'));

    await tester.ensureVisible(find.byKey(const Key('botonGuardarHorario')));
    await tester.tap(find.byKey(const Key('botonGuardarHorario')));
    await tester.pumpAndSettle();

    expect(servicio.llamadas, contains('crearPlantilla'));
    final pedido = servicio.ultimoRequest!;
    expect(pedido.diaSemana, 1);
    expect(pedido.horaInicio, '08:00');
    expect(pedido.horaFin, '09:00');
    expect(pedido.duracionClaseMin, 60);
    expect(pedido.vigenteDesde, isNotNull);
    expect(pedido.vigenteHasta, isNull);
    // * Volvió a la pantalla anterior y avisó cuántas franjas se generaron
    expect(find.text('abrir'), findsOneWidget);
    expect(find.text('Horario creado: 14 franjas generadas'), findsOneWidget);
  });

  testWidgets('un error de negocio del backend se muestra en el formulario y no pierde lo escrito', (tester) async {
    servicio.error = ApiException(code: 'ERR_ACA_04', message: 'El horario se cruza con otro horario activo tuyo');
    await montar(tester);
    await aceptarHora(tester, const Key('campoHoraInicio'));
    await aceptarHora(tester, const Key('campoHoraFin'));

    await tester.ensureVisible(find.byKey(const Key('botonGuardarHorario')));
    await tester.tap(find.byKey(const Key('botonGuardarHorario')));
    await tester.pumpAndSettle();

    expect(find.text('El horario se cruza con otro horario activo tuyo'), findsOneWidget);
    expect(find.text('Nuevo horario'), findsWidgets);
    expect(find.text('08:00'), findsOneWidget);
  });

  testWidgets('un error de red muestra el aviso genérico', (tester) async {
    servicio.error = StateError('sin red');
    await montar(tester);
    await aceptarHora(tester, const Key('campoHoraInicio'));
    await aceptarHora(tester, const Key('campoHoraFin'));

    await tester.ensureVisible(find.byKey(const Key('botonGuardarHorario')));
    await tester.tap(find.byKey(const Key('botonGuardarHorario')));
    await tester.pumpAndSettle();

    expect(find.text('No pudimos conectar con el servidor. Intenta de nuevo.'), findsOneWidget);
  });
}
