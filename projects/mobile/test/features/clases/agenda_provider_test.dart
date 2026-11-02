import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/clases/providers/agenda_fecha_provider.dart';
import 'package:mobile/features/clases/providers/agenda_provider.dart';

import '../../helpers/clases_falsos.dart';

void main() {
  late ClasesServiceFalso servicio;

  setUp(() => servicio = ClasesServiceFalso());

  ProviderContainer crear() {
    final container = ProviderContainer(overrides: [clasesServiceFalso(servicio)]);
    addTearDown(container.dispose);
    return container;
  }

  test('pide el día elegido y entrega las clases en el orden del servidor', () async {
    servicio.clasesDeAgenda = [
      claseDeAgendaDePrueba(codigo: 'a', horaInicio: '09:00'),
      claseDeAgendaDePrueba(codigo: 'b', horaInicio: '10:00'),
    ];
    final container = crear();
    container.read(agendaFechaUiProvider.notifier).elegir(DateTime(2026, 11, 2));

    final clases = await container.read(agendaListProvider.future);

    expect(servicio.pedidosDeAgenda, [DateTime(2026, 11, 2)]);
    expect(clases.map((c) => c.codigo), ['a', 'b']);
  });

  test('un día sin clases es una lista vacía', () async {
    expect(await crear().read(agendaListProvider.future), isEmpty);
  });

  test('cambiar de día vuelve a pedir la agenda', () async {
    final container = crear();
    final notifier = container.read(agendaFechaUiProvider.notifier);
    notifier.elegir(DateTime(2026, 11, 2));
    container.listen(agendaListProvider, (_, _) {});
    await container.read(agendaListProvider.future);

    notifier.siguiente();
    await container.read(agendaListProvider.future);

    expect(servicio.pedidosDeAgenda, [DateTime(2026, 11, 2), DateTime(2026, 11, 3)]);
  });

  test('un error de negocio del servicio queda en el AsyncValue', () async {
    servicio.errorAgenda = ApiException(code: 'ERR_ACA_17', message: 'No pudimos obtener los datos');
    final container = crear();
    container.listen(agendaListProvider, (_, _) {});

    await expectLater(
      container.read(agendaListProvider.future),
      throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_17')),
    );

    expect(container.read(agendaListProvider).hasError, isTrue);
  });

  test('un error de red queda en el AsyncValue para poder reintentar', () async {
    servicio.errorAgenda = DioException(requestOptions: RequestOptions(path: '/reservas/agenda'));
    final container = crear();
    container.listen(agendaListProvider, (_, _) {});

    await expectLater(container.read(agendaListProvider.future), throwsA(isA<DioException>()));

    servicio.errorAgenda = null;
    container.invalidate(agendaListProvider);
    expect(await container.read(agendaListProvider.future), isEmpty);
  });
}
