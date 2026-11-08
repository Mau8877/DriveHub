import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/offline/almacen_offline.dart';
import '../../../core/offline/modelos_offline.dart';
import '../../../core/offline/nucleo_offline.dart';

/// El núcleo offline de la app. Lo crea `main()` (necesita la sesión para saber si hay token) y lo entrega
/// con `overrideWithValue`.
///
/// * Sin override (los tests de pantallas) es un núcleo inerte: almacén en memoria, sin token y sin servidor.
final nucleoOfflineProvider = Provider<NucleoOffline>((ref) {
  final nucleo = NucleoOffline(
    almacen: AlmacenEnMemoria(),
    dio: () => ApiClient.instance,
    hayToken: () => false,
    sondeo: () async => false,
  );
  ref.onDispose(nucleo.dispose);
  return nucleo;
});

/// Estado del modo offline para la interfaz (sin conexión, pendientes, rechazadas).
class OfflineNotifier extends Notifier<EstadoOffline> {
  @override
  EstadoOffline build() {
    final nucleo = ref.watch(nucleoOfflineProvider);
    void alCambiar() => state = nucleo.estado.value;
    nucleo.estado.addListener(alCambiar);
    ref.onDispose(() => nucleo.estado.removeListener(alCambiar));
    return nucleo.estado.value;
  }

  /// La persona ya leyó qué se cumplió y qué no.
  Future<void> descartarResultados() => ref.read(nucleoOfflineProvider).descartarResultados();
}

final offlineProvider = NotifierProvider<OfflineNotifier, EstadoOffline>(OfflineNotifier.new);
