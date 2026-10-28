import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/simulacros/providers/rendir_provider.dart';

import '../../helpers/simulacros_falsos.dart';

void main() {
  late SimulacrosServiceFalso servicio;
  late ProviderContainer container;

  setUp(() {
    servicio = SimulacrosServiceFalso();
    container = ProviderContainer(
      overrides: [simulacrosServiceFalso(servicio)],
    );
    addTearDown(container.dispose);
  });

  Future<void> iniciar() =>
      container.read(rendirProvider.notifier).iniciar('s1');

  test(
    'sin intento, el estado es nulo; al iniciar queda en la primera pregunta sin respuestas',
    () async {
      expect(container.read(rendirProvider), isNull);

      await iniciar();

      final estado = container.read(rendirProvider)!;
      expect(servicio.iniciados, ['s1']);
      expect(estado.indice, 0);
      expect(estado.respondidas, 0);
      expect(estado.esUltima, isFalse);
      expect(
        estado.venceEn.isAfter(DateTime.now().add(const Duration(minutes: 19))),
        isTrue,
      );
    },
  );

  test(
    'en una pregunta de opción única, elegir otra opción reemplaza la anterior',
    () async {
      await iniciar();
      final notifier = container.read(rendirProvider.notifier);

      notifier.elegir('p1', 'A');
      notifier.elegir('p1', 'B');

      expect(container.read(rendirProvider)!.elegidas['p1'], {'B'});
    },
  );

  test(
    'en una de varias respuestas, elegir agrega y volver a elegir quita',
    () async {
      await iniciar();
      final notifier = container.read(rendirProvider.notifier);

      notifier.elegir('p2', 'A');
      notifier.elegir('p2', 'B');
      expect(container.read(rendirProvider)!.elegidas['p2'], {'A', 'B'});
      notifier.elegir('p2', 'A');

      expect(container.read(rendirProvider)!.elegidas['p2'], {'B'});
      expect(container.read(rendirProvider)!.respondidas, 1);
    },
  );

  test('irA se mueve entre preguntas y no se sale del rango', () async {
    await iniciar();
    final notifier = container.read(rendirProvider.notifier);

    notifier.irA(1);
    expect(container.read(rendirProvider)!.esUltima, isTrue);
    notifier.irA(2);
    notifier.irA(-1);

    expect(container.read(rendirProvider)!.indice, 1);
  });

  test(
    'finalizar envía lo marcado, devuelve la corrección y deja la app sin intento',
    () async {
      await iniciar();
      container.read(rendirProvider.notifier).elegir('p1', 'A');

      final resultado = await container
          .read(rendirProvider.notifier)
          .finalizar();

      expect(servicio.enviados.single, {
        'p1': {'A'},
      });
      expect(resultado.codigo, 'i1');
      expect(container.read(rendirProvider), isNull);
    },
  );

  test(
    'si finalizar falla, el intento sigue en la app para reintentar',
    () async {
      await iniciar();
      servicio.errorAlFinalizar = ApiException(code: 'ERR_X', message: 'falló');

      await expectLater(
        container.read(rendirProvider.notifier).finalizar(),
        throwsA(isA<ApiException>()),
      );

      expect(container.read(rendirProvider), isNotNull);
    },
  );

  test('si iniciar falla, no queda ningún intento', () async {
    servicio.errorAlIniciar = ApiException(
      code: 'ERR_ACA_28',
      message: 'sin preguntas',
    );

    await expectLater(iniciar(), throwsA(isA<ApiException>()));

    expect(container.read(rendirProvider), isNull);
  });
}
