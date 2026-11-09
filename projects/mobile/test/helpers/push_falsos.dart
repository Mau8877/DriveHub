import 'dart:async';

import 'package:mobile/features/push/services/dispositivo_push_storage.dart';
import 'package:mobile/features/push/services/dispositivos_service.dart';
import 'package:mobile/features/push/services/mensajeria_push.dart';

/// Firebase controlado desde el test: ni toca Firebase ni el teléfono.
class MensajeriaFalsa implements MensajeriaPush {
  bool permiso = true;
  String? token = 'token-1';
  Object? errorAlPedirPermiso;
  int permisosPedidos = 0;
  int tokensPedidos = 0;

  final _tokens = StreamController<String>.broadcast();
  final _avisos = StreamController<AvisoPush>.broadcast();

  /// Firebase cambió el token del teléfono.
  void renovarToken(String nuevo) => _tokens.add(nuevo);

  /// Llegó un aviso con la app abierta.
  void llegaUnAviso(AvisoPush aviso) => _avisos.add(aviso);

  bool get hayAlguienSiguiendoElToken => _tokens.hasListener;

  @override
  Future<bool> pedirPermiso() async {
    permisosPedidos++;
    if (errorAlPedirPermiso != null) throw errorAlPedirPermiso!;
    return permiso;
  }

  @override
  Future<String?> obtenerToken() async {
    tokensPedidos++;
    return token;
  }

  @override
  Stream<String> get tokenRenovado => _tokens.stream;

  @override
  Stream<AvisoPush> get avisosEnPrimerPlano => _avisos.stream;
}

/// El registro de dispositivos controlado desde el test.
class DispositivosFalso extends DispositivosService {
  final List<String> registrados = [];
  final List<String> quitados = [];
  Object? errorAlRegistrar;
  Object? errorAlQuitar;
  Future<void> Function()? antesDeRegistrar;
  String codigo = 'dispositivo-1';

  @override
  Future<String> registrar(String tokenFcm) async {
    await antesDeRegistrar?.call();
    if (errorAlRegistrar != null) throw errorAlRegistrar!;
    registrados.add(tokenFcm);
    return codigo;
  }

  @override
  Future<void> quitar(String codigoDispositivoPush) async {
    if (errorAlQuitar != null) throw errorAlQuitar!;
    quitados.add(codigoDispositivoPush);
  }
}

class DispositivoStorageEnMemoria implements DispositivoPushStorage {
  String? codigo;

  @override
  Future<String?> leer() async => codigo;

  @override
  Future<void> guardar(String codigoDispositivoPush) async => codigo = codigoDispositivoPush;

  @override
  Future<void> borrar() async => codigo = null;
}
