import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/horarios/models/crear_plantilla_horario_request.dart';
import 'package:mobile/features/horarios/models/estado_plantilla.dart';
import 'package:mobile/features/horarios/providers/franjas_provider.dart';
import 'package:mobile/features/horarios/providers/horarios_provider.dart';

import '../../helpers/horarios_falsos.dart';

void main() {
  late HorariosServiceFalso servicio;

  const request = CrearPlantillaHorarioRequest(
    nombre: 'Mañanas',
    diasSemana: [1, 3],
    horaInicio: '08:00',
    horaFin: '12:00',
    duracionClaseMin: 60,
  );

  // * Una semana cualquiera, a modo de clave de la lista de franjas
  final semana = DateTime(2026, 11, 2);

  ProviderContainer crear() {
    final container = ProviderContainer(overrides: [horariosServiceFalso(servicio)]);
    addTearDown(container.dispose);
    return container;
  }

  /// Mantiene vivas las dos listas y las carga, para ver después que se vuelven a pedir al invalidarse.
  Future<void> cargarListas(ProviderContainer container) async {
    container.listen(horariosListProvider, (_, _) {});
    container.listen(franjasListProvider(semana), (_, _) {});
    await container.read(horariosListProvider.future);
    await container.read(franjasListProvider(semana).future);
    servicio.llamadas.clear();
  }

  setUp(() => servicio = HorariosServiceFalso());

  group('horariosListProvider', () {
    test('por defecto pide los activos y los pausados', () async {
      servicio.horarios = [horarioDePrueba()];
      final container = crear();

      final lista = await container.read(horariosListProvider.future);

      expect(servicio.ultimosEstados, {EstadoPlantilla.activa, EstadoPlantilla.pausada});
      expect(lista.single.codigo, 'h1');
    });

    test('al incluir inactivos pide también ese estado', () async {
      servicio.horarios = [horarioDePrueba(), horarioDePrueba(codigo: 'h2', estado: EstadoPlantilla.inactiva)];
      final container = crear();
      container.read(horariosIncluirInactivosUiProvider.notifier).fijar(true);

      final lista = await container.read(horariosListProvider.future);

      expect(servicio.ultimosEstados, {EstadoPlantilla.activa, EstadoPlantilla.pausada, EstadoPlantilla.inactiva});
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

  test('la pestaña inicial es Plantillas y se puede cambiar', () {
    final container = crear();

    expect(container.read(horariosPestanaUiProvider), PestanaHorarios.plantillas);
    container.read(horariosPestanaUiProvider.notifier).elegir(PestanaHorarios.franjas);
    expect(container.read(horariosPestanaUiProvider), PestanaHorarios.franjas);
  });

  group('horariosProvider', () {
    test('crear devuelve lo creado e invalida las listas de horarios y de franjas', () async {
      final container = crear();
      await cargarListas(container);

      final creada = await container.read(horariosProvider.notifier).crear(request);
      await container.read(horariosListProvider.future);
      await container.read(franjasListProvider(semana).future);

      expect(creada.franjasGeneradas, 14);
      expect(servicio.ultimoRequest, request);
      expect(servicio.llamadas, containsAllInOrder(['crearPlantilla', 'listarPlantillas', 'listarFranjas']));
    });

    test('pausar llama al servicio con el código e invalida las dos listas', () async {
      final container = crear();
      await cargarListas(container);

      await container.read(horariosProvider.notifier).pausar('h1');
      await container.read(horariosListProvider.future);
      await container.read(franjasListProvider(semana).future);

      expect(servicio.llamadas, containsAllInOrder(['pausarPlantilla:h1', 'listarPlantillas', 'listarFranjas']));
    });

    test('reanudar llama al servicio con el código e invalida las dos listas', () async {
      final container = crear();
      await cargarListas(container);

      await container.read(horariosProvider.notifier).reanudar('h1');
      await container.read(horariosListProvider.future);
      await container.read(franjasListProvider(semana).future);

      expect(servicio.llamadas, containsAllInOrder(['reanudarPlantilla:h1', 'listarPlantillas', 'listarFranjas']));
    });

    test('desactivar llama al servicio con el código e invalida las dos listas', () async {
      final container = crear();
      await cargarListas(container);

      await container.read(horariosProvider.notifier).desactivar('h1');
      await container.read(horariosListProvider.future);
      await container.read(franjasListProvider(semana).future);

      expect(servicio.llamadas, containsAllInOrder(['desactivarPlantilla:h1', 'listarPlantillas', 'listarFranjas']));
    });

    test('si crear falla, el error se relanza y no se invalida nada', () async {
      final container = crear();
      await cargarListas(container);
      servicio.error = ApiException(code: 'ERR_ACA_04', message: 'se cruza');

      await expectLater(
        container.read(horariosProvider.notifier).crear(request),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_04')),
      );

      expect(servicio.llamadas, ['crearPlantilla']);
    });

    test('si reanudar falla por un cruce, el error se relanza y no se recarga nada', () async {
      final container = crear();
      await cargarListas(container);
      servicio.error = ApiException(code: 'ERR_ACA_04', message: 'se cruza');

      await expectLater(
        container.read(horariosProvider.notifier).reanudar('h1'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_04')),
      );

      expect(servicio.llamadas, ['reanudarPlantilla:h1']);
    });
  });

  group('franjasSemanaUiProvider', () {
    test('la semana inicial es la de hoy, empezando en lunes y sin hora', () {
      final lunes = crear().read(franjasSemanaUiProvider);

      expect(lunes.weekday, DateTime.monday);
      expect(lunes.hour, 0);
      expect(DateTime.now().difference(lunes).inDays, inInclusiveRange(0, 6));
    });

    test('anterior y siguiente mueven de a siete días', () {
      final container = crear();
      final notifier = container.read(franjasSemanaUiProvider.notifier);
      notifier.irA(DateTime(2026, 11, 4));

      expect(container.read(franjasSemanaUiProvider), DateTime(2026, 11, 2));
      notifier.siguiente();
      expect(container.read(franjasSemanaUiProvider), DateTime(2026, 11, 9));
      notifier.anterior();
      notifier.anterior();
      expect(container.read(franjasSemanaUiProvider), DateTime(2026, 10, 26));
    });

    test('irA lleva a la semana que contiene la fecha, aunque sea domingo', () {
      final container = crear();

      container.read(franjasSemanaUiProvider.notifier).irA(DateTime(2026, 11, 8, 22, 10));

      expect(container.read(franjasSemanaUiProvider), DateTime(2026, 11, 2));
    });
  });

  group('franjasListProvider', () {
    test('pide la semana completa: del lunes al domingo', () async {
      servicio.franjas = [franjaDePrueba()];
      final container = crear();

      final lista = await container.read(franjasListProvider(DateTime(2026, 11, 4, 15, 30)).future);

      expect(servicio.ultimoDesde, DateTime(2026, 11, 2));
      expect(servicio.ultimoHasta, DateTime(2026, 11, 8));
      expect(lista.single.codigo, 'f1');
    });
  });

  group('franjasProvider', () {
    test('bloquear invalida la lista de franjas', () async {
      final container = crear();
      await cargarListas(container);

      final franja = await container.read(franjasProvider.notifier).bloquear('f1');
      await container.read(franjasListProvider(semana).future);

      expect(franja.codigo, 'f1');
      expect(servicio.llamadas, ['bloquearFranja:f1', 'listarFranjas']);
    });

    test('desbloquear invalida la lista de franjas', () async {
      final container = crear();
      await cargarListas(container);

      await container.read(franjasProvider.notifier).desbloquear('f1');
      await container.read(franjasListProvider(semana).future);

      expect(servicio.llamadas, ['desbloquearFranja:f1', 'listarFranjas']);
    });

    test('bloquearDia devuelve cuántas cambió, descarta la hora de la fecha e invalida la lista', () async {
      final container = crear();
      await cargarListas(container);
      servicio.franjasDelDia = 4;

      final dia = await container.read(franjasProvider.notifier).bloquearDia(DateTime(2026, 11, 4, 18, 45));
      await container.read(franjasListProvider(semana).future);

      expect(dia.franjasAfectadas, 4);
      expect(servicio.llamadas, ['bloquearDia:2026-11-04', 'listarFranjas']);
    });

    test('desbloquearDia invalida la lista de franjas', () async {
      final container = crear();
      await cargarListas(container);

      await container.read(franjasProvider.notifier).desbloquearDia(DateTime(2026, 11, 4));
      await container.read(franjasListProvider(semana).future);

      expect(servicio.llamadas, ['desbloquearDia:2026-11-04', 'listarFranjas']);
    });

    test('si bloquear falla, el error se relanza y no se recarga la lista', () async {
      final container = crear();
      await cargarListas(container);
      servicio.error = ApiException(code: 'ERR_ACA_08', message: 'no está libre');

      await expectLater(
        container.read(franjasProvider.notifier).bloquear('f1'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_08')),
      );

      expect(servicio.llamadas, ['bloquearFranja:f1']);
    });

    test('si bloquearDia falla por una fecha pasada, el error se relanza y no se recarga la lista', () async {
      final container = crear();
      await cargarListas(container);
      servicio.error = ApiException(code: 'ERR_ACA_10', message: 'El día ya pasó');

      await expectLater(
        container.read(franjasProvider.notifier).bloquearDia(DateTime(2020, 1, 1)),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_10')),
      );

      expect(servicio.llamadas, ['bloquearDia:2020-01-01']);
    });
  });
}
