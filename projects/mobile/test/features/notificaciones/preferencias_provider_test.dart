import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/notificaciones/models/preferencias_notificacion.dart';
import 'package:mobile/features/notificaciones/providers/notificaciones_providers.dart';
import 'package:mobile/features/perfil/providers/perfil_provider.dart';

import '../../helpers/preferencias_falsas.dart';
import '../../helpers/sesion_falsa.dart';

ProviderContainer _contenedor(ServicioPreferenciasFalso servicio) {
  final contenedor = ProviderContainer(
    overrides: [
      sesionFalsa(SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba))),
      perfilServiceProvider.overrideWithValue(PerfilFalso()),
      preferenciasServiceProvider.overrideWithValue(servicio),
    ],
  );
  addTearDown(contenedor.dispose);
  return contenedor;
}

void main() {
  test('carga las preferencias guardadas', () async {
    final servicio = ServicioPreferenciasFalso()
      ..guardadas = PreferenciasNotificacion.porDefecto().conCanal(push: false);
    final contenedor = _contenedor(servicio);

    final p = await contenedor.read(preferenciasNotificacionProvider.future);

    expect(p.push, isFalse);
    expect(servicio.obtenciones, 1);
  });

  test(
    'guardar envía la configuración con el correo del perfil y deja lo que respondió el servicio',
    () async {
      final servicio = ServicioPreferenciasFalso();
      final contenedor = _contenedor(servicio);
      await contenedor.read(preferenciasNotificacionProvider.future);

      final nuevas = PreferenciasNotificacion.porDefecto().conTipo(
        TipoNotificacion.pago,
        false,
      );
      await contenedor
          .read(preferenciasNotificacionProvider.notifier)
          .guardar(nuevas);

      expect(servicio.enviadas, nuevas);
      expect(servicio.correoEnviado, 'ana@gmail.com');
      expect(contenedor.read(preferenciasNotificacionProvider).value, nuevas);
    },
  );

  test('si guardar falla relanza el error y conserva lo guardado', () async {
    final servicio = ServicioPreferenciasFalso()
      ..errorAlGuardar = ApiException(
        code: 'ERR_NOT_03',
        message: 'Franja no válida',
      );
    final contenedor = _contenedor(servicio);
    final original = await contenedor.read(
      preferenciasNotificacionProvider.future,
    );

    await expectLater(
      contenedor
          .read(preferenciasNotificacionProvider.notifier)
          .guardar(original.conSilencio(desde: '10:00', hasta: '10:00')),
      throwsA(isA<ApiException>()),
    );

    final estado = contenedor.read(preferenciasNotificacionProvider);
    expect(estado.hasError, isFalse);
    expect(estado.value, original);
  });

  test('si la carga falla, el estado queda en error', () async {
    final servicio = ServicioPreferenciasFalso()
      ..errorAlObtener = Exception('sin red');
    final contenedor = _contenedor(servicio);

    await expectLater(
      contenedor.read(preferenciasNotificacionProvider.future),
      throwsException,
    );
    expect(contenedor.read(preferenciasNotificacionProvider).hasError, isTrue);
  });

  group('borrador', () {
    test('sin tocar nada no hay borrador ni cambios', () {
      final contenedor = _contenedor(ServicioPreferenciasFalso());
      final guardadas = PreferenciasNotificacion.porDefecto();

      expect(contenedor.read(preferenciasFormUiProvider), isNull);
      expect(
        hayCambiosEnPreferencias(
          guardadas,
          contenedor.read(preferenciasFormUiProvider),
        ),
        isFalse,
      );
    });

    test('editar parte de lo guardado y acumula los cambios', () {
      final contenedor = _contenedor(ServicioPreferenciasFalso());
      final guardadas = PreferenciasNotificacion.porDefecto();
      final form = contenedor.read(preferenciasFormUiProvider.notifier);

      form.editar(guardadas, (p) => p.conCanal(push: false));
      form.editar(guardadas, (p) => p.conTipo(TipoNotificacion.pago, false));

      final borrador = contenedor.read(preferenciasFormUiProvider)!;
      expect(borrador.push, isFalse);
      expect(borrador.tipos[TipoNotificacion.pago], isFalse);
      expect(hayCambiosEnPreferencias(guardadas, borrador), isTrue);
    });

    test(
      'volver a lo guardado deja de contar como cambio, y descartar limpia el borrador',
      () {
        final contenedor = _contenedor(ServicioPreferenciasFalso());
        final guardadas = PreferenciasNotificacion.porDefecto();
        final form = contenedor.read(preferenciasFormUiProvider.notifier);

        form.editar(guardadas, (p) => p.conCanal(push: false));
        form.editar(guardadas, (p) => p.conCanal(push: true));
        expect(
          hayCambiosEnPreferencias(
            guardadas,
            contenedor.read(preferenciasFormUiProvider),
          ),
          isFalse,
        );

        form.editar(guardadas, (p) => p.conCanal(correo: false));
        form.descartar();
        expect(contenedor.read(preferenciasFormUiProvider), isNull);
      },
    );
  });
}
