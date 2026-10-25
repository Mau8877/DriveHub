import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/features/pagos/providers/abridor_provider.dart';
import 'package:mobile/features/pagos/screens/comprar_horas_screen.dart';
import 'package:mobile/features/pagos/screens/pago_resultado_screen.dart';
import 'package:mobile/features/pagos/screens/resumen_compra_screen.dart';

import 'pagos_falsos.dart';

/// Quien "abre" la página de pago en los tests: anota las direcciones y responde lo configurado.
class AbridorFalso {
  final List<String> abiertas = [];
  bool resultado = true;

  Future<bool> call(String url) async {
    abiertas.add(url);
    return resultado;
  }
}

/// Un container con el servicio de pagos y el abridor falsos, sin la sesión ni el router de verdad.
ProviderContainer containerDePagos(PagosServiceFalso servicio, AbridorFalso abridor) {
  final container = ProviderContainer(
    overrides: [pagosServiceFalso(servicio), abridorDePaginaProvider.overrideWithValue(abridor.call)],
  );
  addTearDown(container.dispose);
  return container;
}

/// Lo que devuelve montar las pantallas: el container y el router, para inspeccionar el estado y la ruta.
class MontajeDePagos {
  final ProviderContainer container;
  final GoRouter router;

  const MontajeDePagos(this.container, this.router);
}

/// Monta las pantallas de la compra con un router mínimo (las mismas rutas que el real) y devuelve el router
/// para poder ver dónde terminó la navegación.
Future<GoRouter> montarPagos(WidgetTester tester, ProviderContainer container, {String inicial = '/payments/plans'}) async {
  final router = GoRouter(
    initialLocation: inicial,
    routes: [
      GoRoute(path: '/payments/plans', builder: (_, _) => const ComprarHorasScreen()),
      GoRoute(path: '/payments/summary', builder: (_, _) => const ResumenCompraScreen()),
      GoRoute(path: '/payments/result', builder: (_, _) => const PagoResultadoScreen()),
      GoRoute(path: '/inicio', builder: (_, _) => const Scaffold(body: Text('PANTALLA DE INICIO'))),
    ],
  );
  await tester.pumpWidget(
    UncontrolledProviderScope(container: container, child: MaterialApp.router(routerConfig: router)),
  );
  await tester.pump();
  await tester.pump(const Duration(milliseconds: 50));
  return router;
}

/// Quita las pantallas y los providers: sin esto quedan temporizadores pendientes y el test falla.
Future<void> desmontar(WidgetTester tester, ProviderContainer container) async {
  await tester.pumpWidget(const SizedBox());
  container.dispose();
}
