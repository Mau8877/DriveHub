import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/shared/widgets/marca_drivehub.dart';

Future<void> _montar(WidgetTester tester, double ancho) {
  return tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light,
      home: Scaffold(
        body: Align(
          alignment: Alignment.topLeft,
          child: SizedBox(width: ancho, child: const LogoDriveHub(tamano: 34)),
        ),
      ),
    ),
  );
}

void main() {
  testWidgets('con ancho de sobra muestra el nombre completo', (tester) async {
    await _montar(tester, 300);

    expect(tester.takeException(), isNull);
    expect(find.textContaining('Drive', findRichText: true), findsOneWidget);
  });

  testWidgets('en un ancho muy chico no desborda la fila', (tester) async {
    await _montar(tester, 100);

    // * Antes: "A RenderFlex overflowed by 12 pixels on the right"
    expect(tester.takeException(), isNull);
  });
}
