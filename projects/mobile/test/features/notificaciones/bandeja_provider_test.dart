import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/notificaciones/models/preferencias_notificacion.dart';
import 'package:mobile/features/notificaciones/providers/bandeja_providers.dart';

import '../../helpers/notificaciones_falsas.dart';
import '../../helpers/sesion_falsa.dart';

ProviderContainer _contenedor(NotificacionesServiceFalso servicio) {
  final contenedor = ProviderContainer(
    overrides: [
      sesionFalsa(SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba))),
      notificacionesServiceFalso(servicio),
    ],
  );
  addTearDown(contenedor.dispose);
  return contenedor;
}

/// Mantiene vivos los providers autoDispose mientras dura el test.
void _escuchar(ProviderContainer c) {
  c.listen(bandejaProvider, (_, _) {});
  c.listen(bandejaFiltroUiProvider, (_, _) {});
  c.listen(conteoNoLeidasProvider, (_, _) {});
}

void main() {
  group('bandejaProvider', () {
    test('carga la primera página de 20 y sabe que hay más', () async {
      final servicio = NotificacionesServiceFalso()
        ..avisos = [for (var i = 1; i <= 45; i++) avisoDePrueba(i)];
      final c = _contenedor(servicio);
      _escuchar(c);

      final bandeja = await c.read(bandejaProvider.future);

      expect(bandeja.items, hasLength(20));
      expect(bandeja.hayMas, isTrue);
      expect(servicio.listados.single.page, 1);
    });

    test('cargarMas agrega la página siguiente y apaga hayMas en la última', () async {
      final servicio = NotificacionesServiceFalso()
        ..avisos = [for (var i = 1; i <= 45; i++) avisoDePrueba(i)];
      final c = _contenedor(servicio);
      _escuchar(c);
      await c.read(bandejaProvider.future);

      await c.read(bandejaProvider.notifier).cargarMas();
      await c.read(bandejaProvider.notifier).cargarMas();
      await c.read(bandejaProvider.notifier).cargarMas();

      final bandeja = c.read(bandejaProvider).requireValue;
      expect(bandeja.items, hasLength(45));
      expect(bandeja.hayMas, isFalse);
      expect(servicio.listados.map((l) => l.page), [1, 2, 3]);
    });

    test('un fallo en una página posterior conserva lo cargado y no deja el estado en error', () async {
      final servicio = NotificacionesServiceFalso()
        ..avisos = [for (var i = 1; i <= 45; i++) avisoDePrueba(i)]
        ..errorListado = Exception('sin red')
        ..errorListadoSoloDesdeLaPagina2 = true;
      final c = _contenedor(servicio);
      _escuchar(c);
      await c.read(bandejaProvider.future);

      await c.read(bandejaProvider.notifier).cargarMas();

      final estado = c.read(bandejaProvider);
      expect(estado.hasError, isFalse);
      expect(estado.requireValue.items, hasLength(20));
      expect(estado.requireValue.errorAlCargarMas, isNotNull);
      expect(estado.requireValue.cargandoMas, isFalse);
    });

    test('cambiar el filtro vuelve a la primera página con el filtro nuevo', () async {
      final servicio = NotificacionesServiceFalso()
        ..avisos = [
          avisoDePrueba(1, leida: true),
          avisoDePrueba(2),
          avisoDePrueba(3, tipo: TipoNotificacion.examen),
        ];
      final c = _contenedor(servicio);
      _escuchar(c);
      await c.read(bandejaProvider.future);

      c.read(bandejaFiltroUiProvider.notifier).alternarNoLeidas();
      var bandeja = await c.read(bandejaProvider.future);
      expect(bandeja.items.map((a) => a.codigoNotificacion), ['n2', 'n3']);
      expect(servicio.listados.last.soloNoLeidas, isTrue);

      // * «No leídas» se combina con un tipo
      c.read(bandejaFiltroUiProvider.notifier).elegirTipo(TipoNotificacion.examen);
      bandeja = await c.read(bandejaProvider.future);
      expect(bandeja.items.map((a) => a.codigoNotificacion), ['n3']);
      expect(servicio.listados.last.tipo, TipoNotificacion.examen);
      expect(servicio.listados.last.page, 1);

      c.read(bandejaFiltroUiProvider.notifier).todas();
      bandeja = await c.read(bandejaProvider.future);
      expect(bandeja.items, hasLength(3));
    });

    test('elegir el mismo tipo otra vez lo quita', () {
      final c = _contenedor(NotificacionesServiceFalso());
      _escuchar(c);
      final filtro = c.read(bandejaFiltroUiProvider.notifier);

      filtro.elegirTipo(TipoNotificacion.pago);
      expect(c.read(bandejaFiltroUiProvider).tipo, TipoNotificacion.pago);
      filtro.elegirTipo(TipoNotificacion.pago);
      expect(c.read(bandejaFiltroUiProvider).esTodas, isTrue);
    });

    test('un fallo al cargar deja la bandeja en error (la pantalla ofrece reintentar)', () async {
      final servicio = NotificacionesServiceFalso()..errorListado = Exception('sin red');
      final c = _contenedor(servicio);
      _escuchar(c);

      await expectLater(c.read(bandejaProvider.future), throwsException);
    });

    group('marcarLeida', () {
      test('deja el aviso leído en la lista enseguida y baja el contador', () async {
        final servicio = NotificacionesServiceFalso()
          ..avisos = [avisoDePrueba(1), avisoDePrueba(2), avisoDePrueba(3)];
        final c = _contenedor(servicio);
        _escuchar(c);
        await c.read(bandejaProvider.future);
        expect(await c.read(conteoNoLeidasProvider.future), 3);

        await c.read(bandejaProvider.notifier).marcarLeida('n2');

        final items = c.read(bandejaProvider).requireValue.items;
        expect(items.firstWhere((a) => a.codigoNotificacion == 'n2').leida, isTrue);
        expect(items.where((a) => !a.leida), hasLength(2));
        expect(await c.read(conteoNoLeidasProvider.future), 2);
        expect(servicio.leidos, ['n2']);
      });

      test('uno que ya estaba leído no llama al servicio', () async {
        final servicio = NotificacionesServiceFalso()..avisos = [avisoDePrueba(1, leida: true)];
        final c = _contenedor(servicio);
        _escuchar(c);
        await c.read(bandejaProvider.future);

        await c.read(bandejaProvider.notifier).marcarLeida('n1');

        expect(servicio.leidos, isEmpty);
      });

      test('si falla por la red lo deja sin leer (se reintenta al abrirlo) y relanza el error', () async {
        final servicio = NotificacionesServiceFalso()
          ..avisos = [avisoDePrueba(1)]
          ..errorAlLeer = Exception('sin red');
        final c = _contenedor(servicio);
        _escuchar(c);
        await c.read(bandejaProvider.future);

        await expectLater(c.read(bandejaProvider.notifier).marcarLeida('n1'), throwsException);

        expect(c.read(bandejaProvider).requireValue.items.single.leida, isFalse);
      });

      test('con ERR_NOT_04 quita el aviso de la lista y relanza el error', () async {
        final servicio = NotificacionesServiceFalso()
          ..avisos = [avisoDePrueba(1), avisoDePrueba(2)]
          ..errorAlLeer = ApiException(code: 'ERR_NOT_04', message: 'El aviso no existe');
        final c = _contenedor(servicio);
        _escuchar(c);
        await c.read(bandejaProvider.future);

        await expectLater(
          c.read(bandejaProvider.notifier).marcarLeida('n1'),
          throwsA(isA<ApiException>()),
        );

        expect(
          c.read(bandejaProvider).requireValue.items.map((a) => a.codigoNotificacion),
          ['n2'],
        );
      });
    });

    group('marcarTodas', () {
      test('deja todo leído, devuelve cuántas eran y el contador en cero', () async {
        final servicio = NotificacionesServiceFalso()
          ..avisos = [avisoDePrueba(1), avisoDePrueba(2), avisoDePrueba(3, leida: true)];
        final c = _contenedor(servicio);
        _escuchar(c);
        await c.read(bandejaProvider.future);
        expect(await c.read(conteoNoLeidasProvider.future), 2);

        final marcadas = await c.read(bandejaProvider.notifier).marcarTodas();

        expect(marcadas, 2);
        expect(c.read(bandejaProvider).requireValue.items.every((a) => a.leida), isTrue);
        expect(await c.read(conteoNoLeidasProvider.future), 0);
      });

      test('si falla relanza el error y no cambia la lista', () async {
        final servicio = NotificacionesServiceFalso()
          ..avisos = [avisoDePrueba(1)]
          ..errorAlLeerTodas = Exception('sin red');
        final c = _contenedor(servicio);
        _escuchar(c);
        await c.read(bandejaProvider.future);

        await expectLater(c.read(bandejaProvider.notifier).marcarTodas(), throwsException);

        expect(c.read(bandejaProvider).requireValue.items.single.leida, isFalse);
      });
    });
  });

  group('conteoNoLeidasProvider', () {
    test('un fallo del servicio es null (sin insignia), no un error', () async {
      final servicio = NotificacionesServiceFalso()..errorConteo = Exception('sin red');
      final c = _contenedor(servicio);
      _escuchar(c);

      expect(await c.read(conteoNoLeidasProvider.future), isNull);
      expect(c.read(conteoNoLeidasProvider).hasError, isFalse);
    });

    test('sin sesión no pide nada al servicio', () async {
      final servicio = NotificacionesServiceFalso();
      final c = ProviderContainer(
        overrides: [
          sesionFalsa(SesionFalsa(const EstadoSesion.sinSesion())),
          notificacionesServiceFalso(servicio),
        ],
      );
      addTearDown(c.dispose);
      c.listen(conteoNoLeidasProvider, (_, _) {});

      expect(await c.read(conteoNoLeidasProvider.future), isNull);
      expect(servicio.conteos, 0);
    });

    test('al invalidarlo vuelve a pedir el contador', () async {
      final servicio = NotificacionesServiceFalso()..avisos = [avisoDePrueba(1)];
      final c = _contenedor(servicio);
      _escuchar(c);
      expect(await c.read(conteoNoLeidasProvider.future), 1);

      servicio.avisos = [avisoDePrueba(1), avisoDePrueba(2)];
      c.invalidate(conteoNoLeidasProvider);

      expect(await c.read(conteoNoLeidasProvider.future), 2);
    });
  });
}
