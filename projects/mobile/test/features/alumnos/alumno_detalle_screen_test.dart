import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/alumnos/models/consumo_clase.dart';
import 'package:mobile/features/alumnos/screens/alumno_detalle_screen.dart';
import 'package:mobile/features/pagos/models/estado_pago_visible.dart';

import '../../helpers/alumnos_falsos.dart';

void main() {
  late HorasAlumnosServiceFalso servicio;

  setUp(() => servicio = HorasAlumnosServiceFalso());

  Future<void> montar(WidgetTester tester) async {
    final router = GoRouter(
      initialLocation: '/lista',
      routes: [
        GoRoute(
          path: '/lista',
          builder: (context, _) => Scaffold(
            body: TextButton(onPressed: () => context.push('/students/a1'), child: const Text('ABRIR')),
          ),
        ),
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
    await tester.tap(find.text('ABRIR'));
    await tester.pumpAndSettle();
  }

  testWidgets('muestra el resumen, el tiempo con el instructor y los pagos del alumno', (tester) async {
    servicio.detalles = {
      'a1': detalleDeAlumno(
        consumos: [
          ConsumoClase(claseCodigo: 'cl2', fecha: DateTime(2026, 5, 10, 9), horas: 3),
          ConsumoClase(claseCodigo: 'cl1', fecha: DateTime(2026, 5, 4, 10), horas: 2),
        ],
      ),
    };

    await montar(tester);

    expect(find.text('Ana Pérez'), findsOneWidget);
    expect(find.text('20 h'), findsOneWidget);
    expect(find.text('6 h'), findsOneWidget);
    expect(find.text('14 h'), findsOneWidget);
    expect(find.text('Tiempo conmigo'), findsOneWidget);
    expect(find.text('10 may 2026'), findsOneWidget);
    expect(find.text('3 h'), findsOneWidget);
    expect(find.text('04 may 2026'), findsOneWidget);
    expect(find.text('2 h'), findsOneWidget);
    expect(find.text('Pagos'), findsOneWidget);
    expect(find.text('Plan 10 horas'), findsOneWidget);
    expect(find.text('Confirmado'), findsOneWidget);
    expect(find.text('10 horas · 01 abr 2026'), findsOneWidget);
  });

  testWidgets('el tiempo conmigo se ve de la clase más reciente a la más antigua, como lo entrega el backend', (tester) async {
    servicio.detalles = {
      'a1': detalleDeAlumno(
        consumos: [
          ConsumoClase(claseCodigo: 'reciente', fecha: DateTime(2026, 5, 10), horas: 1),
          ConsumoClase(claseCodigo: 'antigua', fecha: DateTime(2026, 5, 1), horas: 1),
        ],
      ),
    };

    await montar(tester);

    final reciente = tester.getTopLeft(find.byKey(const Key('clase-reciente'))).dy;
    final antigua = tester.getTopLeft(find.byKey(const Key('clase-antigua'))).dy;
    expect(reciente, lessThan(antigua));
  });

  testWidgets('un alumno sin clases muestra el aviso en vez de la lista', (tester) async {
    servicio.detalles = {'a1': detalleDeAlumno(consumos: [])};

    await montar(tester);

    expect(find.byKey(const Key('sinClasesConmigo')), findsOneWidget);
    expect(find.text('Todavía no tienes clases con este alumno.'), findsOneWidget);
  });

  testWidgets('es de solo lectura: sin comprobante ni acciones sobre el pago', (tester) async {
    servicio.detalles = {'a1': detalleDeAlumno()};

    await montar(tester);

    expect(find.textContaining('omprobante'), findsNothing);
    expect(find.textContaining('eembolso'), findsNothing);
    expect(find.byType(FilledButton), findsNothing);
    expect(find.byType(OutlinedButton), findsNothing);
  });

  testWidgets('el estado de cada pago va en español', (tester) async {
    servicio.detalles = {
      'a1': detalleDeAlumno(
        pagos: [
          detalleDeAlumno().pagos.first,
        ],
      ),
    };
    await montar(tester);
    expect(find.text(EstadoPagoVisible.confirmado.etiqueta), findsOneWidget);
  });

  testWidgets('un 503 muestra «No pudimos cargar los datos» y permite reintentar', (tester) async {
    servicio.errorDetalle = ApiException(code: 'ERR_PAG_15', message: 'caído');
    await montar(tester);
    expect(find.text('No pudimos cargar los datos, inténtalo más tarde.'), findsOneWidget);

    servicio.errorDetalle = null;
    await tester.tap(find.byKey(const Key('botonReintentarAlumno')));
    await tester.pumpAndSettle();

    expect(find.text('Tiempo conmigo'), findsOneWidget);
  });

  testWidgets('sin conexión avisa que no hay internet', (tester) async {
    servicio.errorDetalle = DioException(requestOptions: RequestOptions(path: '/horas-alumnos/a1'));

    await montar(tester);

    expect(find.textContaining('No hay conexión'), findsOneWidget);
  });

  testWidgets('un alumno ajeno (ERR_PAG_12) vuelve a la lista con un aviso', (tester) async {
    servicio.errorDetalle = ApiException(code: 'ERR_PAG_12', message: 'no es tuyo');

    await montar(tester);

    expect(find.text('ABRIR'), findsOneWidget);
    expect(find.text('Ese alumno no está entre tus alumnos.'), findsOneWidget);
  });
}
