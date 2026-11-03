import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/clases/models/actualizar_clase_request.dart';
import 'package:mobile/features/clases/models/asistencia_clase.dart';
import 'package:mobile/features/clases/models/cerrar_clase_request.dart';
import 'package:mobile/features/clases/providers/agenda_provider.dart';
import 'package:mobile/features/clases/providers/clase_dictada_provider.dart';
import 'package:mobile/features/clases/providers/gestion_clase_provider.dart';
import 'package:mobile/features/pagos/providers/saldo_horas_provider.dart';

import '../../helpers/clases_falsos.dart';
import '../../helpers/pagos_falsos.dart';

void main() {
  late ClasesServiceFalso servicio;
  late PagosServiceFalso pagos;

  const cierre = CerrarClaseRequest(reservaCodigo: 'r1', asistencia: AsistenciaClase.presente, tema: 'Giros');

  setUp(() {
    servicio = ClasesServiceFalso();
    pagos = PagosServiceFalso()..saldo = 5;
  });

  ProviderContainer crear() {
    final container = ProviderContainer(overrides: [clasesServiceFalso(servicio), pagosServiceFalso(pagos)]);
    addTearDown(container.dispose);
    return container;
  }

  /// Mantiene vivos la agenda, el detalle de la clase y el saldo, y los carga, para ver después qué se vuelve a pedir.
  Future<void> cargarTodo(ProviderContainer container) async {
    container.listen(agendaListProvider, (_, _) {});
    container.listen(claseDictadaDetailProvider('c1'), (_, _) {});
    container.listen(saldoHorasProvider, (_, _) {});
    await container.read(agendaListProvider.future);
    await container.read(claseDictadaDetailProvider('c1').future);
    await container.read(saldoHorasProvider.future);
    servicio.pedidosDeAgenda.clear();
    servicio.clasesDictadasPedidas.clear();
    pagos.pedidosDeSaldo = 0;
  }

  group('cerrar', () {
    test('pide el cierre, devuelve la clase y refresca la agenda', () async {
      final container = crear();
      await cargarTodo(container);

      final clase = await container.read(gestionClaseProvider.notifier).cerrar(cierre);

      expect(servicio.cierresPedidos.single.reservaCodigo, 'r1');
      expect(clase.asistencia, AsistenciaClase.presente);
      expect(clase.tema, 'Giros');
      await container.read(agendaListProvider.future);
      expect(servicio.pedidosDeAgenda, hasLength(1));
    });

    test('con un error de negocio lo propaga con su código y refresca la agenda igual', () async {
      servicio.errorAlCerrarClase = ApiException(code: 'ERR_ACA_37', message: 'Esa clase no se puede cerrar');
      final container = crear();
      await cargarTodo(container);

      await expectLater(
        container.read(gestionClaseProvider.notifier).cerrar(cierre),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_37')),
      );

      await container.read(agendaListProvider.future);
      expect(servicio.pedidosDeAgenda, hasLength(1));
    });

    test('con un error de red lo propaga y refresca la agenda igual: el cierre pudo llegar a hacerse', () async {
      servicio.errorAlCerrarClase = DioException(requestOptions: RequestOptions(path: '/clases'));
      final container = crear();
      await cargarTodo(container);

      await expectLater(container.read(gestionClaseProvider.notifier).cerrar(cierre), throwsA(isA<DioException>()));

      await container.read(agendaListProvider.future);
      expect(servicio.pedidosDeAgenda, hasLength(1));
    });

    test('nunca pide el saldo de horas: cerrar no mueve horas', () async {
      final container = crear();
      await cargarTodo(container);

      await container.read(gestionClaseProvider.notifier).cerrar(cierre);
      await container.read(agendaListProvider.future);
      await container.read(saldoHorasProvider.future);

      expect(pagos.pedidosDeSaldo, 0);
    });
  });

  group('actualizar', () {
    const cambio = ActualizarClaseRequest(tema: 'Nuevo');

    test('pide la corrección, devuelve la clase y refresca la agenda y el detalle de esa clase', () async {
      final container = crear();
      await cargarTodo(container);

      final clase = await container.read(gestionClaseProvider.notifier).actualizar('c1', cambio);

      expect(servicio.correccionesPedidas.single.codigo, 'c1');
      expect(servicio.correccionesPedidas.single.request.tema, 'Nuevo');
      expect(clase.tema, 'Nuevo');
      await container.read(agendaListProvider.future);
      await container.read(claseDictadaDetailProvider('c1').future);
      expect(servicio.pedidosDeAgenda, hasLength(1));
      expect(servicio.clasesDictadasPedidas, ['c1']);
    });

    test('no refresca el detalle de otra clase', () async {
      final container = crear();
      await cargarTodo(container);
      container.listen(claseDictadaDetailProvider('c2'), (_, _) {});
      await container.read(claseDictadaDetailProvider('c2').future);
      servicio.clasesDictadasPedidas.clear();

      await container.read(gestionClaseProvider.notifier).actualizar('c1', cambio);
      await container.read(claseDictadaDetailProvider('c1').future);
      await container.read(claseDictadaDetailProvider('c2').future);

      expect(servicio.clasesDictadasPedidas, ['c1']);
    });

    test('con un error (de negocio o de red) lo propaga y refresca igual', () async {
      servicio.errorAlActualizarClase = ApiException(code: 'ERR_ACA_38', message: 'Datos inválidos');
      final container = crear();
      await cargarTodo(container);

      await expectLater(
        container.read(gestionClaseProvider.notifier).actualizar('c1', cambio),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_38')),
      );
      await container.read(agendaListProvider.future);
      await container.read(claseDictadaDetailProvider('c1').future);
      expect(servicio.pedidosDeAgenda, hasLength(1));
      expect(servicio.clasesDictadasPedidas, ['c1']);

      servicio.errorAlActualizarClase = DioException(requestOptions: RequestOptions(path: '/clases/c1'));
      await expectLater(
        container.read(gestionClaseProvider.notifier).actualizar('c1', cambio),
        throwsA(isA<DioException>()),
      );
    });

    test('nunca pide el saldo de horas', () async {
      final container = crear();
      await cargarTodo(container);

      await container.read(gestionClaseProvider.notifier).actualizar('c1', cambio);
      await container.read(saldoHorasProvider.future);

      expect(pagos.pedidosDeSaldo, 0);
    });
  });
}
