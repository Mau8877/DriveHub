import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Dónde se guarda el código del dispositivo push registrado, para poder quitarlo al cerrar sesión.
abstract class DispositivoPushStorage {
  Future<String?> leer();
  Future<void> guardar(String codigoDispositivoPush);
  Future<void> borrar();
}

/// Implementación sobre `flutter_secure_storage`, que la app ya usa para la sesión.
///
/// * Solo se guarda el código del dispositivo (un UUID del servicio), nunca el token de Firebase.
class DispositivoPushStorageSegura implements DispositivoPushStorage {
  static const _clave = 'drivehub.push.dispositivo';

  final FlutterSecureStorage _storage;

  DispositivoPushStorageSegura({FlutterSecureStorage? storage}) : _storage = storage ?? const FlutterSecureStorage();

  @override
  Future<String?> leer() async {
    final valor = (await _storage.read(key: _clave))?.trim();
    return (valor == null || valor.isEmpty) ? null : valor;
  }

  @override
  Future<void> guardar(String codigoDispositivoPush) => _storage.write(key: _clave, value: codigoDispositivoPush);

  @override
  Future<void> borrar() => _storage.delete(key: _clave);
}
