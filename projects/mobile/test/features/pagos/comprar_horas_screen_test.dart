import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/pagos/providers/compra_provider.dart';

import '../../helpers/pagos_app.dart';
import '../../helpers/pagos_falsos.dart';

void main() {
  late PagosServiceFalso servicio;
  late AbridorFalso abridor;

  setUp(() {
    servicio = PagosServiceFalso();
    abridor = AbridorFalso();
  });

  testWidgets('muestra cada plan con su nombre, sus horas, el precio en Bs y en USD, y el botón Comprar', (tester) async {
    servicio.planes = [
      planDePrueba(codigo: 'p1', nombre: 'Clase suelta (1 hora)', horas: 1, costoBs: 60, costoUsd: 5.1),
      planDePrueba(codigo: 'p10', nombre: 'Plan 10 horas', horas: 10, costoBs: 1250.5, costoUsd: 106.25),
    ];
    final container = containerDePagos(servicio, abridor);
    await montarPagos(tester, container);

    expect(find.text('Comprar horas'), findsOneWidget);
    expect(find.text('Clase suelta (1 hora)'), findsOneWidget);
    expect(find.text('1 hora'), findsOneWidget);
    expect(find.text('Bs 60,00'), findsOneWidget);
    expect(find.text('≈ US\$ 5,10'), findsOneWidget);
    expect(find.text('Plan 10 horas'), findsOneWidget);
    expect(find.text('10 horas'), findsOneWidget);
    expect(find.text('Bs 1.250,50'), findsOneWidget);
    expect(find.text('≈ US\$ 106,25'), findsOneWidget);
    expect(find.byKey(const Key('comprar-p1')), findsOneWidget);
    expect(find.byKey(const Key('comprar-p10')), findsOneWidget);
    await desmontar(tester, container);
  });

  testWidgets('los planes salen en el orden que manda el backend', (tester) async {
    servicio.planes = [
      planDePrueba(codigo: 'a', nombre: 'Primero', horas: 1),
      planDePrueba(codigo: 'b', nombre: 'Segundo', horas: 5),
      planDePrueba(codigo: 'c', nombre: 'Tercero', horas: 10),
    ];
    final container = containerDePagos(servicio, abridor);
    await montarPagos(tester, container);

    final arriba = tester.getTopLeft(find.text('Primero')).dy;
    final medio = tester.getTopLeft(find.text('Segundo')).dy;
    final abajo = tester.getTopLeft(find.text('Tercero')).dy;
    expect(arriba, lessThan(medio));
    expect(medio, lessThan(abajo));
    await desmontar(tester, container);
  });

  testWidgets('sin planes muestra que la escuela todavía no ofrece', (tester) async {
    final container = containerDePagos(servicio, abridor);
    await montarPagos(tester, container);

    expect(find.text('Sin planes por ahora'), findsOneWidget);
    expect(find.text('Tu escuela todavía no ofrece planes de horas.'), findsOneWidget);
    expect(find.text('Comprar'), findsNothing);
    await desmontar(tester, container);
  });

  testWidgets('si falla la carga muestra un aviso con Reintentar, y reintentar vuelve a pedir', (tester) async {
    servicio.errorPlanes = Exception('sin red');
    final container = containerDePagos(servicio, abridor);
    await montarPagos(tester, container);

    expect(find.textContaining('No pudimos cargar los planes'), findsOneWidget);
    expect(find.byKey(const Key('botonReintentarPlanes')), findsOneWidget);

    servicio.errorPlanes = null;
    servicio.planes = [planDePrueba()];
    await tester.tap(find.byKey(const Key('botonReintentarPlanes')));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 50));

    expect(find.text('Plan 10 horas'), findsOneWidget);
    expect(find.textContaining('No pudimos cargar los planes'), findsNothing);
    expect(servicio.pedidosDePlanes, 2);
    await desmontar(tester, container);
  });

  testWidgets('Comprar guarda el plan en la compra en curso (con su clave) y va al resumen', (tester) async {
    servicio.planes = [planDePrueba(codigo: 'p5', nombre: 'Plan 5 horas', horas: 5)];
    final container = containerDePagos(servicio, abridor);
    final router = await montarPagos(tester, container);

    await tester.tap(find.byKey(const Key('comprar-p5')));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));

    final compra = container.read(compraUiProvider);
    expect(compra.plan!.codigo, 'p5');
    expect(compra.clave, isNotNull);
    expect(compra.clave, hasLength(32));
    expect(router.state.uri.path, '/payments/summary');
    expect(find.text('Resumen de la compra'), findsOneWidget);
    await desmontar(tester, container);
  });

  testWidgets('volver a la lista y elegir el mismo plan conserva la clave de la compra', (tester) async {
    servicio.planes = [planDePrueba(codigo: 'p5', nombre: 'Plan 5 horas', horas: 5)];
    final container = containerDePagos(servicio, abridor);
    final router = await montarPagos(tester, container);
    await tester.tap(find.byKey(const Key('comprar-p5')));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));
    final clave = container.read(compraUiProvider).clave;

    router.pop();
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));
    await tester.tap(find.byKey(const Key('comprar-p5')));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));

    expect(container.read(compraUiProvider).clave, clave);
    await desmontar(tester, container);
  });
}
