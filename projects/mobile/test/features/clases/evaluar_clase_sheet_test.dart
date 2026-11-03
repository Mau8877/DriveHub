import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/models/tipo_prueba.dart';
import 'package:mobile/features/clases/widgets/evaluar_clase_sheet.dart';

import '../../helpers/clases_falsos.dart';

void main() {
  late ClasesServiceFalso servicio;

  final porEvaluar = claseDeAgendaDePrueba(
    codigo: 'r1',
    estado: EstadoReserva.completada,
    claseCodigo: 'c1',
    evaluable: true,
    estudianteNombre: 'Eva Estudiante',
  );

  setUp(() => servicio = ClasesServiceFalso());

  /// Una pantalla mínima con un botón que abre la hoja de evaluar.
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
          home: Scaffold(
            body: Builder(
              builder: (context) => TextButton(
                key: const Key('abrirEvaluar'),
                onPressed: () => mostrarEvaluarClase(context, porEvaluar),
                child: const Text('abrir evaluar'),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Future<void> abrir(WidgetTester tester) async {
    await montar(tester);
    await tester.tap(find.byKey(const Key('abrirEvaluar')));
    await tester.pumpAndSettle();
  }

  /// Mueve el control de un criterio al valor dado, como si la persona lo arrastrara.
  Future<void> puntuar(WidgetTester tester, String codigo, double valor) async {
    tester.widget<Slider>(find.byKey(Key('slider-$codigo'))).onChanged!(valor);
    await tester.pump();
  }

  Future<void> puntuarTodos(WidgetTester tester, TipoPrueba tipo, double valor) async {
    await puntuar(tester, '${tipo.name}_a', valor);
    await puntuar(tester, '${tipo.name}_b', valor);
  }

  Future<void> guardar(WidgetTester tester) async {
    await tester.tap(find.byKey(const Key('guardarPrueba')));
    await tester.pumpAndSettle();
  }

  bool guardarHabilitado(WidgetTester tester) =>
      tester.widget<FilledButton>(find.byKey(const Key('guardarPrueba'))).onPressed != null;

  bool hojaAbierta() => find.byKey(const Key('guardarPrueba')).evaluate().isNotEmpty;

  String textoDe(WidgetTester tester, String clave) => tester.widget<Text>(find.byKey(Key(clave))).data!;

  group('la hoja', () {
    testWidgets('muestra la clase, el primer tipo con sus criterios sin puntuar y «Guardar prueba» apagado', (
      tester,
    ) async {
      await abrir(tester);

      expect(find.text('Evaluar clase'), findsOneWidget);
      expect(find.byKey(const Key('claseAEvaluar')), findsOneWidget);
      expect(find.text('Eva Estudiante'), findsOneWidget);
      expect(find.byKey(const Key('criterio-manejo_a')), findsOneWidget);
      expect(find.byKey(const Key('criterio-manejo_b')), findsOneWidget);
      expect(find.byKey(const Key('criterio-ciudad_a')), findsNothing);
      expect(textoDe(tester, 'valor-manejo_a'), '—');
      expect(textoDe(tester, 'totalPrueba'), 'Total: — / 100');
      expect(find.byKey(const Key('resultadoPrueba')), findsNothing);
      expect(guardarHabilitado(tester), isFalse);
      expect(servicio.evaluacionesPedidas, isEmpty);
    });

    testWidgets('«Guardar prueba» se enciende solo cuando todos los criterios tienen puntaje', (tester) async {
      await abrir(tester);

      await puntuar(tester, 'manejo_a', 8);
      expect(guardarHabilitado(tester), isFalse);
      expect(textoDe(tester, 'valor-manejo_a'), '8');
      expect(textoDe(tester, 'valor-manejo_b'), '—');

      await puntuar(tester, 'manejo_b', 8);
      expect(guardarHabilitado(tester), isTrue);
    });

    testWidgets('el total y el resultado se actualizan mientras se puntúa', (tester) async {
      await abrir(tester);

      await puntuarTodos(tester, TipoPrueba.manejo, 8);
      expect(textoDe(tester, 'totalPrueba'), 'Total: 80 / 100');
      expect(
        find.descendant(of: find.byKey(const Key('resultadoPrueba')), matching: find.text('Aprobado')),
        findsOneWidget,
      );

      await puntuarTodos(tester, TipoPrueba.manejo, 6);
      expect(textoDe(tester, 'totalPrueba'), 'Total: 60 / 100');
      expect(
        find.descendant(of: find.byKey(const Key('resultadoPrueba')), matching: find.text('Reprobado')),
        findsOneWidget,
      );

      await puntuar(tester, 'manejo_b', 7.5);
      expect(textoDe(tester, 'valor-manejo_b'), '7,5');
      expect(textoDe(tester, 'totalPrueba'), 'Total: 67,5 / 100');
    });

    testWidgets('cambiar de tipo muestra otros criterios y reinicia los puntajes', (tester) async {
      await abrir(tester);
      await puntuarTodos(tester, TipoPrueba.manejo, 9);

      await tester.tap(find.descendant(of: find.byKey(const Key('tipoPrueba')), matching: find.text('Ciudad')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('criterio-ciudad_a')), findsOneWidget);
      expect(find.byKey(const Key('criterio-manejo_a')), findsNothing);
      expect(textoDe(tester, 'valor-ciudad_a'), '—');
      expect(guardarHabilitado(tester), isFalse);
    });

    testWidgets('un tipo que la clase ya tiene queda deshabilitado y se empieza por el siguiente', (tester) async {
      servicio.pruebasPorClase['c1'] = [pruebaClaseDePrueba(tipo: TipoPrueba.manejo)];
      await abrir(tester);

      final segmentos = tester.widget<SegmentedButton<TipoPrueba>>(find.byKey(const Key('tipoPrueba'))).segments;
      expect(segmentos.firstWhere((s) => s.value == TipoPrueba.manejo).enabled, isFalse);
      expect(segmentos.firstWhere((s) => s.value == TipoPrueba.estacionamiento).enabled, isTrue);
      expect(find.text('Hecha'), findsOneWidget);
      expect(find.byKey(const Key('criterio-estacionamiento_a')), findsOneWidget);
      expect(find.byKey(const Key('criterio-manejo_a')), findsNothing);
    });

    testWidgets('con las tres pruebas hechas avisa y solo deja volver', (tester) async {
      servicio.pruebasPorClase['c1'] = [
        for (final tipo in TipoPrueba.values) pruebaClaseDePrueba(codigo: tipo.name, tipo: tipo),
      ];
      await abrir(tester);

      expect(find.byKey(const Key('todasLasPruebas')), findsOneWidget);
      expect(find.text('Esta clase ya tiene las tres pruebas.'), findsOneWidget);
      expect(find.byKey(const Key('guardarPrueba')), findsNothing);

      await tester.tap(find.byKey(const Key('volverDePrueba')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('volverDePrueba')), findsNothing);
    });

    testWidgets('«Volver» cierra la hoja sin pedir nada', (tester) async {
      await abrir(tester);

      await tester.tap(find.byKey(const Key('volverDePrueba')));
      await tester.pumpAndSettle();

      expect(hojaAbierta(), isFalse);
      expect(servicio.evaluacionesPedidas, isEmpty);
    });

    testWidgets('no se cierra tocando afuera', (tester) async {
      await abrir(tester);

      await tester.tapAt(const Offset(5, 5));
      await tester.pumpAndSettle();

      expect(hojaAbierta(), isTrue);
    });

    testWidgets('si no se pueden traer los criterios muestra el error con «Reintentar» y «Volver»', (tester) async {
      servicio.errorCatalogo = DioException(requestOptions: RequestOptions(path: '/x'));
      await abrir(tester);

      expect(find.byKey(const Key('errorCargarPrueba')), findsOneWidget);
      expect(find.byKey(const Key('guardarPrueba')), findsNothing);
      expect(find.byKey(const Key('volverDePrueba')), findsOneWidget);

      servicio.errorCatalogo = null;
      await tester.tap(find.text('Reintentar'));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('errorCargarPrueba')), findsNothing);
      expect(find.byKey(const Key('criterio-manejo_a')), findsOneWidget);
    });
  });

  group('guardar', () {
    testWidgets('manda el tipo, los criterios en orden y las observaciones; cierra y avisa el resultado', (
      tester,
    ) async {
      await abrir(tester);
      await puntuar(tester, 'manejo_b', 7.5);
      await puntuar(tester, 'manejo_a', 8);
      await tester.enterText(find.byKey(const Key('observacionesPrueba')), '  Muy buena clase  ');
      await tester.pump();

      await guardar(tester);

      expect(servicio.evaluacionesPedidas.single.toJson(), {
        'claseCodigo': 'c1',
        'tipo': 'MANEJO',
        'criterios': [
          {'codigo': 'manejo_a', 'puntaje': 8.0},
          {'codigo': 'manejo_b', 'puntaje': 7.5},
        ],
        'observaciones': 'Muy buena clase',
      });
      expect(hojaAbierta(), isFalse);
      expect(find.text('Prueba registrada: 80 puntos, aprobado.'), findsOneWidget);
    });

    testWidgets('sin observaciones no las manda y una prueba reprobada se avisa como reprobada', (tester) async {
      await abrir(tester);
      await puntuarTodos(tester, TipoPrueba.manejo, 5);

      await guardar(tester);

      expect(servicio.evaluacionesPedidas.single.toJson().containsKey('observaciones'), isFalse);
      expect(find.text('Prueba registrada: 50 puntos, reprobado.'), findsOneWidget);
    });

    testWidgets('observaciones de más de 500 caracteres: avisa y no envía', (tester) async {
      await abrir(tester);
      await puntuarTodos(tester, TipoPrueba.manejo, 8);
      await tester.enterText(find.byKey(const Key('observacionesPrueba')), 'a' * 501);
      await tester.pump();

      await guardar(tester);

      expect(find.text('Las observaciones admiten hasta 500 caracteres'), findsOneWidget);
      expect(servicio.evaluacionesPedidas, isEmpty);
      expect(hojaAbierta(), isTrue);
    });

    testWidgets('mientras envía los botones están deshabilitados y un segundo toque no manda otra petición', (
      tester,
    ) async {
      final espera = Completer<void>();
      servicio.antesDeEvaluarClase = () => espera.future;
      await abrir(tester);
      await puntuarTodos(tester, TipoPrueba.manejo, 8);

      await tester.tap(find.byKey(const Key('guardarPrueba')));
      await tester.pump();

      expect(guardarHabilitado(tester), isFalse);
      expect(tester.widget<OutlinedButton>(find.byKey(const Key('volverDePrueba'))).onPressed, isNull);
      expect(tester.widget<Slider>(find.byKey(const Key('slider-manejo_a'))).onChanged, isNull);

      espera.complete();
      await tester.pumpAndSettle();

      expect(servicio.evaluacionesPedidas, hasLength(1));
      expect(hojaAbierta(), isFalse);
    });
  });

  group('errores', () {
    for (final codigo in ['ERR_ACA_35', 'ERR_ACA_39', 'ERR_ACA_40']) {
      testWidgets('$codigo cierra la hoja y avisa con el mensaje del servidor', (tester) async {
        servicio.errorAlEvaluarClase = ApiException(code: codigo, message: 'Mensaje del servidor para $codigo');
        await abrir(tester);
        await puntuarTodos(tester, TipoPrueba.manejo, 8);

        await guardar(tester);

        expect(hojaAbierta(), isFalse);
        expect(find.text('Mensaje del servidor para $codigo'), findsOneWidget);
      });
    }

    testWidgets('ERR_ACA_41 se muestra dentro de la hoja, que sigue abierta con lo puntuado', (tester) async {
      servicio.errorAlEvaluarClase = ApiException(code: 'ERR_ACA_41', message: 'Falta puntuar: Criterio B');
      await abrir(tester);
      await puntuarTodos(tester, TipoPrueba.manejo, 8);
      await tester.enterText(find.byKey(const Key('observacionesPrueba')), 'Algo');
      await tester.pump();

      await guardar(tester);

      expect(hojaAbierta(), isTrue);
      expect(find.byKey(const Key('errorPrueba')), findsOneWidget);
      expect(find.text('Falta puntuar: Criterio B'), findsOneWidget);
      expect(textoDe(tester, 'valor-manejo_a'), '8');
      expect(tester.widget<TextFormField>(find.byKey(const Key('observacionesPrueba'))).controller?.text, 'Algo');
      expect(guardarHabilitado(tester), isTrue);
    });

    testWidgets('sin conexión la hoja queda abierta y el botón pasa a «Reintentar»', (tester) async {
      servicio.errorAlEvaluarClase = DioException(requestOptions: RequestOptions(path: '/x'));
      await abrir(tester);
      await puntuarTodos(tester, TipoPrueba.manejo, 8);

      await guardar(tester);

      expect(hojaAbierta(), isTrue);
      expect(find.byKey(const Key('errorPrueba')), findsOneWidget);
      expect(find.text('Reintentar'), findsOneWidget);

      servicio.errorAlEvaluarClase = null;
      await guardar(tester);

      expect(hojaAbierta(), isFalse);
      expect(servicio.evaluacionesPedidas, hasLength(2));
      expect(find.text('Prueba registrada: 80 puntos, aprobado.'), findsOneWidget);
    });
  });
}
