import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/offline/nucleo_offline.dart';
import '../../auth/providers/sesion_provider.dart';
import '../../clases/services/clases_service.dart';
import '../../clases/services/verificar_reserva_offline.dart';

/// Lleva la cuenta de los providers que guardan datos de la red (los que valen `AsyncValue`), para volver a
/// pedirlos cuando regresa internet: sin esto, una pantalla que se llenó con lo guardado seguiría mostrándolo.
///
/// * No distingue cuáles se llenaron con lo guardado: refresca todos los vivos. Es más barato que rastrear de qué
/// * respuesta salió cada uno, y solo ocurre al volver la conexión o al enviar pendientes.
class RegistroDeDatos extends ProviderObserver {
  final Set<ProviderBase<Object?>> _vivos = {};

  @override
  void didAddProvider(ProviderBase<Object?> provider, Object? value, ProviderContainer container) =>
      _registrar(provider, value);

  @override
  void didUpdateProvider(
    ProviderBase<Object?> provider,
    Object? previousValue,
    Object? newValue,
    ProviderContainer container,
  ) => _registrar(provider, newValue);

  @override
  void didDisposeProvider(ProviderBase<Object?> provider, ProviderContainer container) => _vivos.remove(provider);

  void _registrar(ProviderBase<Object?> provider, Object? valor) {
    if (valor is AsyncValue) _vivos.add(provider);
  }

  /// Vuelve a pedir los datos de todos los providers de red que están vivos.
  void refrescar(ProviderContainer container) {
    for (final provider in _vivos.toList()) {
      container.invalidate(provider);
    }
  }
}

/// Conecta el núcleo offline con la sesión, el estado de la red del sistema y los providers de datos.
/// La llama `main()` una vez, con el container ya creado.
///
/// * Al haber sesión (con o sin token) abre el espacio de esa persona; con token, envía lo pendiente.
/// * Cuando la red del sistema cambia: sin red se marca de inmediato; con red se sondea al servidor.
/// * Cuando vuelve la conexión o se envían pendientes, se refrescan los datos.
void conectarOffline(ProviderContainer container, NucleoOffline nucleo, RegistroDeDatos registro) {
  nucleo.alCambiarDatos = () => registro.refrescar(container);
  nucleo.verificadores[tipoReservarClases] = verificarReservaOffline;

  final sesion = container.read(sesionProvider.notifier);
  sesion.alCerrarSesion = nucleo.cerrarSesion;

  container.listen<EstadoSesion>(sesionProvider, (anterior, actual) {
    final usuario = actual.usuario;
    if (!actual.conSesionActiva || usuario == null) return;
    unawaited(() async {
      await nucleo.abrirSesion(usuario.codigo);
      await nucleo.sincronizar();
    }());
  }, fireImmediately: true);

  Connectivity().onConnectivityChanged.listen((redes) {
    if (redes.contains(ConnectivityResult.none) || redes.isEmpty) {
      nucleo.marcarSinConexion();
    } else {
      unawaited(nucleo.sondear());
    }
  });
}
