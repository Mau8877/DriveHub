import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/clases/models/hora_de_cancelacion.dart';
import 'package:mobile/features/clases/models/resultado_cancelacion_instructor.dart';
import 'package:mobile/features/clases/providers/agenda_provider.dart';
import 'package:mobile/features/clases/providers/gestion_agenda_provider.dart';

import '../../helpers/clases_falsos.dart';

void main() {
  late ClasesServiceFalso servicio;

  setUp(() => servicio = ClasesServiceFalso());

  ProviderContainer crear() {
    final container = ProviderContainer(overrides: [clasesServiceFalso(servicio)]);
    addTearDown(container.dispose);
    return container;
  }

  /// Mantiene viva la agenda y la carga, para ver después que se vuelve a pedir al invalidarse.
  Future<void> cargarAgenda(ProviderContainer container) async {
    container.listen(agendaListProvider, (_, _) {});
    await container.read(agendaListProvider.future);
    servicio.pedidosDeAgenda.clear();
  }

  test('pide la cancelación, devuelve el resultado y refresca la agenda', () async {
    final container = crear();
    await cargarAgenda(container);

    final resultado = await container.read(gestionAgendaProvider.notifier).cancelar('r1');

    expect(servicio.cancelacionesDeInstructorPedidas, ['r1']);
    expect(resultado.horaDevuelta, HoraDeCancelacion.devuelta);
    await container.read(agendaListProvider.future);
    expect(servicio.pedidosDeAgenda, hasLength(1));
  });

  test('devuelve tal cual la hora en proceso cuando Pagos no respondió', () async {
    servicio.alCancelarComoInstructor = (codigo) => ResultadoCancelacionInstructor(
      clase: claseDeAgendaDePrueba(codigo: codigo),
      horaDevuelta: HoraDeCancelacion.enProceso,
    );
    final container = crear();

    final resultado = await container.read(gestionAgendaProvider.notifier).cancelar('r1');

    expect(resultado.horaDevuelta, HoraDeCancelacion.enProceso);
  });

  test('un error de negocio se propaga con su código y la agenda se refresca igual', () async {
    servicio.errorAlCancelarComoInstructor = ApiException(
      code: 'ERR_ACA_36',
      message: 'Esa clase ya no se puede cancelar',
    );
    final container = crear();
    await cargarAgenda(container);

    await expectLater(
      container.read(gestionAgendaProvider.notifier).cancelar('r1'),
      throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_36')),
    );

    await container.read(agendaListProvider.future);
    expect(servicio.pedidosDeAgenda, hasLength(1));
  });

  test('un error de red se propaga y la agenda se refresca igual: la cancelación pudo llegar a hacerse', () async {
    servicio.errorAlCancelarComoInstructor = DioException(
      requestOptions: RequestOptions(path: '/reservas/r1/cancelar-por-instructor'),
    );
    final container = crear();
    await cargarAgenda(container);

    await expectLater(container.read(gestionAgendaProvider.notifier).cancelar('r1'), throwsA(isA<DioException>()));

    await container.read(agendaListProvider.future);
    expect(servicio.pedidosDeAgenda, hasLength(1));
  });
}
