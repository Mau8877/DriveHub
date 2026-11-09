import 'dart:async';

import 'dispositivo_push_storage.dart';
import 'dispositivos_service.dart';
import 'mensajeria_push.dart';

/// Registra este teléfono en Notificaciones para que reciba los avisos push (CU36), y lo quita al cerrar sesión.
///
/// * [activar] pide el permiso (una vez por ejecución de la app), obtiene el token de Firebase y lo registra; después
///   sigue los cambios de token. Con la sesión ya registrada para esa persona no hace nada, así que se puede llamar
///   cada vez que la sesión cambia.
/// * Si algo falla (sin permiso, sin Google Play Services, sin red, sesión sin token) no pasa nada: el push es un
///   extra y **nunca** bloquea la sesión ni la navegación. Un registro que falló se reintenta en el siguiente cambio
///   de sesión.
/// * El código del dispositivo se guarda para poder quitarlo con [desactivar]; el token de Firebase no se guarda ni se
///   escribe en ningún registro.
class RegistroDePush {
  final MensajeriaPush _mensajeria;
  final DispositivosService _dispositivos;
  final DispositivoPushStorage _storage;

  String? _usuarioRegistrado;
  bool _sinPermiso = false;
  Future<void>? _enCurso;
  StreamSubscription<String>? _suscripcionToken;

  RegistroDePush({
    required this._mensajeria,
    required this._dispositivos,
    required this._storage,
  });

  /// ¿Este teléfono ya está registrado para [usuarioCodigo]?
  bool estaRegistradoPara(String usuarioCodigo) => _usuarioRegistrado == usuarioCodigo;

  /// Registra el teléfono para la persona con sesión. Nunca lanza.
  Future<void> activar(String usuarioCodigo) {
    // * Una a la vez: dos cambios de sesión seguidos no registran dos veces
    final previa = _enCurso ?? Future<void>.value();
    final actual = previa.then((_) => _activar(usuarioCodigo));
    _enCurso = actual;
    return actual.whenComplete(() {
      if (identical(_enCurso, actual)) _enCurso = null;
    });
  }

  Future<void> _activar(String usuarioCodigo) async {
    if (_usuarioRegistrado == usuarioCodigo || _sinPermiso) return;
    try {
      if (!await _mensajeria.pedirPermiso()) {
        // * Si la persona dijo que no, no se vuelve a insistir hasta la próxima vez que se abra la app
        _sinPermiso = true;
        return;
      }
      final token = await _mensajeria.obtenerToken();
      if (token == null || token.isEmpty) return;
      await _registrar(token);
      _usuarioRegistrado = usuarioCodigo;
      _suscripcionToken ??= _mensajeria.tokenRenovado.listen((nuevo) => unawaited(_alRenovarToken(nuevo)), onError: (_) {});
    } catch (_) {
      // * Sin push no pasa nada: se reintenta en el próximo cambio de sesión
    }
  }

  Future<void> _registrar(String token) async {
    final codigo = await _dispositivos.registrar(token);
    await _storage.guardar(codigo);
  }

  Future<void> _alRenovarToken(String nuevo) async {
    if (_usuarioRegistrado == null) return;
    try {
      await _registrar(nuevo);
    } catch (_) {
      // * Se reintenta con el siguiente token o el siguiente inicio de sesión
    }
  }

  /// Quita el teléfono de la persona (cerrar sesión). Nunca lanza.
  ///
  /// * Tiene que llamarse ANTES de perder el token de sesión: quitar el dispositivo pide sesión.
  /// * Si no se pudo quitar (sin red), igual se olvida lo local: el token queda inactivo cuando Firebase lo rechace.
  Future<void> desactivar() async {
    try {
      await _enCurso;
    } catch (_) {}
    await _suscripcionToken?.cancel();
    _suscripcionToken = null;
    _usuarioRegistrado = null;
    try {
      final codigo = await _storage.leer();
      if (codigo != null) await _dispositivos.quitar(codigo);
    } catch (_) {
      // * Sin red o sesión vencida: se sigue
    }
    try {
      await _storage.borrar();
    } catch (_) {}
  }

  /// Deja de escuchar los cambios de token (al cerrarse el container).
  Future<void> liberar() async {
    await _suscripcionToken?.cancel();
    _suscripcionToken = null;
  }
}
