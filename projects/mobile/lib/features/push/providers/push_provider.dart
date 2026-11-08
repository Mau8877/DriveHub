import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/transporte.dart';
import '../../notificaciones/providers/transporte_provider.dart';
import '../services/dispositivo_push_storage.dart';
import '../services/dispositivos_service.dart';
import '../services/mensajeria_push.dart';
import '../services/registro_de_push.dart';

/// Firebase Cloud Messaging; un provider aparte para sustituirlo en los tests (ninguno toca Firebase).
final mensajeriaPushProvider = Provider<MensajeriaPush>((ref) => MensajeriaFirebase());

final dispositivoPushStorageProvider = Provider<DispositivoPushStorage>((ref) => DispositivoPushStorageSegura());

/// Registro del dispositivo: por GraphQL o REST según `NOTIFICACIONES_TRANSPORTE`, igual que los avisos.
final dispositivosServiceProvider = Provider<DispositivosService>(
  (ref) => ref.watch(transporteNotificacionesProvider) == Transporte.graphql
      ? DispositivosGraphqlService()
      : DispositivosService(),
);

/// Quien registra este teléfono al iniciar sesión y lo quita al cerrarla.
final registroDePushProvider = Provider<RegistroDePush>((ref) {
  final registro = RegistroDePush(
    mensajeria: ref.watch(mensajeriaPushProvider),
    dispositivos: ref.watch(dispositivosServiceProvider),
    storage: ref.watch(dispositivoPushStorageProvider),
  );
  ref.onDispose(registro.liberar);
  return registro;
});
