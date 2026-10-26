import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/clases/models/momento_dia.dart';
import 'package:mobile/features/clases/models/reservas_resultado.dart';
import 'package:mobile/features/clases/models/vista_reservas.dart';
import 'package:mobile/features/clases/providers/franjas_disponibles_provider.dart';
import 'package:mobile/features/clases/providers/franjas_seleccionadas_provider.dart';
import 'package:mobile/features/clases/providers/reservas_provider.dart';
import 'package:mobile/features/horarios/models/fechas.dart';
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

  group('filtros de Reservar', () {
    test('por defecto: hoy, todos los momentos y todos los instructores', () {
      final container = crear();

      expect(container.read(reservarFechaUiProvider), soloFecha(DateTime.now()));
      expect(container.read(reservarMomentoUiProvider), MomentoDia.todos);
      expect(container.read(reservarInstructorUiProvider), isNull);
    });

    test('la lista de franjas pide los filtros actuales', () async {
      servicio.franjas = [franjaDisponibleDePrueba()];
      servicio.instructores = [instructorDePrueba()];
      final container = crear();
      container.read(reservarFechaUiProvider.notifier).elegir(DateTime(2099, 11, 2, 15, 30));
      container.read(reservarMomentoUiProvider.notifier).elegir(MomentoDia.tarde);
      container.read(reservarInstructorUiProvider.notifier).elegir('i1');

      final franjas = await container.read(franjasDisponiblesListProvider.future);

      expect(franjas.single.codigo, 'f1');
      final pedido = servicio.pedidosDeFranjas.last;
      expect(pedido.fecha, DateTime(2099, 11, 2));
      expect(pedido.momento, MomentoDia.tarde);
      expect(pedido.instructor, 'i1');
    });

    test('al cambiar de día, un instructor que ese día no atiende deja de filtrar', () async {
      servicio.instructores = [instructorDePrueba()];
      final container = crear();
      container.listen(reservarInstructorEfectivoProvider, (_, _) {});
      container.read(reservarInstructorUiProvider.notifier).elegir('i1');
      await container.read(instructoresDisponiblesListProvider.future);
      expect(container.read(reservarInstructorEfectivoProvider), 'i1');

      servicio.instructores = [];
      container.read(reservarFechaUiProvider.notifier).elegir(DateTime(2099, 11, 3));
      await container.read(instructoresDisponiblesListProvider.future);

      expect(container.read(reservarInstructorEfectivoProvider), isNull);
    });

    test('las franjas que ya empezaron no se muestran', () async {
      servicio.franjas = [
        franjaDisponibleDePrueba(codigo: 'vieja', fecha: DateTime(2020, 1, 1)),
        franjaDisponibleDePrueba(codigo: 'futura'),
      ];
      final container = crear();

      final franjas = await container.read(franjasDisponiblesListProvider.future);

      expect(franjas.map((f) => f.codigo), ['futura']);
    });

    test('un error del service queda en el AsyncValue', () async {
      servicio.errorFranjas = ApiException(code: 'ERR_ACA_17', message: 'No');
      final container = crear();
      container.listen(franjasDisponiblesListProvider, (_, _) {});

      await expectLater(container.read(franjasDisponiblesListProvider.future), throwsA(isA<ApiException>()));
    });
  });

  group('selección de franjas', () {
    test('alternar elige y quita, en el orden en que se eligió', () {
      final container = crear();
      final notifier = container.read(franjasSeleccionadasUiProvider.notifier);
      final a = franjaDisponibleDePrueba(codigo: 'a');
      final b = franjaDisponibleDePrueba(codigo: 'b', horaInicio: '11:00');

      expect(notifier.alternar(a, horasDisponibles: 5), isTrue);
      expect(notifier.alternar(b, horasDisponibles: 5), isTrue);
      expect(container.read(franjasSeleccionadasUiProvider).keys, ['a', 'b']);

      expect(notifier.alternar(a, horasDisponibles: 5), isTrue);
      expect(container.read(franjasSeleccionadasUiProvider).keys, ['b']);
    });

    test('no deja elegir más franjas que horas disponibles', () {
      final container = crear();
      final notifier = container.read(franjasSeleccionadasUiProvider.notifier);

      expect(notifier.alternar(franjaDisponibleDePrueba(codigo: 'a'), horasDisponibles: 2), isTrue);
      expect(notifier.alternar(franjaDisponibleDePrueba(codigo: 'b'), horasDisponibles: 2), isTrue);
      expect(notifier.alternar(franjaDisponibleDePrueba(codigo: 'c'), horasDisponibles: 2), isFalse);
      expect(container.read(franjasSeleccionadasUiProvider), hasLength(2));
    });

    test('sin horas no deja elegir ninguna, pero sí quitar una elegida', () {
      final container = crear();
      final notifier = container.read(franjasSeleccionadasUiProvider.notifier);
      final a = franjaDisponibleDePrueba(codigo: 'a');
      notifier.alternar(a, horasDisponibles: 1);

      expect(notifier.alternar(franjaDisponibleDePrueba(codigo: 'b'), horasDisponibles: 0), isFalse);
      expect(notifier.alternar(a, horasDisponibles: 0), isTrue);
      expect(container.read(franjasSeleccionadasUiProvider), isEmpty);
    });

    test('nunca pasa del máximo por confirmación, aunque haya más horas', () {
      final container = crear();
      final notifier = container.read(franjasSeleccionadasUiProvider.notifier);

      for (var i = 0; i < maximoPorConfirmacion; i++) {
        expect(notifier.alternar(franjaDisponibleDePrueba(codigo: 'f$i'), horasDisponibles: 50), isTrue);
      }
      expect(notifier.alternar(franjaDisponibleDePrueba(codigo: 'extra'), horasDisponibles: 50), isFalse);
    });

    test('las elegidas sobreviven al cambio de día, de momento y de instructor', () {
      final container = crear();
      container.read(franjasSeleccionadasUiProvider.notifier).alternar(franjaDisponibleDePrueba(), horasDisponibles: 5);

      container.read(reservarFechaUiProvider.notifier).elegir(DateTime(2099, 12, 1));
      container.read(reservarMomentoUiProvider.notifier).elegir(MomentoDia.noche);
      container.read(reservarInstructorUiProvider.notifier).elegir('i9');

      expect(container.read(franjasSeleccionadasUiProvider).keys, ['f1']);
    });

    test('vaciar y quitar', () {
      final container = crear();
      final notifier = container.read(franjasSeleccionadasUiProvider.notifier);
      notifier.alternar(franjaDisponibleDePrueba(codigo: 'a'), horasDisponibles: 5);
      notifier.alternar(franjaDisponibleDePrueba(codigo: 'b'), horasDisponibles: 5);

      notifier.quitar('a');
      expect(container.read(franjasSeleccionadasUiProvider).keys, ['b']);
      notifier.quitar('no-esta');
      notifier.vaciar();
      expect(container.read(franjasSeleccionadasUiProvider), isEmpty);
    });
  });

  group('horasQueQuedan', () {
    test('baja una hora por franja elegida, sin pedir nada al servidor', () async {
      final container = crear();
      container.listen(horasQueQuedanProvider, (_, _) {});
      await container.read(saldoHorasProvider.future);
      pagos.pedidosDeSaldo = 0;

      expect(container.read(horasQueQuedanProvider).value, 5);
      container.read(franjasSeleccionadasUiProvider.notifier).alternar(franjaDisponibleDePrueba(codigo: 'a'), horasDisponibles: 5);
      container.read(franjasSeleccionadasUiProvider.notifier).alternar(franjaDisponibleDePrueba(codigo: 'b'), horasDisponibles: 5);

      expect(container.read(horasQueQuedanProvider).value, 3);
      expect(pagos.pedidosDeSaldo, 0);
    });

    test('nunca es negativo', () async {
      pagos.saldo = 1;
      final container = crear();
      container.listen(horasQueQuedanProvider, (_, _) {});
      await container.read(saldoHorasProvider.future);
      final notifier = container.read(franjasSeleccionadasUiProvider.notifier);
      notifier.alternar(franjaDisponibleDePrueba(codigo: 'a'), horasDisponibles: 1);

      // * Se fuerza una segunda elegida para comprobar el piso, aunque la pantalla no lo permita
      container.read(franjasSeleccionadasUiProvider.notifier).alternar(franjaDisponibleDePrueba(codigo: 'b'), horasDisponibles: 9);

      expect(container.read(horasQueQuedanProvider).value, 0);
    });
  });

  group('confirmar', () {
    test('manda los códigos elegidos, vacía la selección y refresca todo', () async {
      final container = crear();
      await cargarTodo(container);
      final seleccion = container.read(franjasSeleccionadasUiProvider.notifier);
      seleccion.alternar(franjaDisponibleDePrueba(codigo: 'a'), horasDisponibles: 5);
      seleccion.alternar(franjaDisponibleDePrueba(codigo: 'b'), horasDisponibles: 5);

      final resultado = await container.read(reservasProvider.notifier).confirmar();

      expect(servicio.reservasPedidas.single, ['a', 'b']);
      expect(resultado.reservadas, hasLength(2));
      expect(container.read(franjasSeleccionadasUiProvider), isEmpty);
      await container.read(franjasDisponiblesListProvider.future);
      await container.read(instructoresDisponiblesListProvider.future);
      await container.read(reservasListProvider(VistaReservas.proximas).future);
      await container.read(saldoHorasProvider.future);
      expect(servicio.pedidosDeFranjas, hasLength(1));
      expect(servicio.pedidosDeInstructores, hasLength(1));
      expect(servicio.pedidosDeReservas, [VistaReservas.proximas]);
      expect(pagos.pedidosDeSaldo, 1);
    });

    test('un resultado con rechazadas no es un error: igual vacía y refresca', () async {
      servicio.alReservar = (codigos) => ReservasResultado(
        reservadas: [reservaDePrueba(codigo: 'r-${codigos.first}')],
        rechazadas: [rechazadaDePrueba(franjaCodigo: codigos.last)],
        horasDisponibles: 4,
      );
      final container = crear();
      await cargarTodo(container);
      final seleccion = container.read(franjasSeleccionadasUiProvider.notifier);
      seleccion.alternar(franjaDisponibleDePrueba(codigo: 'a'), horasDisponibles: 5);
      seleccion.alternar(franjaDisponibleDePrueba(codigo: 'b'), horasDisponibles: 5);

      final resultado = await container.read(reservasProvider.notifier).confirmar();

      expect(resultado.rechazadas.single.franjaCodigo, 'b');
      expect(container.read(franjasSeleccionadasUiProvider), isEmpty);
    });

    test('con un error de red conserva la selección, pero refresca las clases y el saldo', () async {
      servicio.errorAlReservar = DioException(requestOptions: RequestOptions(path: '/reservas'));
      final container = crear();
      await cargarTodo(container);
      container.read(franjasSeleccionadasUiProvider.notifier).alternar(franjaDisponibleDePrueba(), horasDisponibles: 5);

      await expectLater(container.read(reservasProvider.notifier).confirmar(), throwsA(isA<DioException>()));

      expect(container.read(franjasSeleccionadasUiProvider).keys, ['f1']);
      await container.read(reservasListProvider(VistaReservas.proximas).future);
      await container.read(saldoHorasProvider.future);
      expect(servicio.pedidosDeReservas, [VistaReservas.proximas]);
      expect(pagos.pedidosDeSaldo, 1);
      // * las franjas no se refrescan: la selección sigue siendo válida para reintentar
      expect(servicio.pedidosDeFranjas, isEmpty);
    });

    test('un 400 del backend se propaga como ApiException y no vacía la selección', () async {
      servicio.errorAlReservar = ApiException(code: 'ERR_SYS_01', message: 'Elige al menos una clase');
      final container = crear();
      container.read(franjasSeleccionadasUiProvider.notifier).alternar(franjaDisponibleDePrueba(), horasDisponibles: 5);

      await expectLater(
        container.read(reservasProvider.notifier).confirmar(),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_SYS_01')),
      );
      expect(container.read(franjasSeleccionadasUiProvider), isNotEmpty);
    });
  });

  group('reservasListProvider', () {
    test('pide la vista de cada familia', () async {
      servicio.proximas = [reservaDePrueba(codigo: 'p')];
      servicio.historial = [reservaDePrueba(codigo: 'h')];
      final container = crear();

      final proximas = await container.read(reservasListProvider(VistaReservas.proximas).future);
      final historial = await container.read(reservasListProvider(VistaReservas.historial).future);

      expect(proximas.single.codigo, 'p');
      expect(historial.single.codigo, 'h');
    });
  });
}
