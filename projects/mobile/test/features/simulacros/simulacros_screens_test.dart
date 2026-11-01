import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/simulacros/screens/rendir_simulacro_screen.dart';
import 'package:mobile/features/simulacros/screens/resultado_simulacro_screen.dart';
import 'package:mobile/features/simulacros/screens/simulacros_screen.dart';

import '../../helpers/simulacros_falsos.dart';

void main() {
  late SimulacrosServiceFalso servicio;

  setUp(() => servicio = SimulacrosServiceFalso());

  Future<ProviderContainer> montar(
    WidgetTester tester, {
    String inicial = '/study',
  }) async {
    final container = ProviderContainer(
      overrides: [simulacrosServiceFalso(servicio)],
    );
    addTearDown(container.dispose);
    final router = GoRouter(
      initialLocation: inicial,
      routes: [
        GoRoute(
          path: '/study',
          builder: (_, _) => const Scaffold(body: SimulacrosScreen()),
        ),
        GoRoute(
          path: '/study/simulacros/rendir',
          builder: (_, _) => const RendirSimulacroScreen(),
        ),
        GoRoute(
          path: '/study/attempts/:codigo',
          builder: (_, state) =>
              ResultadoSimulacroScreen(codigo: state.pathParameters['codigo']!),
        ),
      ],
    );
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp.router(theme: AppTheme.light, routerConfig: router),
      ),
    );
    await tester.pumpAndSettle();
    return container;
  }

  group('SimulacrosScreen', () {
    testWidgets('lista los simulacros y el historial', (tester) async {
      servicio.simulacros = [simulacroDePrueba()];
      servicio.historialDePrueba = [resumenDePrueba()];

      await montar(tester);

      expect(find.text('Simulacro básico'), findsWidgets);
      expect(find.byKey(const Key('simulacro-s1')), findsOneWidget);
      expect(find.byKey(const Key('intento-i1')), findsOneWidget);
      expect(find.text('Aprobado'), findsOneWidget);
    });

    testWidgets('sin simulacros ni intentos muestra los estados vacíos', (
      tester,
    ) async {
      await montar(tester);

      expect(find.text('No hay simulacros disponibles'), findsOneWidget);
      expect(find.text('Todavía no rindes ningún simulacro'), findsOneWidget);
    });

    testWidgets(
      'Rendir pide confirmar, inicia el intento y abre la primera pregunta',
      (tester) async {
        servicio.simulacros = [simulacroDePrueba()];

        await montar(tester);
        await tester.tap(find.text('Rendir'));
        await tester.pumpAndSettle();
        expect(
          find.textContaining('2 preguntas en 20 minutos'),
          findsOneWidget,
        );
        await tester.tap(find.byKey(const Key('botonComenzarSimulacro')));
        await tester.pumpAndSettle();

        expect(servicio.iniciados, ['s1']);
        expect(find.text('Pregunta 1 de 2'), findsOneWidget);
        expect(find.text('¿Qué indica el PARE?'), findsOneWidget);
        expect(find.byKey(const Key('cronometro')), findsOneWidget);
      },
    );

    testWidgets(
      'un error al iniciar se avisa con un mensaje claro y no abre el intento',
      (tester) async {
        servicio.simulacros = [simulacroDePrueba()];
        servicio.errorAlIniciar = ApiException(
          code: 'ERR_ACA_28',
          message: 'x',
        );

        await montar(tester);
        await tester.tap(find.text('Rendir'));
        await tester.pumpAndSettle();
        await tester.tap(find.byKey(const Key('botonComenzarSimulacro')));
        await tester.pumpAndSettle();

        expect(
          find.textContaining('todavía no tiene preguntas suficientes'),
          findsOneWidget,
        );
        expect(find.text('Pregunta 1 de 2'), findsNothing);
      },
    );
  });

  group('RendirSimulacroScreen', () {
    Future<void> comenzar(WidgetTester tester) async {
      servicio.simulacros = [simulacroDePrueba()];
      await montar(tester);
      await tester.tap(find.text('Rendir'));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('botonComenzarSimulacro')));
      await tester.pumpAndSettle();
    }

    testWidgets(
      'marca opciones, avanza y finaliza enviando lo marcado y abre la corrección',
      (tester) async {
        await comenzar(tester);

        await tester.tap(find.byKey(const Key('opcion-A')));
        await tester.pump();
        await tester.tap(find.byKey(const Key('botonSiguiente')));
        await tester.pumpAndSettle();
        expect(find.text('Pregunta 2 de 2'), findsOneWidget);
        expect(find.text('Marca todas las correctas.'), findsOneWidget);
        await tester.tap(find.byKey(const Key('opcion-A')));
        await tester.tap(find.byKey(const Key('opcion-B')));
        await tester.pump();
        await tester.tap(find.byKey(const Key('botonFinalizar')));
        await tester.pumpAndSettle();
        await tester.tap(find.byKey(const Key('botonConfirmarEnvio')));
        await tester.pumpAndSettle();

        expect(servicio.enviados.single, {
          'p1': {'A'},
          'p2': {'A', 'B'},
        });
        expect(find.byKey(const Key('puntajeFinal')), findsOneWidget);
        expect(find.text('50%'), findsOneWidget);
      },
    );

    testWidgets('Finalizar avisa cuántas preguntas quedan sin responder', (
      tester,
    ) async {
      await comenzar(tester);

      await tester.tap(find.byKey(const Key('botonSiguiente')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('botonFinalizar')));
      await tester.pumpAndSettle();

      expect(find.textContaining('2 preguntas sin responder'), findsOneWidget);
    });

    testWidgets(
      'Anterior vuelve a la pregunta anterior y está desactivado en la primera',
      (tester) async {
        await comenzar(tester);

        expect(
          tester
              .widget<OutlinedButton>(find.byKey(const Key('botonAnterior')))
              .onPressed,
          isNull,
        );
        await tester.tap(find.byKey(const Key('botonSiguiente')));
        await tester.pumpAndSettle();
        await tester.tap(find.byKey(const Key('botonAnterior')));
        await tester.pumpAndSettle();

        expect(find.text('Pregunta 1 de 2'), findsOneWidget);
      },
    );

    testWidgets('un error al enviar deja el intento en pantalla y avisa', (
      tester,
    ) async {
      await comenzar(tester);
      servicio.errorAlFinalizar = ApiException(
        code: 'ERR_ACA_31',
        message: 'x',
      );

      await tester.tap(find.byKey(const Key('botonSiguiente')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('botonFinalizar')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('botonConfirmarEnvio')));
      await tester.pumpAndSettle();

      expect(find.text('Este intento ya terminó.'), findsOneWidget);
      expect(find.text('Pregunta 2 de 2'), findsOneWidget);
    });

    testWidgets('cuando el tiempo llega a cero se envía solo', (tester) async {
      servicio.intento = intentoDePrueba(duracionMin: 0);
      await comenzar(tester);
      await tester.pump(const Duration(seconds: 2));
      await tester.pumpAndSettle();

      expect(servicio.enviados, hasLength(1));
      expect(find.byKey(const Key('puntajeFinal')), findsOneWidget);
    });
  });

  group('ResultadoSimulacroScreen', () {
    testWidgets(
      'muestra el puntaje, si aprobó y la corrección con la respuesta correcta y la explicación',
      (tester) async {
        servicio.resultado = resultadoDePrueba(aprobado: true);

        await montar(tester, inicial: '/study/attempts/i1');

        expect(find.text('100%'), findsOneWidget);
        expect(find.text('Aprobado'), findsOneWidget);
        expect(find.text('2 de 2 correctas'), findsOneWidget);
        expect(find.text('Obliga a detenerse.'), findsOneWidget);
        expect(
          find.textContaining('A. Detenerse (tu respuesta)'),
          findsOneWidget,
        );
        expect(
          find.textContaining('B. Velocidad máxima (correcta)'),
          findsOneWidget,
        );
      },
    );

    testWidgets(
      'un intento sin tiempo se marca como tal y uno no aprobado dice cuánto necesitaba',
      (tester) async {
        servicio.resultado = resultadoDePrueba(estado: 'EXPIRADO');
        await montar(tester, inicial: '/study/attempts/i1');
        expect(find.text('Se acabó el tiempo'), findsOneWidget);
      },
    );

    testWidgets('no aprobado muestra el mínimo necesario', (tester) async {
      await montar(tester, inicial: '/study/attempts/i1');
      expect(find.textContaining('necesitas 70%'), findsOneWidget);
    });
  });
}
