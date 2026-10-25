import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/alumnos/models/pago_alumno.dart';
import 'package:mobile/features/alumnos/screens/alumno_detalle_screen.dart';
import 'package:mobile/features/pagos/models/estado_pago_visible.dart';

import '../../helpers/alumnos_falsos.dart';
import '../../helpers/comprobantes_falsos.dart';

PagoAlumno _pago(String codigo, {String? comprobante}) => PagoAlumno(
  codigo: codigo,
  planNombre: 'Plan 10 horas',
  horas: 10,
  estado: EstadoPagoVisible.confirmado,
  creadoEn: DateTime(2026, 4, 1, 9),
  comprobanteCodigo: comprobante,
);

void main() {
  late HorasAlumnosServiceFalso servicio;
  late ComprobantesServiceFalso comprobantes;
  late AbridorFalso abridor;

  setUp(() {
    servicio = HorasAlumnosServiceFalso();
    comprobantes = ComprobantesServiceFalso();
    abridor = AbridorFalso();
  });

  Future<void> montar(WidgetTester tester) async {
    tester.view.physicalSize = const Size(800, 2400);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
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
        overrides: [horasAlumnosServiceFalso(servicio), ...comprobantesFalsos(comprobantes, abridor)],
        child: MaterialApp.router(theme: AppTheme.light, routerConfig: router),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('ABRIR'));
    await tester.pumpAndSettle();
  }

  testWidgets('un pago con comprobante muestra su botón; uno sin comprobante, nada', (tester) async {
    servicio.detalles = {
      'a1': detalleDeAlumno(pagos: [_pago('p1', comprobante: 'k1'), _pago('p2')]),
    };

    await montar(tester);

    expect(find.byKey(const Key('botonComprobante-k1')), findsOneWidget);
    expect(find.byType(OutlinedButton), findsOneWidget);
    expect(find.byKey(const Key('pago-p2')), findsOneWidget);
  });

  testWidgets('el botón descarga y abre el comprobante del pago del alumno', (tester) async {
    servicio.detalles = {
      'a1': detalleDeAlumno(pagos: [_pago('p1', comprobante: 'k1')]),
    };
    await montar(tester);

    await tester.tap(find.byKey(const Key('botonComprobante-k1')));
    await tester.pumpAndSettle();

    expect(comprobantes.descargas, ['k1']);
    expect(abridor.abiertos.single, endsWith('comprobante-k1.pdf'));
  });

  group('fromJson', () {
    Map<String, dynamic> base() => {
      'codigo': 'p1',
      'planNombre': 'Plan 10 horas',
      'horas': 10,
      'estado': 'CONFIRMADO',
      'creadoEn': '2026-04-01T13:00:00Z',
    };

    test('lee el código del comprobante cuando viene', () {
      expect(PagoAlumno.fromJson({...base(), 'comprobanteCodigo': 'k1'}).comprobanteCodigo, 'k1');
    });

    test('sin el campo (o nulo) queda sin comprobante', () {
      expect(PagoAlumno.fromJson(base()).comprobanteCodigo, isNull);
      expect(PagoAlumno.fromJson({...base(), 'comprobanteCodigo': null}).comprobanteCodigo, isNull);
    });
  });
}
