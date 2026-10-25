import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/pagos/models/estado_pago_visible.dart';
import 'package:mobile/features/pagos/models/motivo_reembolso.dart';
import 'package:mobile/features/pagos/providers/pago_detail_provider.dart';
import 'package:mobile/features/pagos/providers/reembolso_provider.dart';
import 'package:mobile/features/pagos/providers/saldo_horas_provider.dart';

import '../../helpers/pagos_falsos.dart';

void main() {
  late PagosServiceFalso servicio;
  late ProviderContainer contenedor;

  setUp(() {
    servicio = PagosServiceFalso();
    contenedor = ProviderContainer(
      overrides: [
        pagosServiceFalso(servicio),
        intervaloConsultaReembolsoProvider.overrideWithValue(const Duration(seconds: 5)),
        plazoConsultaReembolsoProvider.overrideWithValue(const Duration(seconds: 20)),
      ],
    );
    // * Es autoDispose: la pantalla lo mantiene vivo
    contenedor.listen(reembolsoProvider('c1'), (_, _) {});
  });

  // * Cierra el contenedor al terminar: corta el seguimiento y no quedan temporizadores pendientes
  void prueba(String nombre, Future<void> Function(WidgetTester) cuerpo) => testWidgets(nombre, (tester) async {
    await cuerpo(tester);
    contenedor.dispose();
  });

  ReembolsoNotifier notifier() => contenedor.read(reembolsoProvider('c1').notifier);
  ResultadoReembolso? resultado() => contenedor.read(reembolsoProvider('c1')).valueOrNull;

  Future<void> avanzar(WidgetTester tester, Duration tiempo) async {
    await tester.pump(tiempo);
    await tester.pump();
  }

  /// Pide el reembolso y deja lista la espera de la pasarela.
  Future<void> pedido(WidgetTester tester) async {
    await tester.pump();
    await notifier().solicitar(MotivoReembolso.mudanza);
    await tester.pump();
  }

  prueba('arranca sin reembolso pedido', (tester) async {
    await tester.pump();
    expect(resultado(), ResultadoReembolso.ninguno);
  });

  prueba('solicitar envía el motivo y deja el reembolso en proceso', (tester) async {
    await tester.pump();

    expect(await notifier().solicitar(MotivoReembolso.mudanza), isTrue);

    expect(servicio.reembolsos.single.codigo, 'c1');
    expect(servicio.reembolsos.single.motivo, MotivoReembolso.mudanza);
    expect(resultado(), ResultadoReembolso.enProceso);
  });

  prueba('con «Otro» el texto viaja al servicio', (tester) async {
    await tester.pump();

    await notifier().solicitar(MotivoReembolso.otro, motivoDetalle: 'Perdí mi trabajo');

    expect(servicio.reembolsos.single.motivo, MotivoReembolso.otro);
    expect(servicio.reembolsos.single.detalle, 'Perdí mi trabajo');
  });

  prueba('un toque doble a la vez llama al servicio una sola vez', (tester) async {
    await tester.pump();
    final liberar = Completer<void>();
    servicio.antesDeReembolsar = () => liberar.future;

    final primero = notifier().solicitar(MotivoReembolso.mudanza);
    final segundo = notifier().solicitar(MotivoReembolso.mudanza);
    liberar.complete();

    expect(await segundo, isFalse);
    expect(await primero, isTrue);
    expect(servicio.reembolsos, hasLength(1));
  });

  prueba('al aceptarse se vuelve a pedir el saldo y el detalle que ya se habían leído', (tester) async {
    await tester.pump();
    contenedor.listen(saldoHorasProvider, (_, _) {});
    contenedor.listen(pagoDetailProvider('c1'), (_, _) {});
    await tester.pump();
    final saldosAntes = servicio.pedidosDeSaldo;
    final detallesAntes = servicio.pedidosDeDetalle;

    await notifier().solicitar(MotivoReembolso.sinTiempo);
    await tester.pump(const Duration(milliseconds: 10));
    await tester.pump();

    expect(servicio.pedidosDeSaldo, saldosAntes + 1);
    expect(servicio.pedidosDeDetalle, greaterThan(detallesAntes));
  });

  prueba('un error de negocio queda en el estado con su código, y se puede volver a intentar', (tester) async {
    await tester.pump();
    servicio.errorReembolso = ApiException(code: 'ERR_PAG_11', message: 'horas usadas');

    expect(await notifier().solicitar(MotivoReembolso.mudanza), isFalse);

    final error = contenedor.read(reembolsoProvider('c1')).error;
    expect(error, isA<ApiException>().having((e) => e.code, 'code', 'ERR_PAG_11'));

    servicio.errorReembolso = null;
    expect(await notifier().solicitar(MotivoReembolso.mudanza), isTrue);
  });

  prueba('un error de red también queda en el estado', (tester) async {
    await tester.pump();
    servicio.errorReembolso = DioException(requestOptions: RequestOptions(path: '/pagos/c1/reembolso'));

    expect(await notifier().solicitar(MotivoReembolso.mudanza), isFalse);
    expect(contenedor.read(reembolsoProvider('c1')).error, isA<DioException>());
  });

  prueba('consulta el detalle cada 5 segundos y termina en «reembolsado»', (tester) async {
    await pedido(tester);
    final consultasIniciales = servicio.pedidosDeDetalle;

    await avanzar(tester, const Duration(seconds: 5));
    expect(servicio.pedidosDeDetalle, consultasIniciales + 1);
    expect(resultado(), ResultadoReembolso.enProceso);

    servicio.detalles = {'c1': detalleDePrueba(estado: EstadoPagoVisible.reembolsado, horasRestantes: 0)};
    await avanzar(tester, const Duration(seconds: 5));
    expect(resultado(), ResultadoReembolso.reembolsado);

    final alTerminar = servicio.pedidosDeDetalle;
    await avanzar(tester, const Duration(seconds: 30));
    expect(servicio.pedidosDeDetalle, alTerminar);
  });

  prueba('si la compra vuelve a «confirmado», la devolución falló', (tester) async {
    await pedido(tester);

    servicio.detalles = {'c1': detalleDePrueba()};
    await avanzar(tester, const Duration(seconds: 5));

    expect(resultado(), ResultadoReembolso.fallo);
  });

  prueba('un error de red al consultar no corta el seguimiento', (tester) async {
    await pedido(tester);

    servicio.errorDetalle = DioException(requestOptions: RequestOptions(path: '/pagos/c1'));
    await avanzar(tester, const Duration(seconds: 5));
    expect(resultado(), ResultadoReembolso.enProceso);

    servicio.errorDetalle = null;
    servicio.detalles = {'c1': detalleDePrueba(estado: EstadoPagoVisible.reembolsado, horasRestantes: 0)};
    await avanzar(tester, const Duration(seconds: 5));
    expect(resultado(), ResultadoReembolso.reembolsado);
  });

  prueba('pasado el plazo deja de consultar', (tester) async {
    await pedido(tester);
    servicio.detalles = {'c1': detalleDePrueba(estado: EstadoPagoVisible.reembolsoEnProceso, horasRestantes: 0)};

    await avanzar(tester, const Duration(seconds: 20));
    final consultas = servicio.pedidosDeDetalle;
    await avanzar(tester, const Duration(seconds: 60));

    expect(servicio.pedidosDeDetalle, consultas);
    expect(resultado(), ResultadoReembolso.enProceso);
  });

  prueba('vigilar es idempotente: no abre dos seguimientos', (tester) async {
    await tester.pump();
    servicio.detalles = {'c1': detalleDePrueba(estado: EstadoPagoVisible.reembolsoEnProceso, horasRestantes: 0)};

    notifier().vigilar();
    notifier().vigilar();
    final antes = servicio.pedidosDeDetalle;
    await avanzar(tester, const Duration(seconds: 5));

    expect(servicio.pedidosDeDetalle, antes + 1);
    expect(resultado(), ResultadoReembolso.enProceso);
  });
}
