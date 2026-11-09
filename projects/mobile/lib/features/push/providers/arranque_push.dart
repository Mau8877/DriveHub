import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/providers/sesion_provider.dart';
import '../../notificaciones/providers/bandeja_providers.dart';
import '../services/mensajeria_push.dart';
import 'push_provider.dart';

/// Conecta los avisos push (CU36) con la sesión y con la bandeja. La llama `main()` una vez, con el container ya creado.
///
/// * Con sesión iniciada registra el teléfono; al cerrarla lo quita (antes de perder el token de sesión).
/// * Con la app abierta Android no muestra el aviso del sistema: se refrescan el contador y la bandeja y se muestra un
///   aviso breve. Con la app cerrada o en segundo plano lo muestra el sistema solo.
/// * Nada de esto bloquea ni rompe la app si Firebase no está disponible.
void conectarPush(ProviderContainer container, GlobalKey<ScaffoldMessengerState> mensajero) {
  final registro = container.read(registroDePushProvider);

  container.read(sesionProvider.notifier).antesDeCerrarSesion = registro.desactivar;

  // * Cada vez que la sesión cambia (incluida cada renovación) se intenta: si ya está registrado, no hace nada
  container.listen<EstadoSesion>(sesionProvider, (anterior, actual) {
    final usuario = actual.usuario;
    if (actual.conSesionActiva && usuario != null) unawaited(registro.activar(usuario.codigo));
  }, fireImmediately: true);

  container.read(mensajeriaPushProvider).avisosEnPrimerPlano.listen(
    (aviso) => _alLlegarUnAviso(container, mensajero, aviso),
    // * Sin Firebase (por ejemplo, sin Google Play Services) simplemente no hay avisos en vivo
    onError: (_) {},
  );
}

void _alLlegarUnAviso(ProviderContainer container, GlobalKey<ScaffoldMessengerState> mensajero, AvisoPush aviso) {
  container.invalidate(conteoNoLeidasProvider);
  container.invalidate(bandejaProvider);

  final texto = [aviso.titulo, aviso.cuerpo].whereType<String>().where((t) => t.trim().isNotEmpty).join(': ');
  if (texto.isEmpty) return;
  mensajero.currentState?.showSnackBar(SnackBar(content: Text(texto), duration: const Duration(seconds: 5)));
}
