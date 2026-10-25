import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/alumnos/screens/alumno_detalle_screen.dart';
import 'package:mobile/features/alumnos/screens/mis_alumnos_screen.dart';

import '../../helpers/alumnos_falsos.dart';

void main() {
  late HorasAlumnosServiceFalso servicio;

  setUp(() => servicio = HorasAlumnosServiceFalso());

  Future<void> montar(WidgetTester tester) async {
    final router = GoRouter(
      routes: [
        GoRoute(path: '/', builder: (_, _) => const Scaffold(body: MisAlumnosScreen())),
        GoRoute(path: '/students/:codigo', builder: (_, s) => AlumnoDetalleScreen(codigo: s.pathParameters['codigo']!)),
      ],
    );
    await tester.pumpWidget(
      ProviderScope(
        overrides: [horasAlumnosServiceFalso(servicio)],
        child: MaterialApp.router(theme: AppTheme.light, routerConfig: router),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('muestra a cada alumno con lo comprado, lo gastado con el instructor y lo que le queda', (tester) async {
    servicio.alumnos = [
      alumnoDePrueba(codigo: 'a1', nombres: 'Ana', apellidos: 'Pérez', comprado: 20, conmigo: 6, restantes: 14),
      alumnoDePrueba(codigo: 'a2', nombres: 'Beto', apellidos: 'Rojas', comprado: 5, conmigo: 0, restantes: 5),
    ];

    await montar(tester);

    expect(find.text('Ana Pérez'), findsOneWidget);
    expect(find.text('Beto Rojas'), findsOneWidget);
    expect(find.text('20 h'), findsOneWidget);
    expect(find.text('6 h'), findsOneWidget);
    expect(find.text('14 h'), findsOneWidget);
    expect(find.text('Comprado'), findsNWidgets(2));
    expect(find.text('Conmigo'), findsNWidgets(2));
    expect(find.text('Restantes'), findsNWidgets(2));
  });

  testWidgets('la búsqueda se aplica tras la pausa y pide solo los que coinciden', (tester) async {
    servicio.alumnos = [
      alumnoDePrueba(codigo: 'a1', nombres: 'Ana', apellidos: 'Pérez'),
      alumnoDePrueba(codigo: 'a2', nombres: 'Beto', apellidos: 'Rojas'),
    ];
    await montar(tester);

    await tester.enterText(find.byKey(const Key('campoBuscarAlumno')), 'beto');
    await tester.pump(const Duration(milliseconds: 100));
    expect(servicio.listados.last.search, '');
    await tester.pump(const Duration(milliseconds: 500));
    await tester.pumpAndSettle();

    expect(servicio.listados.last.search, 'beto');
    expect(find.text('Beto Rojas'), findsOneWidget);
    expect(find.text('Ana Pérez'), findsNothing);
  });

  testWidgets('sin alumnos asignados muestra el estado vacío', (tester) async {
    await montar(tester);

    expect(find.text('Todavía no tienes alumnos asignados'), findsOneWidget);
  });

  testWidgets('una búsqueda sin resultados dice «Sin resultados»', (tester) async {
    servicio.alumnos = [alumnoDePrueba()];
    await montar(tester);

    await tester.enterText(find.byKey(const Key('campoBuscarAlumno')), 'zzz');
    await tester.pump(const Duration(milliseconds: 500));
    await tester.pumpAndSettle();

    expect(find.text('Sin resultados'), findsOneWidget);
    expect(find.text('Todavía no tienes alumnos asignados'), findsNothing);
  });

  testWidgets('borrar la búsqueda con la X vuelve a mostrar a todos', (tester) async {
    servicio.alumnos = [
      alumnoDePrueba(codigo: 'a1', nombres: 'Ana', apellidos: 'Pérez'),
      alumnoDePrueba(codigo: 'a2', nombres: 'Beto', apellidos: 'Rojas'),
    ];
    await montar(tester);
    await tester.enterText(find.byKey(const Key('campoBuscarAlumno')), 'beto');
    await tester.pump(const Duration(milliseconds: 500));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('botonLimpiarBusqueda')));
    await tester.pumpAndSettle();

    expect(find.text('Ana Pérez'), findsOneWidget);
    expect(find.text('Beto Rojas'), findsOneWidget);
  });

  testWidgets('«Cargar más» agrega la siguiente página y desaparece en la última', (tester) async {
    servicio.alumnos = [for (var i = 0; i < 25; i++) alumnoDePrueba(codigo: 'a$i', nombres: 'Alumno$i', apellidos: 'X')];
    await montar(tester);
    final cargarMas = find.byKey(const Key('botonCargarMasAlumnos'));
    await tester.scrollUntilVisible(cargarMas, 300, scrollable: find.byType(Scrollable).first);

    await tester.tap(cargarMas);
    await tester.pumpAndSettle();

    expect(servicio.listados.last.page, 2);
    await tester.scrollUntilVisible(find.text('Alumno24 X'), 300, scrollable: find.byType(Scrollable).first);
    expect(find.text('Alumno24 X'), findsOneWidget);
    expect(cargarMas, findsNothing);
  });

  testWidgets('un error al cargar muestra el mensaje y permite reintentar', (tester) async {
    servicio.errorListado = Exception('sin red');
    await montar(tester);
    expect(find.textContaining('No pudimos cargar tus alumnos'), findsOneWidget);

    servicio.errorListado = null;
    servicio.alumnos = [alumnoDePrueba()];
    await tester.tap(find.byKey(const Key('botonReintentarAlumnos')));
    await tester.pumpAndSettle();

    expect(find.text('Ana Pérez'), findsOneWidget);
  });

  testWidgets('tocar un alumno abre su detalle', (tester) async {
    servicio.alumnos = [alumnoDePrueba(codigo: 'a1')];
    await montar(tester);

    await tester.tap(find.byKey(const Key('alumno-a1')));
    await tester.pumpAndSettle();

    expect(servicio.pedidosDeDetalle, ['a1']);
    expect(find.text('Tiempo conmigo'), findsOneWidget);
  });
}
