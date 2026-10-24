import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../models/sesion_respuesta.dart';
import '../models/usuario_sesion.dart';
import '../services/auth_service.dart';
import '../services/sesion_storage.dart';

final authServiceProvider = Provider<AuthService>((ref) => AuthService());

enum FaseSesion { cargando, sinSesion, conSesion }

/// Estado de la sesión de la app.
///
/// * `motivo` es el código `ERR_IDE_05` a `ERR_IDE_09` cuando se perdió una
/// * sesión por un rechazo del backend (para explicarlo en el login).
class EstadoSesion {
  final FaseSesion fase;
  final UsuarioSesion? usuario;
  final String? motivo;

  const EstadoSesion._(this.fase, {this.usuario, this.motivo});
  const EstadoSesion.cargando() : this._(FaseSesion.cargando);
  const EstadoSesion.sinSesion({String? motivo})
    : this._(FaseSesion.sinSesion, motivo: motivo);
  const EstadoSesion.conSesion(UsuarioSesion usuario)
    : this._(FaseSesion.conSesion, usuario: usuario);

  bool get conSesionActiva => fase == FaseSesion.conSesion;
}

/// Único lugar que sabe si hay sesión: la inicia, la renueva (una sola
/// renovación a la vez, antes de que venza) y la recupera al abrir la app.
///
/// * El refresh token rota en cada renovación: siempre se guarda el nuevo.
class SesionNotifier extends Notifier<EstadoSesion> {
  /// Se renueva cuando pasó esta fracción de la vida del token de acceso.
  static const fraccionDeVidaParaRenovar = 0.8;
  static const _reintentoSinRed = Duration(seconds: 30);
  static const _motivosQueSeExplican = {
    'ERR_IDE_05',
    'ERR_IDE_06',
    'ERR_IDE_07',
    'ERR_IDE_08',
    'ERR_IDE_09',
  };

  String? _accessToken;
  String? _refreshToken;
  Timer? _temporizador;
  Future<String?>? _renovacionEnCurso;

  @override
  EstadoSesion build() {
    ref.onDispose(_cancelarTemporizador);
    return const EstadoSesion.cargando();
  }

  /// Token de acceso vigente (lo usa el interceptor de Dio).
  String? obtenerToken() => _accessToken;

  /// Al abrir la app: si hay sesión guardada, la renueva (sin pedir la contraseña).
  Future<void> restaurar() async {
    final guardada = await ref.read(sesionStorageProvider).leer();
    if (guardada == null) {
      state = const EstadoSesion.sinSesion();
      return;
    }
    _refreshToken = guardada.refreshToken;
    await renovar();
  }

  /// Inicia sesión. Un rechazo del backend se relanza como [ApiException].
  Future<void> iniciarSesion(String email, String password) async {
    final sesion = await ref.read(authServiceProvider).login(email, password);
    await _establecer(sesion);
  }

  /// Pide el correo de verificación (CU04b). Si el correo ya estaba verificado
  /// (`ERR_IDE_10`) renueva la sesión para actualizarla y no es un error; cualquier
  /// otro fallo se relanza para que la pantalla lo muestre.
  Future<void> verificarCorreo() async {
    try {
      await ref.read(authServiceProvider).solicitarVerificacionCorreo();
    } on ApiException catch (e) {
      if (e.code != 'ERR_IDE_10') rethrow;
      await renovar();
    }
  }

  /// Reemplaza en la sesión los datos del perfil que cambiaron (CU06) para que la barra y el
  /// avatar se actualicen al instante, sin tocar los tokens ni el temporizador. Solo cambia lo
  /// indicado: `fotoUrl` pone una foto nueva y `quitarFoto` la borra. Sin sesión no hace nada.
  Future<void> actualizarUsuario({
    String? nombres,
    String? apellidos,
    String? fotoUrl,
    bool quitarFoto = false,
  }) async {
    final actual = state.usuario;
    if (!state.conSesionActiva || actual == null) return;
    final nuevo = UsuarioSesion(
      codigo: actual.codigo,
      nombres: nombres ?? actual.nombres,
      apellidos: apellidos ?? actual.apellidos,
      roles: actual.roles,
      escuelaCodigo: actual.escuelaCodigo,
      correoVerificado: actual.correoVerificado,
      fotoUrl: quitarFoto ? null : (fotoUrl ?? actual.fotoUrl),
    );
    state = EstadoSesion.conSesion(nuevo);
    final refresh = _refreshToken;
    if (refresh != null) {
      await ref
          .read(sesionStorageProvider)
          .guardar(SesionGuardada(refreshToken: refresh, usuario: nuevo));
    }
  }

  /// Cierra la sesión (CU03): avisa al backend y borra todo lo local.
  ///
  /// * El borrado local no depende del backend: si falla (red, 429, 5xx) igual
  /// * se sale. El redirect global del router lleva al login.
  Future<void> cerrarSesion() async {
    final refresh = _refreshToken;
    if (refresh != null) {
      try {
        await ref.read(authServiceProvider).cerrar(refresh);
      } catch (_) {
        // * La sesión renovable vence sola en el servidor
      }
    }
    await _perderSesion(null);
  }

  /// Renueva la sesión; devuelve el token nuevo o `null` si no se pudo.
  Future<String?> renovar() {
    return _renovacionEnCurso ??= _renovar().whenComplete(
      () => _renovacionEnCurso = null,
    );
  }

  Future<String?> _renovar() async {
    final refresh = _refreshToken;
    if (refresh == null) {
      await _perderSesion(null);
      return null;
    }
    try {
      final sesion = await ref.read(authServiceProvider).renovar(refresh);
      await _establecer(sesion);
      return sesion.accessToken;
    } on ApiException catch (e) {
      await _perderSesion(e.code);
      return null;
    } catch (_) {
      // * Sin red u otro fallo que no es un rechazo del backend: la sesión no se pierde.
      if (state.fase == FaseSesion.cargando) {
        state = const EstadoSesion.sinSesion();
      } else {
        _programar(_reintentoSinRed);
      }
      return null;
    }
  }

  Future<void> _establecer(SesionRespuesta sesion) async {
    _accessToken = sesion.accessToken;
    _refreshToken = sesion.refreshToken;
    await ref
        .read(sesionStorageProvider)
        .guardar(
          SesionGuardada(
            refreshToken: sesion.refreshToken,
            usuario: sesion.usuario,
          ),
        );
    state = EstadoSesion.conSesion(sesion.usuario);
    _programar(
      Duration(
        milliseconds:
            (sesion.expiraEnSegundos * 1000 * fraccionDeVidaParaRenovar)
                .round(),
      ),
    );
  }

  Future<void> _perderSesion(String? codigo) async {
    _cancelarTemporizador();
    _accessToken = null;
    _refreshToken = null;
    await ref.read(sesionStorageProvider).borrar();
    state = EstadoSesion.sinSesion(
      motivo: _motivosQueSeExplican.contains(codigo) ? codigo : null,
    );
  }

  void _programar(Duration espera) {
    _cancelarTemporizador();
    _temporizador = Timer(espera, () => unawaited(renovar()));
  }

  void _cancelarTemporizador() {
    _temporizador?.cancel();
    _temporizador = null;
  }
}

final sesionProvider = NotifierProvider<SesionNotifier, EstadoSesion>(
  SesionNotifier.new,
);
