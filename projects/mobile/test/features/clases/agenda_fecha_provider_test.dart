import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/clases/providers/agenda_fecha_provider.dart';
import 'package:mobile/features/horarios/models/fechas.dart';

void main() {
  ProviderContainer crear() {
    final container = ProviderContainer();
    addTearDown(container.dispose);
    return container;
  }

  test('arranca en hoy, solo la fecha y sin hora', () {
    final fecha = crear().read(agendaFechaUiProvider);

    expect(fecha, soloFecha(DateTime.now()));
    expect(fecha.hour, 0);
    expect(fecha.minute, 0);
  });

  test('anterior y siguiente mueven el día de uno en uno', () {
    final container = crear();
    final notifier = container.read(agendaFechaUiProvider.notifier);
    final hoy = container.read(agendaFechaUiProvider);

    notifier.siguiente();
    notifier.siguiente();
    expect(container.read(agendaFechaUiProvider), sumarDias(hoy, 2));

    notifier.anterior();
    expect(container.read(agendaFechaUiProvider), sumarDias(hoy, 1));
  });

  test('siguiente cruza el fin de mes y el de año', () {
    final container = crear();
    final notifier = container.read(agendaFechaUiProvider.notifier);

    notifier.elegir(DateTime(2026, 12, 31));
    notifier.siguiente();

    expect(container.read(agendaFechaUiProvider), DateTime(2027, 1, 1));
  });

  test('elegir guarda solo la fecha, aunque llegue con hora', () {
    final container = crear();

    container.read(agendaFechaUiProvider.notifier).elegir(DateTime(2026, 11, 5, 18, 45));

    expect(container.read(agendaFechaUiProvider), DateTime(2026, 11, 5));
  });

  test('hoy vuelve al día de hoy desde cualquier otro', () {
    final container = crear();
    final notifier = container.read(agendaFechaUiProvider.notifier);
    notifier.elegir(DateTime(2020, 1, 1));

    notifier.hoy();

    expect(container.read(agendaFechaUiProvider), soloFecha(DateTime.now()));
  });
}
