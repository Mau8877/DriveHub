import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/pagos/models/comprobante_resumen.dart';
import 'package:mobile/features/pagos/models/estado_pago_visible.dart';
import 'package:mobile/features/pagos/models/pago_detalle.dart';
import 'package:mobile/features/pagos/screens/pago_detalle_screen.dart';

import '../../helpers/comprobantes_falsos.dart';
import '../../helpers/pagos_falsos.dart';

const _compra = ComprobanteResumen(codigo: 'k1', tipo: TipoComprobante.pago, numero: 'CMP-2026-000123', disponible: true);
const _reembolso = ComprobanteResumen(codigo: 'k2', tipo: TipoComprobante.reembolso, numero: 'RMB-2026-000124', disponible: true);

void main() {
  late PagosServiceFalso servicio;
  late ComprobantesServiceFalso comprobantes;
  late AbridorFalso abridor;

  setUp(() {
    servicio = PagosServiceFalso();
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
            body: TextButton(onPressed: () => context.push('/payments/c1'), child: const Text('ABRIR')),
          ),
        ),
        GoRoute(path: '/payments/:codigo', builder: (_, s) => PagoDetalleScreen(codigo: s.pathParameters['codigo']!)),
      ],
    );
    await tester.pumpWidget(
      ProviderScope(
        overrides: [pagosServiceFalso(servicio), ...comprobantesFalsos(comprobantes, abridor)],
        child: MaterialApp.router(theme: AppTheme.light, routerConfig: router),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('ABRIR'));
    await tester.pumpAndSettle();
  }

  testWidgets('con el comprobante de la compra: muestra su número y su botón', (tester) async {
    servicio.detalles = {'c1': detalleDePrueba(comprobantes: [_compra])};

    await montar(tester);

    expect(find.byKey(const Key('seccionComprobantes')), findsOneWidget);
    expect(find.text('Comprobante de pago CMP-2026-000123'), findsOneWidget);
    expect(find.byKey(const Key('botonComprobante-k1')), findsOneWidget);
  });

  testWidgets('con el de la compra y el del reembolso: una fila y un botón por cada uno', (tester) async {
    servicio.detalles = {
      'c1': detalleDePrueba(estado: EstadoPagoVisible.reembolsado, horasRestantes: 0, comprobantes: [_compra, _reembolso]),
    };

    await montar(tester);

    expect(find.text('Comprobante de pago CMP-2026-000123'), findsOneWidget);
    expect(find.text('Comprobante de reembolso RMB-2026-000124'), findsOneWidget);
    expect(find.byKey(const Key('botonComprobante-k1')), findsOneWidget);
    expect(find.byKey(const Key('botonComprobante-k2')), findsOneWidget);
  });

  testWidgets('un comprobante en preparación muestra «En preparación» en vez del botón', (tester) async {
    const enPreparacion = ComprobanteResumen(
      codigo: 'k3',
      tipo: TipoComprobante.pago,
      numero: 'CMP-2026-000125',
      disponible: false,
    );
    servicio.detalles = {'c1': detalleDePrueba(comprobantes: [enPreparacion])};

    await montar(tester);

    expect(find.byKey(const Key('enPreparacion-k3')), findsOneWidget);
    expect(find.text('En preparación'), findsOneWidget);
    expect(find.byKey(const Key('botonComprobante-k3')), findsNothing);
  });

  testWidgets('sin comprobantes la sección no aparece', (tester) async {
    servicio.detalles = {'c1': detalleDePrueba()};

    await montar(tester);

    expect(find.byKey(const Key('seccionComprobantes')), findsNothing);
    expect(find.text('Comprobantes'), findsNothing);
  });

  testWidgets('tocar el botón descarga y abre ese comprobante', (tester) async {
    servicio.detalles = {'c1': detalleDePrueba(comprobantes: [_compra, _reembolso])};
    await montar(tester);

    await tester.tap(find.byKey(const Key('botonComprobante-k2')));
    await tester.pumpAndSettle();

    expect(comprobantes.descargas, ['k2']);
    expect(abridor.abiertos.single, endsWith('comprobante-k2.pdf'));
  });

  group('fromJson', () {
    Map<String, dynamic> base() => {
      'codigo': 'c1',
      'planNombre': 'Plan 10 horas',
      'horas': 10,
      'montoUsd': 38.23,
      'montoBs': 450.0,
      'estado': 'CONFIRMADO',
      'creadoEn': '2026-05-04T16:00:00Z',
      'reembolsable': false,
    };

    test('lee los comprobantes con sus campos', () {
      final detalle = PagoDetalle.fromJson({
        ...base(),
        'comprobantes': [
          {'codigo': 'k1', 'tipo': 'PAGO', 'numero': 'CMP-2026-000123', 'disponible': true},
          {'codigo': 'k2', 'tipo': 'REEMBOLSO', 'numero': 'RMB-2026-000124', 'disponible': false},
        ],
      });

      expect(detalle.comprobantes.map((c) => c.tipo), [TipoComprobante.pago, TipoComprobante.reembolso]);
      expect(detalle.comprobantes.map((c) => c.numero), ['CMP-2026-000123', 'RMB-2026-000124']);
      expect(detalle.comprobantes.map((c) => c.disponible), [true, false]);
    });

    test('sin el campo comprobantes (o vacío) queda una lista vacía', () {
      expect(PagoDetalle.fromJson(base()).comprobantes, isEmpty);
      expect(PagoDetalle.fromJson({...base(), 'comprobantes': <dynamic>[]}).comprobantes, isEmpty);
    });

    test('un tipo desconocido no rompe', () {
      final c = ComprobanteResumen.fromJson({'codigo': 'k', 'tipo': 'NUEVO', 'numero': 'X-1', 'disponible': true});
      expect(c.tipo, TipoComprobante.desconocido);
      expect(c.tipo.etiqueta, 'Comprobante');
    });
  });
}
