import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/pagos/models/estado_pago_visible.dart';
import 'package:mobile/features/pagos/providers/pago_detail_provider.dart';
import 'package:mobile/features/pagos/providers/pagos_filtro_ui_provider.dart';
import 'package:mobile/features/pagos/providers/pagos_list_provider.dart';
import 'package:mobile/features/pagos/providers/refrescar_pagos.dart';
import 'package:mobile/features/pagos/providers/saldo_horas_provider.dart';

import '../../helpers/pagos_falsos.dart';

void main() {
  late PagosServiceFalso servicio;
  late ProviderContainer container;

  setUp(() {
    servicio = PagosServiceFalso();
    container = ProviderContainer(overrides: [pagosServiceFalso(servicio)]);
    addTearDown(container.dispose);
  });

  void hayCompras(int cantidad, {EstadoPagoVisible estado = EstadoPagoVisible.confirmado}) {
    servicio.compras = [for (var i = 0; i < cantidad; i++) compraDePrueba(codigo: 'c$i', estado: estado)];
  }

  Future<PagosListado> cargar() async {
    // * Se mantiene viva con un listener: es autoDispose
    container.listen(pagosListProvider, (_, _) {});
    return container.read(pagosListProvider.future);
  }

  PagosListado actual() => container.read(pagosListProvider).requireValue;

  test('carga la primera página de 20', () async {
    hayCompras(45);

    final listado = await cargar();

    expect(listado.items, hasLength(20));
    expect(listado.page, 1);
    expect(listado.totalPages, 3);
    expect(listado.hayMas, isTrue);
    expect(servicio.listados, [(estado: null, page: 1)]);
  });

  test('cargarMas agrega la siguiente página sin duplicar', () async {
    hayCompras(45);
    await cargar();

    await container.read(pagosListProvider.notifier).cargarMas();

    expect(actual().items, hasLength(40));
    expect(actual().items.map((c) => c.codigo).toSet(), hasLength(40));
    expect(actual().page, 2);
  });

  test('no pide más allá de la última página', () async {
    hayCompras(25);
    await cargar();
    final notifier = container.read(pagosListProvider.notifier);

    await notifier.cargarMas();
    await notifier.cargarMas();

    expect(actual().items, hasLength(25));
    expect(actual().hayMas, isFalse);
    expect(servicio.listados.map((l) => l.page), [1, 2]);
  });

  test('no pide la misma página dos veces a la vez', () async {
    hayCompras(45);
    await cargar();
    final notifier = container.read(pagosListProvider.notifier);

    await Future.wait([notifier.cargarMas(), notifier.cargarMas()]);

    expect(servicio.listados.map((l) => l.page), [1, 2]);
    expect(actual().items, hasLength(40));
  });

  test('cambiar el filtro reinicia la lista con el nuevo estado', () async {
    servicio.compras = [
      for (var i = 0; i < 30; i++) compraDePrueba(codigo: 'c$i'),
      compraDePrueba(codigo: 'f1', estado: EstadoPagoVisible.fallido),
    ];
    await cargar();
    await container.read(pagosListProvider.notifier).cargarMas();

    container.read(pagosFiltroUiProvider.notifier).elegir(EstadoPagoVisible.fallido);
    final listado = await container.read(pagosListProvider.future);

    expect(listado.items.map((c) => c.codigo), ['f1']);
    expect(listado.page, 1);
    expect(servicio.listados.last, (estado: EstadoPagoVisible.fallido, page: 1));
  });

  test('refrescar vuelve a pedir la primera página', () async {
    hayCompras(3);
    await cargar();
    hayCompras(5);

    await container.read(pagosListProvider.notifier).refrescar();

    expect(actual().items, hasLength(5));
  });

  test('un error en cargarMas conserva lo ya cargado y permite reintentar', () async {
    hayCompras(45);
    servicio.errorListado = Exception('sin red');
    servicio.errorListadoSoloDesdeLaPagina2 = true;
    await cargar();
    final notifier = container.read(pagosListProvider.notifier);

    await notifier.cargarMas();

    expect(actual().items, hasLength(20));
    expect(actual().errorAlCargarMas, isNotNull);
    expect(actual().cargandoMas, isFalse);

    servicio.errorListado = null;
    await notifier.cargarMas();

    expect(actual().items, hasLength(40));
    expect(actual().errorAlCargarMas, isNull);
  });

  test('un error al cargar la primera página deja el listado en error', () async {
    servicio.errorListado = Exception('sin red');
    container.listen(pagosListProvider, (_, _) {});

    await expectLater(container.read(pagosListProvider.future), throwsException);
    expect(container.read(pagosListProvider).hasError, isTrue);
  });

  test('el detalle pide la compra por su código', () async {
    servicio.detalles = {'c9': detalleDePrueba(codigo: 'c9', horasRestantes: 3)};
    container.listen(pagoDetailProvider('c9'), (_, _) {});

    final detalle = await container.read(pagoDetailProvider('c9').future);

    expect(detalle.horasRestantes, 3);
  });

  test('refrescarPagos vuelve a pedir el saldo, el listado y el detalle abiertos', () async {
    hayCompras(2);
    servicio.saldo = 4;
    container.listen(saldoHorasProvider, (_, _) {});
    container.listen(pagoDetailProvider('c0'), (_, _) {});
    await cargar();
    await container.read(saldoHorasProvider.future);
    await container.read(pagoDetailProvider('c0').future);
    expect((servicio.pedidosDeSaldo, servicio.listados.length, servicio.pedidosDeDetalle), (1, 1, 1));

    refrescarPagos(container.invalidate, container.exists);
    await container.read(saldoHorasProvider.future);
    await container.read(pagosListProvider.future);
    await container.read(pagoDetailProvider('c0').future);

    expect((servicio.pedidosDeSaldo, servicio.listados.length, servicio.pedidosDeDetalle), (2, 2, 2));
  });
}
