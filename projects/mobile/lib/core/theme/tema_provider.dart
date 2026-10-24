import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Dónde se guarda la apariencia elegida. Provider aparte para sustituirlo en los tests.
abstract class AlmacenTema {
  Future<ThemeMode?> leer();
  Future<void> guardar(ThemeMode modo);
}

/// Implementación sobre `flutter_secure_storage`, que la app ya usa para la sesión: no se suma
/// otra dependencia solo para guardar una preferencia.
class AlmacenTemaSeguro implements AlmacenTema {
  static const _clave = 'drivehub.tema';

  final FlutterSecureStorage _storage;

  AlmacenTemaSeguro({FlutterSecureStorage? storage})
    : _storage = storage ?? const FlutterSecureStorage();

  @override
  Future<ThemeMode?> leer() async {
    final texto = await _storage.read(key: _clave);
    for (final modo in ThemeMode.values) {
      if (modo.name == texto) return modo;
    }
    return null;
  }

  @override
  Future<void> guardar(ThemeMode modo) =>
      _storage.write(key: _clave, value: modo.name);
}

final almacenTemaProvider = Provider<AlmacenTema>((ref) => AlmacenTemaSeguro());

/// Apariencia de la app (Sistema, Claro u Oscuro). Arranca siguiendo al sistema y recupera lo
/// guardado apenas puede; elegir otra la guarda.
///
/// * Un error del almacenamiento nunca rompe la app: la apariencia es una preferencia, no un dato.
class TemaNotifier extends Notifier<ThemeMode> {
  @override
  ThemeMode build() {
    _restaurar();
    return ThemeMode.system;
  }

  Future<void> _restaurar() async {
    try {
      final guardado = await ref.read(almacenTemaProvider).leer();
      if (guardado != null) state = guardado;
    } catch (_) {
      // * Se queda con la apariencia del sistema
    }
  }

  Future<void> elegir(ThemeMode modo) async {
    state = modo;
    try {
      await ref.read(almacenTemaProvider).guardar(modo);
    } catch (_) {
      // * Vale para esta sesión aunque no se haya podido guardar
    }
  }
}

final temaProvider = NotifierProvider<TemaNotifier, ThemeMode>(
  TemaNotifier.new,
);
