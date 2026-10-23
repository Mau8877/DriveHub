import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/theme/app_theme.dart';

void main() {
  group('AppTheme', () {
    test(
      'en los dos modos el acento ámbar es secondary, con texto asfalto encima',
      () {
        for (final tema in [AppTheme.light, AppTheme.dark]) {
          expect(tema.colorScheme.secondary, AppColors.ambar);
          expect(tema.colorScheme.onSecondary, AppColors.asfalto);
        }
      },
    );

    test('el fondo y la superficie cambian entre claro y oscuro', () {
      expect(AppTheme.light.scaffoldBackgroundColor, AppColors.lightBackground);
      expect(AppTheme.dark.scaffoldBackgroundColor, AppColors.darkBackground);
      expect(AppTheme.light.colorScheme.surface, AppColors.blanco);
      expect(AppTheme.dark.colorScheme.surface, AppColors.asfalto);
    });

    test(
      'lleva los tokens semánticos y en oscuro la tarjeta no tiene sombra',
      () {
        expect(AppTheme.light.extension<AppSemantic>()!.cardShadow, isNotEmpty);
        expect(AppTheme.dark.extension<AppSemantic>()!.cardShadow, isEmpty);
      },
    );

    test('usa Nunito y conPeso cambia la fuente y el eje de peso a la vez', () {
      final estilo = AppTheme.conPeso(
        AppTheme.light.textTheme.bodyLarge!,
        FontWeight.w900,
      );

      expect(AppTheme.light.textTheme.bodyLarge!.fontFamily, 'Nunito');
      expect(estilo.fontWeight, FontWeight.w900);
      expect(estilo.fontVariations, [const FontVariation('wght', 900)]);
    });

    test('el botón relleno es ámbar y mide 56 dp', () {
      final estilo = AppTheme.light.filledButtonTheme.style!;

      expect(estilo.backgroundColor!.resolve({}), AppColors.ambar);
      expect(estilo.minimumSize!.resolve({})!.height, 56);
    });
  });

  testWidgets(
    'sin el tema de la app, semantica cae a los tokens del modo claro',
    (tester) async {
      late AppSemantic obtenido;
      await tester.pumpWidget(
        MaterialApp(
          home: Builder(
            builder: (context) {
              obtenido = context.semantica;
              return const SizedBox();
            },
          ),
        ),
      );

      expect(obtenido, same(AppSemantic.light));
    },
  );
}
