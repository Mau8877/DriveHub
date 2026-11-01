import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/clases/models/hora_de_cancelacion.dart';
import 'package:mobile/features/clases/models/resultado_cancelacion.dart';
import 'package:mobile/features/clases/models/vista_reservas.dart';
import 'package:mobile/features/clases/providers/franja_para_reprogramar_provider.dart';
import 'package:mobile/features/clases/providers/franjas_disponibles_provider.dart';
import 'package:mobile/features/clases/providers/gestion_reserva_provider.dart';
import 'package:mobile/features/pagos/providers/saldo_horas_provider.dart';

import '../../helpers/clases_falsos.dart';
import '../../helpers/pagos_falsos.dart';

void main() {
  late ClasesServiceFalso servicio;
  late PagosServiceFalso pagos;

  setUp(() {
    servicio = ClasesServiceFalso();
    pagos = PagosServiceFalso()..saldo = 5;
  });

  ProviderContainer crear() {
    final container = ProviderContainer(overrides: [clasesServiceFalso(servicio), pagosServiceFalso(pagos)]);
    addTearDown(container.dispose);
    return container;
  }

  /// Mantiene vivos los listados y el saldo y los carga, para ver después que se vuelven a pedir al invalidarse.
  Future<void> cargarTodo(ProviderContainer container) async {
    container.listen(franjasDisponiblesListProvider, (_, _) {});
    container.listen(instructoresDisponiblesListProvider, (_, _) {});
    container.listen(reservasListProvider(VistaReservas.proximas), (_, _) {});
    container.listen(saldoHorasProvider, (_, _) {});
    await container.read(franjasDisponiblesListProvider.future);
    await container.read(instructoresDisponiblesListProvider.future);
    await container.read(reservasListProvider(VistaReservas.proximas).future);
    await container.read(saldoHorasProvider.future);
    servicio.pedidosDeFranjas.clear();
    servicio.pedidosDeInstructores.clear();
    servicio.pedidosDeReservas.clear();
    pagos.pedidosDeSaldo = 0;
  }

  Future<void> releer(ProviderContainer container) async {
    await container.read(franjasDisponiblesListProvider.future);
    await container.read(instructoresDisponiblesListProvider.future);
    await container.read(reservasListProvider(VistaReservas.proximas).future);
    await container.read(saldoHorasProvider.future);
  }

  group('cancelar', () {
    test('pide la cancelación y refresca las clases, las franjas y el saldo', () async {
      final container = crear();
      await cargarTodo(container);

      final resultado = await container.read(gestionReservaProvider.notifier).cancelar('r1');

      expect(servicio.cancelacionesPedidas, ['r1']);
      expect(resultado.horaDevuelta, HoraDeCancelacion.devuelta);
      await releer(container);
      expect(servicio.pedidosDeReservas, [VistaReservas.proximas]);
      expect(servicio.pedidosDeFranjas, hasLength(1));
      expect(servicio.pedidosDeInstructores, hasLength(1));
      expect(pagos.pedidosDeSaldo, 1);
    });

    test('devuelve tal cual lo que respondió el servicio (hora perdida)', () async {
      servicio.alCancelar = (codigo) => ResultadoCancelacion(
        reserva: reservaDePrueba(codigo: codigo),
        horaDevuelta: HoraDeCancelacion.perdida,
        horasDisponibles: 3,
      );
      final container = crear();

      final resultado = await container.read(gestionReservaProvider.notifier).cancelar('r1');

      expect(resultado.horaDevuelta, HoraDeCancelacion.perdida);
      expect(resultado.horasDisponibles, 3);
    });

    test('con un error de red lo propaga, pero igual refresca las clases, las franjas y el saldo', () async {
      servicio.errorAlCancelar = DioException(requestOptions: RequestOptions(path: '/reservas/r1/cancelar'));
      final container = crear();
      await cargarTodo(container);

      await expectLater(container.read(gestionReservaProvider.notifier).cancelar('r1'), throwsA(isA<DioException>()));

      await releer(container);
      expect(servicio.pedidosDeReservas, [VistaReservas.proximas]);
      expect(servicio.pedidosDeFranjas, hasLength(1));
      expect(pagos.pedidosDeSaldo, 1);
    });

    test('un error de negocio llega como ApiException con su código', () async {
      servicio.errorAlCancelar = ApiException(code: 'ERR_ACA_33', message: 'Esa clase ya no se puede cancelar');
      final container = crear();

      await expectLater(
        container.read(gestionReservaProvider.notifier).cancelar('r1'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_33')),
      );
    });
  });

  group('reprogramar', () {
    test('pide el cambio y refresca las clases y las franjas, pero NO el saldo', () async {
      final container = crear();
      await cargarTodo(container);

      final reserva = await container.read(gestionReservaProvider.notifier).reprogramar('r1', 'f9');

      expect(servicio.reprogramacionesPedidas.single, (reserva: 'r1', franja: 'f9'));
      expect(reserva.codigo, 'r1');
      await container.read(franjasDisponiblesListProvider.future);
      await container.read(instructoresDisponiblesListProvider.future);
      await container.read(reservasListProvider(VistaReservas.proximas).future);
      expect(servicio.pedidosDeReservas, [VistaReservas.proximas]);
      expect(servicio.pedidosDeFranjas, hasLength(1));
      expect(pagos.pedidosDeSaldo, 0);
    });

    test('si la franja ya no está libre propaga ERR_ACA_18 y refresca las franjas para elegir otra', () async {
      servicio.errorAlReprogramar = ApiException(code: 'ERR_ACA_18', message: 'La franja ya no está disponible');
      final container = crear();
      await cargarTodo(container);

      await expectLater(
        container.read(gestionReservaProvider.notifier).reprogramar('r1', 'f9'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_18')),
      );

      await container.read(franjasDisponiblesListProvider.future);
      await container.read(reservasListProvider(VistaReservas.proximas).future);
      expect(servicio.pedidosDeFranjas, hasLength(1));
      expect(servicio.pedidosDeReservas, [VistaReservas.proximas]);
      expect(pagos.pedidosDeSaldo, 0);
    });
  });

  group('franja para reprogramar', () {
    test('empieza sin ninguna elegida', () {
      expect(crear().read(franjaParaReprogramarUiProvider), isNull);
    });

    test('elegir guarda una sola franja y elegir otra reemplaza a la anterior', () {
      final container = crear();
      final notifier = container.read(franjaParaReprogramarUiProvider.notifier);

      notifier.elegir(franjaDisponibleDePrueba(codigo: 'a'));
      notifier.elegir(franjaDisponibleDePrueba(codigo: 'b'));

      expect(container.read(franjaParaReprogramarUiProvider)?.codigo, 'b');
    });

    test('volver a tocar la elegida la quita', () {
      final container = crear();
      final notifier = container.read(franjaParaReprogramarUiProvider.notifier);

      notifier.elegir(franjaDisponibleDePrueba(codigo: 'a'));
      notifier.elegir(franjaDisponibleDePrueba(codigo: 'a'));

      expect(container.read(franjaParaReprogramarUiProvider), isNull);
    });

    test('vaciar la deja sin elegir', () {
      final container = crear();
      final notifier = container.read(franjaParaReprogramarUiProvider.notifier);
      notifier.elegir(franjaDisponibleDePrueba(codigo: 'a'));

      notifier.vaciar();

      expect(container.read(franjaParaReprogramarUiProvider), isNull);
    });
  });
}
