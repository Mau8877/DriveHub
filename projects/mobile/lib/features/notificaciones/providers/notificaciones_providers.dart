import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/providers/sesion_provider.dart';
import '../../perfil/providers/perfil_provider.dart';
import '../models/preferencias_notificacion.dart';
import '../services/preferencias_service.dart';

/// Servicio de las preferencias; es un provider aparte para poder sustituirlo en tests.
final preferenciasServiceProvider = Provider<PreferenciasService>(
  (ref) => PreferenciasService(),
);

/// Las preferencias guardadas de la persona (CU37), con la acción de guardar.
///
/// * Son de UNA persona: si cambia (cierra sesión y entra otra cuenta en la misma ejecución de la app)
///   se descartan y se piden las nuevas.
/// * Un rechazo del backend ([ApiException]) o un fallo de red al guardar se relanza para que la pantalla
///   lo muestre, y NO deja el estado en error: conserva lo último que estaba guardado.
class PreferenciasNotificacionNotifier
    extends AsyncNotifier<PreferenciasNotificacion> {
  @override
  Future<PreferenciasNotificacion> build() {
    ref.watch(sesionProvider.select((s) => s.usuario?.codigo));
    return ref.read(preferenciasServiceProvider).obtener();
  }

  /// Guarda la configuración completa con el correo de la cuenta (el del perfil, CU06) y deja como
  /// guardado lo que respondió el servicio.
  Future<void> guardar(PreferenciasNotificacion preferencias) async {
    final perfil = await ref.read(perfilProvider.future);
    final guardadas = await ref
        .read(preferenciasServiceProvider)
        .guardar(preferencias, email: perfil.email);
    state = AsyncData(guardadas);
  }
}

final preferenciasNotificacionProvider =
    AsyncNotifierProvider<
      PreferenciasNotificacionNotifier,
      PreferenciasNotificacion
    >(PreferenciasNotificacionNotifier.new);

/// El borrador de la pantalla: lo que la persona fue tocando y todavía no guardó. `null` = no tocó nada.
class PreferenciasFormUiNotifier extends Notifier<PreferenciasNotificacion?> {
  @override
  PreferenciasNotificacion? build() => null;

  /// Aplica un cambio sobre el borrador, o sobre lo guardado si todavía no hay borrador.
  void editar(
    PreferenciasNotificacion guardadas,
    PreferenciasNotificacion Function(PreferenciasNotificacion) cambio,
  ) => state = cambio(state ?? guardadas);

  void descartar() => state = null;
}

final preferenciasFormUiProvider =
    NotifierProvider<PreferenciasFormUiNotifier, PreferenciasNotificacion?>(
      PreferenciasFormUiNotifier.new,
    );

/// Si el borrador difiere de lo guardado: «Guardar» solo se habilita entonces.
bool hayCambiosEnPreferencias(
  PreferenciasNotificacion guardadas,
  PreferenciasNotificacion? borrador,
) => borrador != null && borrador != guardadas;
