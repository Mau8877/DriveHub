import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../models/usuario_sesion.dart';

/// Lo que se guarda en el teléfono para recuperar la sesión al abrir la app.
///
/// * Solo el refresh token y los datos de la persona: el access token vive
/// * 15 minutos y se obtiene renovando al abrir, así no queda guardado.
class SesionGuardada {
  final String refreshToken;
  final UsuarioSesion usuario;

  const SesionGuardada({required this.refreshToken, required this.usuario});

  Map<String, dynamic> toJson() => {
    'refreshToken': refreshToken,
    'usuario': usuario.toJson(),
  };

  factory SesionGuardada.fromJson(Map<String, dynamic> json) {
    return SesionGuardada(
      refreshToken: json['refreshToken'] as String,
      usuario: UsuarioSesion.fromJson(json['usuario'] as Map<String, dynamic>),
    );
  }
}

abstract class SesionStorage {
  Future<SesionGuardada?> leer();
  Future<void> guardar(SesionGuardada sesion);
  Future<void> borrar();
}

/// Implementación sobre `flutter_secure_storage` (Keychain / Keystore).
class SesionStorageSegura implements SesionStorage {
  static const _clave = 'drivehub.sesion';

  final FlutterSecureStorage _storage;

  SesionStorageSegura({FlutterSecureStorage? storage})
    : _storage = storage ?? const FlutterSecureStorage();

  @override
  Future<SesionGuardada?> leer() async {
    final texto = await _storage.read(key: _clave);
    if (texto == null) return null;
    try {
      return SesionGuardada.fromJson(jsonDecode(texto) as Map<String, dynamic>);
    } catch (_) {
      // * Valor corrupto: se descarta para no romper el arranque
      await _storage.delete(key: _clave);
      return null;
    }
  }

  @override
  Future<void> guardar(SesionGuardada sesion) {
    return _storage.write(key: _clave, value: jsonEncode(sesion.toJson()));
  }

  @override
  Future<void> borrar() => _storage.delete(key: _clave);
}

/// Provider aparte para poder sustituir el almacenamiento en los tests.
final sesionStorageProvider = Provider<SesionStorage>(
  (ref) => SesionStorageSegura(),
);
