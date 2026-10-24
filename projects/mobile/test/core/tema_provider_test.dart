import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/theme/tema_provider.dart';

class _Almacen implements AlmacenTema {
  ThemeMode? guardado;
  bool falla = false;

  @override
  Future<ThemeMode?> leer() async {
    if (falla) throw Exception('sin almacenamiento');
    return guardado;
  }

  @override
  Future<void> guardar(ThemeMode modo) async {
    if (falla) throw Exception('sin almacenamiento');
    guardado = modo;
  }
}

ProviderContainer _contenedor(_Almacen almacen) {
  final c = ProviderContainer(
    overrides: [almacenTemaProvider.overrideWithValue(almacen)],
  );
  addTearDown(c.dispose);
  return c;
}

void main() {
  test('sin nada guardado sigue al sistema', () async {
    final c = _contenedor(_Almacen());

    expect(c.read(temaProvider), ThemeMode.system);
    await Future<void>.delayed(Duration.zero);

    expect(c.read(temaProvider), ThemeMode.system);
  });

  test('recupera la apariencia guardada', () async {
    final c = _contenedor(_Almacen()..guardado = ThemeMode.dark);

    c.read(temaProvider);
    await Future<void>.delayed(Duration.zero);

    expect(c.read(temaProvider), ThemeMode.dark);
  });

  test('elegir una apariencia la aplica y la guarda', () async {
    final almacen = _Almacen();
    final c = _contenedor(almacen);

    await c.read(temaProvider.notifier).elegir(ThemeMode.light);

    expect(c.read(temaProvider), ThemeMode.light);
    expect(almacen.guardado, ThemeMode.light);
  });

  test('si el almacenamiento falla, la elección vale para la sesión', () async {
    final c = _contenedor(_Almacen()..falla = true);

    await c.read(temaProvider.notifier).elegir(ThemeMode.dark);

    expect(c.read(temaProvider), ThemeMode.dark);
  });
}
