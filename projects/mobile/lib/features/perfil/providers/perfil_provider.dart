import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/providers/sesion_provider.dart';
import '../models/cambiar_contrasena_request.dart';
import '../models/cambiar_correo_request.dart';
import '../models/editar_perfil_request.dart';
import '../models/perfil.dart';
import '../services/perfil_service.dart';

final perfilServiceProvider = Provider<PerfilService>((ref) => PerfilService());

/// Perfil de la cuenta propia (CU06) con sus mutaciones.
///
/// * Tras editar los datos o cambiar la foto, actualiza la sesión (`actualizarUsuario`) para que
/// * la barra y el avatar cambien al instante, sin pedir la sesión de nuevo.
/// * Un rechazo del backend ([ApiException]) se relanza para que la pantalla muestre su mensaje y
/// * NO deja el estado en error: el estado conserva el último perfil bueno.
class PerfilNotifier extends AsyncNotifier<Perfil> {
  @override
  Future<Perfil> build() {
    // * El perfil es de UNA persona: si cambia (cierra sesión y entra otra cuenta en la misma
    // * ejecución de la app) se descarta el anterior y se pide el nuevo. Solo se mira el código:
    // * editar los datos o la foto actualiza la sesión sin cambiar quién es.
    ref.watch(sesionProvider.select((s) => s.usuario?.codigo));
    return ref.read(perfilServiceProvider).obtener();
  }

  Future<void> editar(EditarPerfilRequest request) async {
    final perfil = await ref.read(perfilServiceProvider).editar(request);
    state = AsyncData(perfil);
    await _actualizarSesion(perfil);
  }

  Future<void> subirFoto(
    Uint8List bytes, {
    required String nombre,
    required String tipo,
  }) async {
    final perfil = await ref
        .read(perfilServiceProvider)
        .subirFoto(bytes, nombre: nombre, tipo: tipo);
    state = AsyncData(perfil);
    await _actualizarSesion(perfil);
  }

  Future<void> quitarFoto() async {
    final perfil = await ref.read(perfilServiceProvider).quitarFoto();
    state = AsyncData(perfil);
    await _actualizarSesion(perfil);
  }

  /// No cambia el perfil: las otras sesiones se cierran en el backend, esta se conserva.
  Future<void> cambiarContrasena(CambiarContrasenaRequest request) {
    return ref.read(perfilServiceProvider).cambiarContrasena(request);
  }

  /// No cambia el perfil: el correo solo cambia cuando se abre el enlace.
  Future<void> solicitarCambioCorreo(CambiarCorreoRequest request) {
    return ref.read(perfilServiceProvider).solicitarCambioCorreo(request);
  }

  /// Vuelve a pedir el perfil (por ejemplo al volver a la app tras abrir el enlace del correo).
  /// Si falla, conserva el perfil que ya había.
  Future<void> recargar() async {
    try {
      state = AsyncData(await ref.read(perfilServiceProvider).obtener());
    } catch (_) {
      // * Sin red o sesión vencida: se queda con lo que ya mostraba
    }
  }

  Future<void> _actualizarSesion(Perfil perfil) {
    return ref
        .read(sesionProvider.notifier)
        .actualizarUsuario(
          nombres: perfil.nombres,
          apellidos: perfil.apellidos,
          fotoUrl: perfil.fotoUrl,
          quitarFoto: perfil.fotoUrl == null,
        );
  }
}

final perfilProvider = AsyncNotifierProvider<PerfilNotifier, Perfil>(
  PerfilNotifier.new,
);

/// ¿El correo de la cuenta cambió respecto del que mostraba la pantalla? (CA18)
bool correoCambio(String correoAnterior, String correoActual) {
  return correoAnterior.trim().toLowerCase() !=
      correoActual.trim().toLowerCase();
}
