import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Dónde se guarda en el teléfono la dirección del backend que eligió la persona.
abstract class ServidorStorage {
  /// La dirección completa de la API (`http://192.168.1.10:8080/api/v1`), o `null` si nunca se eligió una.
  Future<String?> leer();
  Future<void> guardar(String direccionApi);
  Future<void> borrar();
}

/// Implementación sobre `flutter_secure_storage`, que la app ya usa para la sesión.
///
/// * La dirección no es un secreto, pero así no se suma otra dependencia de almacenamiento.
class ServidorStorageSegura implements ServidorStorage {
  static const _clave = 'drivehub.servidor';

  final FlutterSecureStorage _storage;

  ServidorStorageSegura({FlutterSecureStorage? storage}) : _storage = storage ?? const FlutterSecureStorage();

  @override
  Future<String?> leer() async {
    final valor = (await _storage.read(key: _clave))?.trim();
    return (valor == null || valor.isEmpty) ? null : valor;
  }

  @override
  Future<void> guardar(String direccionApi) => _storage.write(key: _clave, value: direccionApi);

  @override
  Future<void> borrar() => _storage.delete(key: _clave);
}

/// Provider aparte para poder sustituir el almacenamiento en los tests.
final servidorStorageProvider = Provider<ServidorStorage>((ref) => ServidorStorageSegura());
