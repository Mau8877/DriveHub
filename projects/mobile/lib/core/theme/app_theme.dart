import 'package:flutter/material.dart';

/// Paleta e identidad visual de la app (propuesta B «Cálida y amigable», ver
/// `docs/design.md`).
///
/// * Espejo manual de `packages/design-tokens/tokens.css` -- no hay forma de
/// * compartir el archivo de tokens entre CSS y Dart (ver WIDGETS_UI_MOBILE.md),
/// * así que todo cambio de paleta ahí se replica acá a mano en el mismo cambio.
/// * ! Ningún color literal en los widgets: todo sale de `AppColors` (vía el tema).
abstract final class AppColors {
  // Marca
  static const asfalto = Color(0xFF1E293B);
  static const ambar = Color(0xFFF59E0B);
  static const azul = Color(0xFF0284C7);
  static const grisClaro = Color(0xFFF1F5F9);
  static const blanco = Color(0xFFFFFFFF);
  static const grisMedio = Color(0xFF475569);

  // Claro
  static const lightBackground = grisClaro;
  static const lightSurface = blanco;
  static const lightOnSurface = asfalto;
  static const lightOnSurfaceVariant = grisMedio;
  static final lightOutline = grisMedio.withValues(alpha: .28);
  static const lightBlueText = Color(0xFF0369A1);
  static final lightBlueContainer = azul.withValues(alpha: .12);
  static const lightError = Color(0xFFB91C1C);
  static final lightErrorContainer = lightError.withValues(alpha: .10);
  static const lightSuccess = Color(0xFF15803D);
  static final lightSuccessContainer = lightSuccess.withValues(alpha: .12);
  static final lightNeutralContainer = asfalto.withValues(alpha: .10);
  static const lightSegmentActive = asfalto;

  // Oscuro
  static const darkBackground = Color(0xFF131B2A);
  static const darkSurface = asfalto;
  static const darkOnSurface = grisClaro;
  static const darkOnSurfaceVariant = Color(0xFFA3B1C6);
  static final darkOutline = grisClaro.withValues(alpha: .20);
  static const darkBlueText = Color(0xFF38BDF8);
  static final darkBlueContainer = darkBlueText.withValues(alpha: .16);
  static const darkError = Color(0xFFF87171);
  static final darkErrorContainer = darkError.withValues(alpha: .14);
  static const darkSuccess = Color(0xFF4ADE80);
  static final darkSuccessContainer = darkSuccess.withValues(alpha: .14);
  static final darkNeutralContainer = grisClaro.withValues(alpha: .14);
  static final darkSegmentActive = grisClaro.withValues(alpha: .20);

  // Compartidos
  static final amberContainer = ambar.withValues(alpha: .22);
}

abstract final class AppSpace {
  static const xs = 4.0, sm = 8.0, md = 12.0, lg = 16.0, xl = 20.0, xxl = 24.0;
}

abstract final class AppRadius {
  static const card = 28.0,
      button = 28.0,
      slot = 26.0,
      day = 24.0,
      tile = 22.0,
      textarea = 24.0,
      chip = 14.0;
}

abstract final class AppMotion {
  static const press = Duration(milliseconds: 100),
      fast = Duration(milliseconds: 150),
      standard = Duration(milliseconds: 250),
      enter = Duration(milliseconds: 300),
      exit = Duration(milliseconds: 200);
}

/// Tokens que `ColorScheme` no tiene: éxito, neutros, segmento activo, azul de texto
/// pequeño y la sombra suave de las tarjetas (vacía en oscuro).
@immutable
class AppSemantic extends ThemeExtension<AppSemantic> {
  const AppSemantic({
    required this.success,
    required this.successContainer,
    required this.neutralContainer,
    required this.segmentActive,
    required this.onSegmentActive,
    required this.blueText,
    required this.cardShadow,
  });

  final Color success, successContainer, neutralContainer;
  final Color segmentActive, onSegmentActive, blueText;
  final List<BoxShadow> cardShadow;

  static final light = AppSemantic(
    success: AppColors.lightSuccess,
    successContainer: AppColors.lightSuccessContainer,
    neutralContainer: AppColors.lightNeutralContainer,
    segmentActive: AppColors.lightSegmentActive,
    onSegmentActive: AppColors.blanco,
    blueText: AppColors.lightBlueText,
    cardShadow: [
      BoxShadow(
        color: AppColors.asfalto.withValues(alpha: .09),
        blurRadius: 18,
        offset: const Offset(0, 6),
      ),
    ],
  );

  static final dark = AppSemantic(
    success: AppColors.darkSuccess,
    successContainer: AppColors.darkSuccessContainer,
    neutralContainer: AppColors.darkNeutralContainer,
    segmentActive: AppColors.darkSegmentActive,
    onSegmentActive: AppColors.blanco,
    blueText: AppColors.darkBlueText,
    cardShadow: const [],
  );

  @override
  AppSemantic copyWith({
    Color? success,
    Color? successContainer,
    Color? neutralContainer,
    Color? segmentActive,
    Color? onSegmentActive,
    Color? blueText,
    List<BoxShadow>? cardShadow,
  }) => AppSemantic(
    success: success ?? this.success,
    successContainer: successContainer ?? this.successContainer,
    neutralContainer: neutralContainer ?? this.neutralContainer,
    segmentActive: segmentActive ?? this.segmentActive,
    onSegmentActive: onSegmentActive ?? this.onSegmentActive,
    blueText: blueText ?? this.blueText,
    cardShadow: cardShadow ?? this.cardShadow,
  );

  @override
  AppSemantic lerp(ThemeExtension<AppSemantic>? other, double t) =>
      t < .5 ? this : (other as AppSemantic? ?? this);
}

extension AppTemaContext on BuildContext {
  /// Los tokens semánticos del tema. Sin el tema de la app (un test con un `MaterialApp`
  /// pelado) cae a los del modo claro.
  AppSemantic get semantica =>
      Theme.of(this).extension<AppSemantic>() ?? AppSemantic.light;
}

abstract final class AppTheme {
  static ThemeData get light => _build(
    ColorScheme(
      brightness: Brightness.light,
      primary: AppColors.asfalto,
      onPrimary: AppColors.blanco,
      secondary: AppColors.ambar,
      onSecondary: AppColors.asfalto,
      secondaryContainer: AppColors.amberContainer,
      onSecondaryContainer: AppColors.asfalto,
      tertiary: AppColors.azul,
      onTertiary: AppColors.blanco,
      tertiaryContainer: AppColors.lightBlueContainer,
      onTertiaryContainer: AppColors.lightBlueText,
      error: AppColors.lightError,
      onError: AppColors.blanco,
      errorContainer: AppColors.lightErrorContainer,
      onErrorContainer: AppColors.lightError,
      surface: AppColors.lightSurface,
      onSurface: AppColors.lightOnSurface,
      onSurfaceVariant: AppColors.lightOnSurfaceVariant,
      outline: AppColors.lightOutline,
      outlineVariant: AppColors.lightOutline,
      surfaceTint: Colors.transparent,
    ),
    AppColors.lightBackground,
    AppSemantic.light,
  );

  static ThemeData get dark => _build(
    ColorScheme(
      brightness: Brightness.dark,
      primary: AppColors.grisClaro,
      onPrimary: AppColors.asfalto,
      secondary: AppColors.ambar,
      onSecondary: AppColors.asfalto,
      secondaryContainer: AppColors.amberContainer,
      onSecondaryContainer: AppColors.grisClaro,
      tertiary: AppColors.azul,
      onTertiary: AppColors.blanco,
      tertiaryContainer: AppColors.darkBlueContainer,
      onTertiaryContainer: AppColors.darkBlueText,
      error: AppColors.darkError,
      onError: AppColors.asfalto,
      errorContainer: AppColors.darkErrorContainer,
      onErrorContainer: AppColors.darkError,
      surface: AppColors.darkSurface,
      onSurface: AppColors.darkOnSurface,
      onSurfaceVariant: AppColors.darkOnSurfaceVariant,
      outline: AppColors.darkOutline,
      outlineVariant: AppColors.darkOutline,
      surfaceTint: Colors.transparent,
    ),
    AppColors.darkBackground,
    AppSemantic.dark,
  );

  static ThemeData _build(ColorScheme cs, Color fondo, AppSemantic sem) {
    final text = _textTheme(cs);
    const pill = StadiumBorder();

    return ThemeData(
      useMaterial3: true,
      brightness: cs.brightness,
      colorScheme: cs,
      scaffoldBackgroundColor: fondo,
      fontFamily: _familia,
      textTheme: text,
      extensions: [sem],
      appBarTheme: AppBarTheme(
        backgroundColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        surfaceTintColor: Colors.transparent,
        foregroundColor: cs.onSurface,
        titleTextStyle: text.headlineMedium,
        centerTitle: false,
        toolbarHeight: 64,
      ),
      // * La barra inferior la dibuja `BarraInferior` (esquinas y sombra); acá solo su contenido
      navigationBarTheme: NavigationBarThemeData(
        height: 84,
        backgroundColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        indicatorColor: cs.secondary,
        indicatorShape: const StadiumBorder(),
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        iconTheme: WidgetStateProperty.resolveWith(
          (s) => IconThemeData(
            size: 24,
            color: s.contains(WidgetState.selected)
                ? cs.onSecondary
                : cs.onSurfaceVariant,
          ),
        ),
        labelTextStyle: WidgetStateProperty.resolveWith(
          (s) => conPeso(
            text.labelMedium!.copyWith(
              fontSize: 12,
              color: s.contains(WidgetState.selected)
                  ? cs.onSurface
                  : cs.onSurfaceVariant,
            ),
            s.contains(WidgetState.selected)
                ? FontWeight.w900
                : FontWeight.w700,
          ),
        ),
      ),
      // * La acción principal es ámbar: `secondary`, no `primary`
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size.fromHeight(56),
          shape: pill,
          backgroundColor: cs.secondary,
          foregroundColor: cs.onSecondary,
          disabledBackgroundColor: cs.secondary.withValues(alpha: .38),
          disabledForegroundColor: cs.onSecondary.withValues(alpha: .38),
          textStyle: text.labelLarge,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size.fromHeight(56),
          shape: pill,
          foregroundColor: cs.onSurface,
          backgroundColor: cs.surface,
          side: BorderSide(color: cs.onSurface, width: 2),
          textStyle: text.labelLarge,
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          minimumSize: const Size(48, 48),
          foregroundColor: cs.onTertiaryContainer,
          textStyle: text.labelMedium!.copyWith(fontSize: 15),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: cs.surface,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 18,
          vertical: 16,
        ),
        border: _borde(Colors.transparent),
        enabledBorder: _borde(Colors.transparent),
        focusedBorder: _borde(cs.onSurface),
        errorBorder: _borde(cs.error),
        focusedErrorBorder: _borde(cs.error),
        errorStyle: text.bodyMedium!.copyWith(
          color: cs.error,
          fontSize: 13.5,
          fontWeight: FontWeight.w700,
        ),
        hintStyle: text.bodyLarge!.copyWith(color: cs.onSurfaceVariant),
        labelStyle: text.bodyLarge!.copyWith(color: cs.onSurfaceVariant),
      ),
      cardTheme: CardThemeData(
        color: cs.surface,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppRadius.card),
        ),
      ),
      segmentedButtonTheme: SegmentedButtonThemeData(
        style: SegmentedButton.styleFrom(
          minimumSize: const Size.fromHeight(52),
          shape: pill,
          side: BorderSide.none,
          backgroundColor: sem.neutralContainer,
          foregroundColor: cs.onSurfaceVariant,
          selectedBackgroundColor: sem.segmentActive,
          selectedForegroundColor: sem.onSegmentActive,
          textStyle: text.labelMedium!.copyWith(fontSize: 15),
        ),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        backgroundColor: cs.onSurface,
        contentTextStyle: text.bodyMedium!.copyWith(color: cs.surface),
        actionTextColor: cs.secondary,
        insetPadding: const EdgeInsets.all(16),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: cs.surface,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppRadius.card),
        ),
        titleTextStyle: text.titleLarge,
        contentTextStyle: text.bodyLarge,
        barrierColor: Colors.black.withValues(alpha: .40),
      ),
      progressIndicatorTheme: ProgressIndicatorThemeData(
        color: cs.secondary,
        linearTrackColor: sem.neutralContainer,
        circularTrackColor: sem.neutralContainer,
      ),
      dividerTheme: DividerThemeData(color: cs.outline, thickness: 1),
    );
  }

  static OutlineInputBorder _borde(Color color) => OutlineInputBorder(
    borderRadius: BorderRadius.circular(AppRadius.button),
    borderSide: BorderSide(color: color, width: 2),
  );

  static const _familia = 'Nunito';

  // * Nunito es una fuente variable (assets/fonts): el peso se fija con el eje `wght`, además
  // * de `fontWeight`. Para cambiar el peso de un estilo usa `AppTheme.conPeso`, no `copyWith`.
  static TextStyle conPeso(TextStyle estilo, FontWeight peso) => estilo.copyWith(
    fontWeight: peso,
    fontVariations: [FontVariation('wght', peso.value.toDouble())],
  );

  static TextTheme _textTheme(ColorScheme cs) {
    TextStyle s(double size, FontWeight peso, double alto, [Color? color]) =>
        TextStyle(
          fontFamily: _familia,
          fontVariations: [FontVariation('wght', peso.value.toDouble())],
          fontSize: size,
          fontWeight: peso,
          height: alto,
          color: color ?? cs.onSurface,
          fontFeatures: const [FontFeature.tabularFigures()],
        );

    return TextTheme(
      displaySmall: s(30, FontWeight.w900, 1.10),
      headlineMedium: s(26, FontWeight.w900, 1.10),
      headlineSmall: s(22, FontWeight.w900, 1.15),
      titleLarge: s(18, FontWeight.w800, 1.25),
      titleMedium: s(20, FontWeight.w900, 1.20),
      bodyLarge: s(16, FontWeight.w600, 1.40),
      bodyMedium: s(14, FontWeight.w600, 1.40, cs.onSurfaceVariant),
      labelLarge: s(17, FontWeight.w800, 1.20),
      labelMedium: s(14, FontWeight.w800, 1.20),
      labelSmall: s(12.5, FontWeight.w800, 1.20),
    );
  }
}
