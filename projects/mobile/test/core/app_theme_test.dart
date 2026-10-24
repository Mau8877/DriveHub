import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/shared/widgets/medidor_password.dart';

double _luminancia(Color c) {
  double canal(double v) =>
      v <= 0.03928 ? v / 12.92 : math.pow((v + 0.055) / 1.055, 2.4).toDouble();
  return 0.2126 * canal(c.r) + 0.7152 * canal(c.g) + 0.0722 * canal(c.b);
}

/// Relación de contraste WCAG entre dos colores opacos.
double _contraste(Color a, Color b) {
  final la = _luminancia(a);
  final lb = _luminancia(b);
  return (math.max(la, lb) + 0.05) / (math.min(la, lb) + 0.05);
}

void main() {
  // * Estos tests existen por la legibilidad: el primer mockup del Cockpit tenía texto ámbar y
  // * blanco sobre degradados y no se leía. Todo texto tiene que dar al menos 4.5:1 (WCAG AA).
  for (final (nombre, tema) in [
    ('claro', AppTheme.light),
    ('oscuro', AppTheme.dark),
  ]) {
    group('contraste en modo $nombre', () {
      final cs = tema.colorScheme;
      final colores = tema.extension<CockpitColores>()!;
      final fondo = tema.scaffoldBackgroundColor;
      // * El peor caso del fondo: el resplandor más fuerte sobre el color base
      final fondoConAzul = Color.alphaBlend(colores.brilloAzul, fondo);
      final fondoConAmbar = Color.alphaBlend(colores.brilloAmbar, fondo);

      void exige(String que, Color texto, Color sobre) {
        test(que, () {
          expect(_contraste(texto, sobre), greaterThanOrEqualTo(4.5));
        });
      }

      exige('texto principal sobre tarjeta', cs.onSurface, cs.surface);
      exige('texto principal sobre el fondo', cs.onSurface, fondo);
      exige('texto principal sobre el resplandor azul', cs.onSurface, fondoConAzul);
      exige('texto principal sobre el resplandor ámbar', cs.onSurface, fondoConAmbar);
      exige('texto secundario sobre tarjeta', cs.onSurfaceVariant, cs.surface);
      exige('texto secundario sobre el fondo', cs.onSurfaceVariant, fondo);
      exige('acento sobre tarjeta', colores.acento, cs.surface);
      exige('acento sobre el fondo', colores.acento, fondo);
      exige('enlace sobre tarjeta', colores.enlace, cs.surface);
      exige('enlace sobre el fondo', colores.enlace, fondo);
      exige('éxito sobre tarjeta', colores.exito, cs.surface);
      exige('error sobre tarjeta', cs.error, cs.surface);
      exige('texto del botón de acción', cs.onPrimary, cs.primary);
    });
  }

  group('fuerzaDePassword', () {
    test('vacía vale 0', () => expect(fuerzaDePassword(''), 0));
    test('solo minúsculas cortas vale 1', () {
      expect(fuerzaDePassword('abc'), 1);
    });
    test('larga, con mayúsculas, números y símbolo vale 4', () {
      expect(fuerzaDePassword('Secreta123!'), 4);
    });
    test('nunca pasa de 4', () {
      expect(fuerzaDePassword('Abcdefghi1!_ñ'), 4);
    });
  });
}
