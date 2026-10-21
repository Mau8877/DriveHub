import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/horarios/models/crear_plantilla_horario_request.dart';
import 'package:mobile/features/horarios/providers/franjas_provider.dart';
import 'package:mobile/features/horarios/providers/horarios_provider.dart';

import '../../helpers/horarios_falsos.dart';

void main() {
  late HorariosServiceFalso servicio;

  const request = CrearPlantillaHorarioRequest(
    diaSemana: 1,
    horaInicio: '08:00',
    horaFin: '12:00',
    duracionClaseMin: 60,
  );

  ProviderContainer crear() {
    final container = ProviderContainer(overrides: [horariosServiceFalso(servicio)]);
    addTearDown(container.dispose);
    return container;
  }

  setUp(() => servicio = HorariosServiceFalso());

  group('horariosListProvider', () {
    test('por defecto pide solo los activos', () async {
      servicio.horarios = [horarioDePrueba()];
      final container = crear();

      final lista = await container.read(horariosListProvider.future);

      expect(servicio.ultimaActiva, isTrue);
      expect(lista.single.codigo, 'h1');
    });

    test('al incluir inactivos pide sin filtro', () async {
      servicio.horarios = [horarioDePrueba(), horarioDePrueba(codigo: 'h2', activa: false)];
      final container = crear();
      container.read(horariosIncluirInactivosUiProvider.notifier).fijar(true);

      final lista = await container.read(horariosListProvider.future);

      expect(servicio.ultimaActiva, isNull);
      expect(lista, hasLength(2));
    });

    test('el estado inicial de incluir inactivos es falso', () {
      expect(crear().read(horariosIncluirInactivosUiProvider), isFalse);
    });

    test('un error del servicio queda en el AsyncValue', () async {
      servicio.error = ApiException(code: 'ERR_SYS_00', message: 'falló');
      final container = crear();
      final suscripcion = container.listen(horariosListProvider, (_, _) {});
      addTearDown(suscripcion.close);

      await expectLater(container.read(horariosListProvider.future), throwsA(isA<ApiException>()));

      expect(container.read(horariosListProvider).hasError, isTrue);
    });
  });

  group('horariosProvider', () {
    test('crear devuelve lo creado e invalida las listas de horarios y de franjas', () async {
      final container = crear();
      final fecha = DateTime(2026, 11, 2);
      // * Se mantienen vivas para ver que se vuelven a pedir al invalidarse
      container.listen(horariosListProvider, (_, _) {});
      container.listen(franjasListProvider(fecha), (_, _) {});
      await container.read(horariosListProvider.future);
      await container.read(franjasListProvider(fecha).future);
      servicio.llamadas.clear();

      final creada = await container.read(horariosProvider.notifier).crear(request);
      await container.read(horariosListProvider.future);
      await container.read(franjasListProvider(fecha).future);

      expect(creada.franjasGeneradas, 14);
      expect(servicio.ultimoRequest, request);
      expect(servicio.llamadas, containsAllInOrder(['crearPlantilla', 'listarPlantillas', 'listarFranjas']));
    });

    test('desactivar llama al servicio con el código e invalida las dos listas', () async {
      final container = crear();
      final fecha = DateTime(2026, 11, 2);
      container.listen(horariosListProvider, (_, _) {});
      container.listen(franjasListProvider(fecha), (_, _) {});
      await container.read(horariosListProvider.future);
      await container.read(franjasListProvider(fecha).future);
      servicio.llamadas.clear();

      await container.read(horariosProvider.notifier).desactivar('h1');
      await container.read(horariosListProvider.future);
      await container.read(franjasListProvider(fecha).future);

      expect(servicio.llamadas, containsAllInOrder(['desactivarPlantilla:h1', 'listarPlantillas', 'listarFranjas']));
    });

    test('si crear falla, el error se relanza y no se invalida nada', () async {
      final container = crear();
      container.listen(horariosListProvider, (_, _) {});
      await container.read(horariosListProvider.future);
      servicio.llamadas.clear();
      servicio.error = ApiException(code: 'ERR_ACA_04', message: 'se cruza');

      await expectLater(
        container.read(horariosProvider.notifier).crear(request),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_04')),
      );

      expect(servicio.llamadas, ['crearPlantilla']);
    });
  });

  group('franjasListProvider', () {
    test('pide las franjas de la fecha del parámetro, solo con la fecha', () async {
      servicio.franjas = [franjaDePrueba()];
      final container = crear();

      final lista = await container.read(franjasListProvider(DateTime(2026, 11, 2, 15, 30)).future);

      expect(servicio.ultimaFecha, DateTime(2026, 11, 2));
      expect(lista.single.codigo, 'f1');
    });

    test('la fecha por defecto es hoy, sin hora', () {
      final fecha = crear().read(franjasFechaUiProvider);
      final hoy = DateTime.now();

      expect(fecha, DateTime(hoy.year, hoy.month, hoy.day));
    });

    test('fijar la fecha descarta la hora', () {
      final container = crear();

      container.read(franjasFechaUiProvider.notifier).fijar(DateTime(2026, 11, 9, 22, 10));

      expect(container.read(franjasFechaUiProvider), DateTime(2026, 11, 9));
    });
  });

  group('franjasProvider', () {
    test('bloquear invalida la lista de franjas', () async {
      final container = crear();
      final fecha = DateTime(2026, 11, 2);
      container.listen(franjasListProvider(fecha), (_, _) {});
      await container.read(franjasListProvider(fecha).future);
      servicio.llamadas.clear();

      final franja = await container.read(franjasProvider.notifier).bloquear('f1');
      await container.read(franjasListProvider(fecha).future);

      expect(franja.codigo, 'f1');
      expect(servicio.llamadas, ['bloquearFranja:f1', 'listarFranjas']);
    });

    test('desbloquear invalida la lista de franjas', () async {
      final container = crear();
      final fecha = DateTime(2026, 11, 2);
      container.listen(franjasListProvider(fecha), (_, _) {});
      await container.read(franjasListProvider(fecha).future);
      servicio.llamadas.clear();

      await container.read(franjasProvider.notifier).desbloquear('f1');
      await container.read(franjasListProvider(fecha).future);

      expect(servicio.llamadas, ['desbloquearFranja:f1', 'listarFranjas']);
    });

    test('si bloquear falla, el error se relanza y no se recarga la lista', () async {
      final container = crear();
      final fecha = DateTime(2026, 11, 2);
      container.listen(franjasListProvider(fecha), (_, _) {});
      await container.read(franjasListProvider(fecha).future);
      servicio.llamadas.clear();
      servicio.error = ApiException(code: 'ERR_ACA_08', message: 'no está libre');

      await expectLater(
        container.read(franjasProvider.notifier).bloquear('f1'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_08')),
      );

      expect(servicio.llamadas, ['bloquearFranja:f1']);
    });
  });
}
