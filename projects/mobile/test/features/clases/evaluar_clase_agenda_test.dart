import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/models/tipo_prueba.dart';
import 'package:mobile/features/clases/screens/agenda_instructor_screen.dart';

import '../../helpers/clases_falsos.dart';

/// El recorrido completo por la pantalla de la agenda: de la clase por evaluar a la clase con sus pruebas.
void main() {
  late ClasesServiceFalso servicio;

  setUp(() => servicio = ClasesServiceFalso());

  Future<void> montar(WidgetTester tester) async {
    tester.view.physicalSize = const Size(800, 2400);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final container = ProviderContainer(overrides: [clasesServiceFalso(servicio)]);
    addTearDown(container.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(
          theme: AppTheme.light,
          home: const Scaffold(body: AgendaInstructorScreen()),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  Future<void> puntuar(WidgetTester tester, String codigo, double valor) async {
    tester.widget<Slider>(find.byKey(Key('slider-$codigo'))).onChanged!(valor);
    await tester.pump();
  }

  testWidgets('cada clase ofrece «Evaluar» y «Ver prueba» solo cuando el servidor lo dice', (tester) async {
    servicio.clasesDeAgenda = [
      claseDeAgendaDePrueba(codigo: 'a', estado: EstadoReserva.completada, claseCodigo: 'c1', evaluable: true),
      claseDeAgendaDePrueba(codigo: 'b', estado: EstadoReserva.completada, claseCodigo: 'c2', conPrueba: true),
      claseDeAgendaDePrueba(
        codigo: 'c',
        estado: EstadoReserva.completada,
        claseCodigo: 'c3',
        evaluable: true,
        conPrueba: true,
      ),
      claseDeAgendaDePrueba(codigo: 'd', estado: EstadoReserva.completada, claseCodigo: 'c4'),
      claseDeAgendaDePrueba(codigo: 'e', estado: EstadoReserva.noAsistio, claseCodigo: 'c5'),
      claseDeAgendaDePrueba(codigo: 'f', cerrable: true),
      claseDeAgendaDePrueba(codigo: 'g', estado: EstadoReserva.cancelada),
    ];
    await montar(tester);

    expect(find.byKey(const Key('evaluar-a')), findsOneWidget);
    expect(find.byKey(const Key('ver-prueba-a')), findsNothing);
    expect(find.byKey(const Key('evaluar-b')), findsNothing);
    expect(find.byKey(const Key('ver-prueba-b')), findsOneWidget);
    expect(find.byKey(const Key('evaluar-c')), findsOneWidget);
    expect(find.byKey(const Key('ver-prueba-c')), findsOneWidget);
    for (final codigo in ['d', 'e', 'f', 'g']) {
      expect(find.byKey(Key('evaluar-$codigo')), findsNothing);
      expect(find.byKey(Key('ver-prueba-$codigo')), findsNothing);
    }
  });

  testWidgets('tocar «Evaluar» abre la hoja de evaluar de esa clase', (tester) async {
    servicio.clasesDeAgenda = [
      claseDeAgendaDePrueba(
        codigo: 'a',
        estado: EstadoReserva.completada,
        claseCodigo: 'c1',
        evaluable: true,
        estudianteNombre: 'Ana Alfa',
      ),
    ];
    await montar(tester);

    await tester.tap(find.byKey(const Key('evaluar-a')));
    await tester.pumpAndSettle();

    expect(find.text('Evaluar clase'), findsOneWidget);
    expect(find.byKey(const Key('claseAEvaluar')), findsOneWidget);
    expect(find.byKey(const Key('tipoPrueba')), findsOneWidget);
    expect(servicio.pruebasPedidas, ['c1']);
    expect(servicio.evaluacionesPedidas, isEmpty);
  });

  testWidgets('tocar «Ver prueba» abre la hoja con las pruebas de esa clase', (tester) async {
    servicio.clasesDeAgenda = [
      claseDeAgendaDePrueba(codigo: 'b', estado: EstadoReserva.completada, claseCodigo: 'c2', conPrueba: true),
    ];
    servicio.pruebasPorClase['c2'] = [pruebaClaseDePrueba(claseCodigo: 'c2', tipo: TipoPrueba.ciudad)];
    await montar(tester);

    await tester.tap(find.byKey(const Key('ver-prueba-b')));
    await tester.pumpAndSettle();

    expect(find.text('Pruebas de la clase'), findsOneWidget);
    expect(find.byKey(const Key('prueba-CIUDAD')), findsOneWidget);
    expect(servicio.pruebasPedidas, ['c2']);
  });

  testWidgets('evaluar refresca la agenda: aparece «Ver prueba» y, con los tres tipos, desaparece «Evaluar»', (
    tester,
  ) async {
    servicio.clasesDeAgenda = [
      claseDeAgendaDePrueba(codigo: 'a', estado: EstadoReserva.completada, claseCodigo: 'c1', evaluable: true),
    ];
    await montar(tester);
    expect(find.byKey(const Key('ver-prueba-a')), findsNothing);

    // * Primera prueba: manejo. El servidor ahora dice que la clase ya tiene una y todavía se puede evaluar
    await tester.tap(find.byKey(const Key('evaluar-a')));
    await tester.pumpAndSettle();
    await puntuar(tester, 'manejo_a', 8);
    await puntuar(tester, 'manejo_b', 8);
    servicio.clasesDeAgenda = [
      claseDeAgendaDePrueba(
        codigo: 'a',
        estado: EstadoReserva.completada,
        claseCodigo: 'c1',
        evaluable: true,
        conPrueba: true,
      ),
    ];
    await tester.tap(find.byKey(const Key('guardarPrueba')));
    await tester.pumpAndSettle();

    expect(find.text('Prueba registrada: 80 puntos, aprobado.'), findsOneWidget);
    expect(find.byKey(const Key('ver-prueba-a')), findsOneWidget);
    expect(find.byKey(const Key('evaluar-a')), findsOneWidget);

    // * Las otras dos: con la última, el servidor deja de ofrecer «Evaluar»
    for (final tipo in [TipoPrueba.estacionamiento, TipoPrueba.ciudad]) {
      await tester.tap(find.byKey(const Key('evaluar-a')));
      await tester.pumpAndSettle();
      await tester.tap(find.descendant(of: find.byKey(const Key('tipoPrueba')), matching: find.text(tipo.etiqueta)));
      await tester.pumpAndSettle();
      await puntuar(tester, '${tipo.name}_a', 9);
      await puntuar(tester, '${tipo.name}_b', 9);
      if (tipo == TipoPrueba.ciudad) {
        servicio.clasesDeAgenda = [
          claseDeAgendaDePrueba(codigo: 'a', estado: EstadoReserva.completada, claseCodigo: 'c1', conPrueba: true),
        ];
      }
      await tester.tap(find.byKey(const Key('guardarPrueba')));
      await tester.pumpAndSettle();
    }

    expect(servicio.evaluacionesPedidas.map((e) => e.tipo), TipoPrueba.values);
    expect(find.byKey(const Key('evaluar-a')), findsNothing);
    expect(find.byKey(const Key('ver-prueba-a')), findsOneWidget);
  });

  testWidgets('si el servidor dice que ya hay una prueba de ese tipo, la agenda se refresca y la hoja se cierra', (
    tester,
  ) async {
    servicio.clasesDeAgenda = [
      claseDeAgendaDePrueba(codigo: 'a', estado: EstadoReserva.completada, claseCodigo: 'c1', evaluable: true),
    ];
    await montar(tester);
    await tester.tap(find.byKey(const Key('evaluar-a')));
    await tester.pumpAndSettle();
    await puntuar(tester, 'manejo_a', 8);
    await puntuar(tester, 'manejo_b', 8);
    servicio.errorAlEvaluarClase = ApiException(
      code: 'ERR_ACA_40',
      message: 'Esa clase ya tiene una prueba de ese tipo',
    );
    servicio.clasesDeAgenda = [
      claseDeAgendaDePrueba(codigo: 'a', estado: EstadoReserva.completada, claseCodigo: 'c1', conPrueba: true),
    ];

    await tester.tap(find.byKey(const Key('guardarPrueba')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('guardarPrueba')), findsNothing);
    expect(find.text('Esa clase ya tiene una prueba de ese tipo'), findsOneWidget);
    expect(find.byKey(const Key('evaluar-a')), findsNothing);
    expect(find.byKey(const Key('ver-prueba-a')), findsOneWidget);
  });
}
