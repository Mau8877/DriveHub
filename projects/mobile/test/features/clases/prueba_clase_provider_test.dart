import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/clases/models/evaluar_clase_request.dart';
import 'package:mobile/features/clases/models/tipo_prueba.dart';
import 'package:mobile/features/clases/providers/agenda_provider.dart';
import 'package:mobile/features/clases/providers/catalogo_pruebas_provider.dart';
import 'package:mobile/features/clases/providers/gestion_prueba_clase_provider.dart';
import 'package:mobile/features/clases/providers/pruebas_de_clase_provider.dart';

import '../../helpers/clases_falsos.dart';

void main() {
  late ClasesServiceFalso servicio;

  const pedido = EvaluarClaseRequest(
    claseCodigo: 'c1',
    tipo: TipoPrueba.manejo,
    criterios: [
      PuntajeDeCriterio(codigo: 'manejo_a', puntaje: 8),
      PuntajeDeCriterio(codigo: 'manejo_b', puntaje: 8),
    ],
  );

  setUp(() => servicio = ClasesServiceFalso());

  ProviderContainer crear() {
    final container = ProviderContainer(overrides: [clasesServiceFalso(servicio)]);
    addTearDown(container.dispose);
    return container;
  }

  group('catalogoPruebasProvider', () {
    test('se pide una sola vez aunque se lea dos veces', () async {
      final container = crear();

      await container.read(catalogoPruebasProvider.future);
      await container.read(catalogoPruebasProvider.future);

      expect(servicio.pedidosDeCatalogo, 1);
    });

    test('un error queda en el estado y al invalidar se vuelve a pedir', () async {
      servicio.errorCatalogo = DioException(requestOptions: RequestOptions(path: '/x'));
      final container = crear();
      container.listen(catalogoPruebasProvider, (_, _) {});

      await expectLater(container.read(catalogoPruebasProvider.future), throwsA(isA<DioException>()));
      servicio.errorCatalogo = null;
      container.invalidate(catalogoPruebasProvider);
      final catalogo = await container.read(catalogoPruebasProvider.future);

      expect(catalogo.tipos, hasLength(3));
      expect(servicio.pedidosDeCatalogo, 2);
    });
  });

  group('pruebasDeClaseDetailProvider', () {
    test('pide las pruebas por el código de la clase y cada clase pide las suyas', () async {
      servicio.pruebasPorClase['c1'] = [pruebaClaseDePrueba()];
      final container = crear();

      final deC1 = await container.read(pruebasDeClaseDetailProvider('c1').future);
      final deC2 = await container.read(pruebasDeClaseDetailProvider('c2').future);

      expect(deC1, hasLength(1));
      expect(deC2, isEmpty);
      expect(servicio.pruebasPedidas, ['c1', 'c2']);
    });

    test('un error (clase ajena) queda en el estado para ofrecer reintentar', () async {
      servicio.errorPruebasDeClase = ApiException(code: 'ERR_ACA_35', message: 'No encontramos esa clase');
      final container = crear();
      container.listen(pruebasDeClaseDetailProvider('c1'), (_, _) {});

      await expectLater(
        container.read(pruebasDeClaseDetailProvider('c1').future),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_35')),
      );
    });
  });

  group('gestionPruebaClaseProvider.evaluar', () {
    /// Mantiene vivos la agenda y las pruebas de la clase, y los carga, para ver después qué se vuelve a pedir.
    Future<void> cargarTodo(ProviderContainer container) async {
      container.listen(agendaListProvider, (_, _) {});
      container.listen(pruebasDeClaseDetailProvider('c1'), (_, _) {});
      await container.read(agendaListProvider.future);
      await container.read(pruebasDeClaseDetailProvider('c1').future);
      servicio.pedidosDeAgenda.clear();
      servicio.pruebasPedidas.clear();
    }

    test('pide la evaluación, devuelve la prueba y refresca la agenda y las pruebas de esa clase', () async {
      final container = crear();
      await cargarTodo(container);

      final prueba = await container.read(gestionPruebaClaseProvider.notifier).evaluar(pedido);
      await container.read(agendaListProvider.future);
      final pruebas = await container.read(pruebasDeClaseDetailProvider('c1').future);

      expect(servicio.evaluacionesPedidas, [pedido]);
      expect(prueba.puntajeTotal, 80);
      expect(servicio.pedidosDeAgenda, hasLength(1));
      expect(servicio.pruebasPedidas, ['c1']);
      expect(pruebas.map((p) => p.codigo), [prueba.codigo]);
    });

    test('un error de negocio se propaga y aun así refresca la agenda (el servidor pudo haber guardado)', () async {
      for (final codigo in ['ERR_ACA_39', 'ERR_ACA_40', 'ERR_ACA_41']) {
        servicio.errorAlEvaluarClase = ApiException(code: codigo, message: 'No se pudo');
        final container = crear();
        await cargarTodo(container);

        await expectLater(
          container.read(gestionPruebaClaseProvider.notifier).evaluar(pedido),
          throwsA(isA<ApiException>().having((e) => e.code, 'code', codigo)),
        );
        await container.read(agendaListProvider.future);

        expect(servicio.pedidosDeAgenda, hasLength(1), reason: codigo);
      }
    });

    test('un error de red se propaga y refresca la agenda', () async {
      servicio.errorAlEvaluarClase = DioException(requestOptions: RequestOptions(path: '/x'));
      final container = crear();
      await cargarTodo(container);

      await expectLater(
        container.read(gestionPruebaClaseProvider.notifier).evaluar(pedido),
        throwsA(isA<DioException>()),
      );
      await container.read(agendaListProvider.future);

      expect(servicio.pedidosDeAgenda, hasLength(1));
    });
  });
}
