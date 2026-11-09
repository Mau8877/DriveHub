import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';

/// Un aviso push que llegó con la app abierta.
class AvisoPush {
  final String? titulo;
  final String? cuerpo;

  const AvisoPush({this.titulo, this.cuerpo});
}

/// Lo que la app necesita de Firebase Cloud Messaging; es una interfaz para que los tests no toquen Firebase.
abstract class MensajeriaPush {
  /// Pide el permiso de notificaciones (Android 13 o más). `true` si la persona lo concedió.
  Future<bool> pedirPermiso();

  /// El token de este teléfono, o `null` si Firebase no lo dio.
  Future<String?> obtenerToken();

  /// Firebase cambió el token de este teléfono: hay que registrar el nuevo.
  Stream<String> get tokenRenovado;

  /// Llegó un aviso con la app abierta. Con la app cerrada o en segundo plano lo muestra el sistema solo.
  Stream<AvisoPush> get avisosEnPrimerPlano;
}

/// La implementación real, sobre `firebase_messaging`.
///
/// * Firebase se inicia la primera vez que se usa (no al abrir la app): así un teléfono sin Google Play Services, o un
///   `google-services.json` que falte, solo deja sin push y no frena el arranque.
/// * La configuración sale de `android/app/google-services.json`; el `applicationId` tiene que coincidir con el suyo.
class MensajeriaFirebase implements MensajeriaPush {
  Future<FirebaseMessaging>? _instancia;

  Future<FirebaseMessaging> _mensajeria() => _instancia ??= () async {
    if (Firebase.apps.isEmpty) await Firebase.initializeApp();
    return FirebaseMessaging.instance;
  }();

  @override
  Future<bool> pedirPermiso() async {
    final ajustes = await (await _mensajeria()).requestPermission();
    return ajustes.authorizationStatus == AuthorizationStatus.authorized ||
        ajustes.authorizationStatus == AuthorizationStatus.provisional;
  }

  // * `getToken` y `onTokenRefresh` figuran como obsoletos en firebase_messaging 17 (el reemplazo, `register()` y
  // * `onRegistered`, entrega un ID de instalación para «envío directo»). Se mantienen a propósito: el backend manda con
  // * el Admin SDK a `tokens` de registro de FCM (`sendEachForMulticast`), que es lo que devuelven estos dos. Pasar al
  // * ID de instalación exige cambiar cómo envía el backend: ver las UT de CU36 móvil.
  @override
  // ignore: deprecated_member_use
  Future<String?> obtenerToken() async => (await _mensajeria()).getToken();

  @override
  Stream<String> get tokenRenovado async* {
    // ignore: deprecated_member_use
    yield* (await _mensajeria()).onTokenRefresh;
  }

  @override
  Stream<AvisoPush> get avisosEnPrimerPlano async* {
    await _mensajeria();
    yield* FirebaseMessaging.onMessage.map(
      (m) => AvisoPush(titulo: m.notification?.title, cuerpo: m.notification?.body),
    );
  }
}
