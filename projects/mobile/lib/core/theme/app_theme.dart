import 'package:flutter/material.dart';

/// Paleta e identidad visual de la app: el diseño "Cockpit" (mockup `diseno-3-cockpit`).
///
/// * Espejo manual de `packages/design-tokens/tokens.css` -- no hay forma de
/// * compartir el archivo de tokens entre CSS y Dart (ver WIDGETS_UI_MOBILE.md),
/// * así que todo cambio de paleta ahí se replica acá a mano en el mismo cambio.
/// * Paleta de marca: Gris Asfalto #1E293B (estructura, tipografía), Ámbar / Vial
/// * #F59E0B (acción clave, resaltados), Azul Técnico #0284C7 (estados), Gris Claro
/// * #F1F5F9 (fondo), Blanco #FFFFFF (superficies) y Gris Medio #475569 (texto secundario).
/// * * Legibilidad: el ámbar no tiene contraste sobre blanco, así que el TEXTO de acento usa
/// * * `ambarTexto` en el modo claro; el ámbar puro queda para fondos de botones y brillos.
abstract final class AppColors {
  // Marca
  static const asfalto = Color(0xFF1E293B);
  static const ambar = Color(0xFFF59E0B);
  static const azulTecnico = Color(0xFF0284C7);
  static const grisClaro = Color(0xFFF1F5F9);
  static const grisMedio = Color(0xFF475569);

  /// Ámbar oscurecido para texto sobre superficies claras (contraste 5:1 sobre blanco).
  static const ambarTexto = Color(0xFFB45309);

  /// Azul oscurecido para enlaces y texto sobre superficies claras.
  static const azulTexto = Color(0xFF0369A1);

  // Modo claro
  static const lightPrimary = ambar; // acción clave
  static const lightOnPrimary = asfalto; // sobre ámbar el texto va en asfalto
  static const lightAccent = azulTecnico;
  static const lightBackground = grisClaro;
  static const lightSurface = Color(0xFFFFFFFF);
  static const lightTextPrimary = asfalto;
  static const lightTextSecondary = grisMedio;
  static const lightSuccess = Color(0xFF15803D);
  static const lightError = Color(0xFFDC2626);
  static const lightWarning = ambar;
  static const lightInfo = azulTexto;
  static const lightBorder = Color(0x40475569); // gris medio al 25%

  // Modo oscuro (tonos derivados de la misma familia)
  static const darkPrimary = ambar;
  static const darkOnPrimary = asfalto;
  static const darkAccent = Color(0xFF38BDF8);
  static const darkBackground = Color(0xFF0F172A);
  static const darkSurface = asfalto;
  static const darkTextPrimary = grisClaro;
  static const darkTextSecondary = Color(0xFFA6B3C6);
  static const darkSuccess = Color(0xFF4ADE80);
  static const darkError = Color(0xFFF87171);
  static const darkWarning = ambar;
  static const darkInfo = Color(0xFF38BDF8);
  static const darkBorder = Color(0x1FFFFFFF); // blanco al 12%
}

/// Colores del diseño Cockpit que `ColorScheme` no tiene, disponibles con `context.cockpit`.
@immutable
class CockpitColores extends ThemeExtension<CockpitColores> {
  /// Texto e iconos de acento (ámbar legible sobre el fondo del modo actual).
  final Color acento;

  /// Texto de enlaces y de estados informativos.
  final Color enlace;

  /// Éxito (verde legible sobre superficies del modo actual).
  final Color exito;

  /// Brillo azul y ámbar del fondo y de los bordes destacados.
  final Color brilloAzul;
  final Color brilloAmbar;

  /// Superficie de los botones de la barra y de los chips (un poco distinta de la tarjeta).
  final Color superficieSuave;

  final Color borde;

  const CockpitColores({
    required this.acento,
    required this.enlace,
    required this.exito,
    required this.brilloAzul,
    required this.brilloAmbar,
    required this.superficieSuave,
    required this.borde,
  });

  static const claro = CockpitColores(
    acento: AppColors.ambarTexto,
    enlace: AppColors.azulTexto,
    exito: AppColors.lightSuccess,
    // * En claro los brillos del fondo son muy tenues: nunca quedan detrás de texto
    brilloAzul: Color(0x290284C7),
    brilloAmbar: Color(0x2EF59E0B),
    superficieSuave: Color(0xFFE8EEF5),
    borde: AppColors.lightBorder,
  );

  static const oscuro = CockpitColores(
    acento: AppColors.ambar,
    enlace: AppColors.darkInfo,
    exito: AppColors.darkSuccess,
    brilloAzul: Color(0x610284C7),
    brilloAmbar: Color(0x33F59E0B),
    superficieSuave: Color(0xFF2B3A52),
    borde: AppColors.darkBorder,
  );

  @override
  CockpitColores copyWith({
    Color? acento,
    Color? enlace,
    Color? exito,
    Color? brilloAzul,
    Color? brilloAmbar,
    Color? superficieSuave,
    Color? borde,
  }) {
    return CockpitColores(
      acento: acento ?? this.acento,
      enlace: enlace ?? this.enlace,
      exito: exito ?? this.exito,
      brilloAzul: brilloAzul ?? this.brilloAzul,
      brilloAmbar: brilloAmbar ?? this.brilloAmbar,
      superficieSuave: superficieSuave ?? this.superficieSuave,
      borde: borde ?? this.borde,
    );
  }

  @override
  CockpitColores lerp(ThemeExtension<CockpitColores>? other, double t) {
    if (other is! CockpitColores) return this;
    return CockpitColores(
      acento: Color.lerp(acento, other.acento, t)!,
      enlace: Color.lerp(enlace, other.enlace, t)!,
      exito: Color.lerp(exito, other.exito, t)!,
      brilloAzul: Color.lerp(brilloAzul, other.brilloAzul, t)!,
      brilloAmbar: Color.lerp(brilloAmbar, other.brilloAmbar, t)!,
      superficieSuave: Color.lerp(superficieSuave, other.superficieSuave, t)!,
      borde: Color.lerp(borde, other.borde, t)!,
    );
  }
}

extension CockpitContext on BuildContext {
  CockpitColores get cockpit =>
      Theme.of(this).extension<CockpitColores>() ??
      (Theme.of(this).brightness == Brightness.dark
          ? CockpitColores.oscuro
          : CockpitColores.claro);
}

/// Radios del diseño: controles `--r`, chips y campos pequeños `--r-sm`, tarjetas `--r-lg`.
abstract final class AppRadios {
  static const control = 15.0;
  static const pequeno = 10.0;
  static const tarjeta = 24.0;
}

abstract final class AppTheme {
  static ThemeData get light => _construir(
    brillo: Brightness.light,
    colorScheme: const ColorScheme.light(
      primary: AppColors.lightPrimary,
      onPrimary: AppColors.lightOnPrimary,
      secondary: AppColors.lightAccent,
      onSecondary: Colors.white,
      surface: AppColors.lightSurface,
      onSurface: AppColors.lightTextPrimary,
      onSurfaceVariant: AppColors.lightTextSecondary,
      error: AppColors.lightError,
      onError: Colors.white,
      outline: AppColors.lightBorder,
      outlineVariant: AppColors.lightBorder,
    ),
    fondo: AppColors.lightBackground,
    colores: CockpitColores.claro,
  );

  static ThemeData get dark => _construir(
    brillo: Brightness.dark,
    colorScheme: const ColorScheme.dark(
      primary: AppColors.darkPrimary,
      onPrimary: AppColors.darkOnPrimary,
      secondary: AppColors.darkAccent,
      onSecondary: AppColors.asfalto,
      surface: AppColors.darkSurface,
      onSurface: AppColors.darkTextPrimary,
      onSurfaceVariant: AppColors.darkTextSecondary,
      error: AppColors.darkError,
      onError: AppColors.asfalto,
      outline: AppColors.darkBorder,
      outlineVariant: AppColors.darkBorder,
    ),
    fondo: AppColors.darkBackground,
    colores: CockpitColores.oscuro,
  );

  static ThemeData _construir({
    required Brightness brillo,
    required ColorScheme colorScheme,
    required Color fondo,
    required CockpitColores colores,
  }) {
    final base = ThemeData(brightness: brillo, useMaterial3: true);
    final controlForma = RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(AppRadios.control),
    );
    final bordeCampo = OutlineInputBorder(
      borderRadius: BorderRadius.circular(AppRadios.control),
      borderSide: BorderSide(color: colores.borde),
    );

    return base.copyWith(
      colorScheme: colorScheme,
      extensions: [colores],
      scaffoldBackgroundColor: fondo,
      canvasColor: colorScheme.surface,
      textTheme: base.textTheme
          .apply(
            bodyColor: colorScheme.onSurface,
            displayColor: colorScheme.onSurface,
          )
          .copyWith(
            // * Títulos de pantalla: peso alto y tracking cerrado, como el h1 del mockup
            headlineSmall: base.textTheme.headlineSmall?.copyWith(
              fontWeight: FontWeight.w800,
              letterSpacing: -0.6,
              color: colorScheme.onSurface,
            ),
            titleLarge: base.textTheme.titleLarge?.copyWith(
              fontWeight: FontWeight.w800,
              letterSpacing: -0.4,
              color: colorScheme.onSurface,
            ),
            titleMedium: base.textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w700,
              color: colorScheme.onSurface,
            ),
            bodySmall: base.textTheme.bodySmall?.copyWith(
              color: colorScheme.onSurfaceVariant,
            ),
          ),
      // * La barra es transparente: el título y los botones se leen sobre el fondo de la pantalla
      appBarTheme: AppBarTheme(
        backgroundColor: Colors.transparent,
        foregroundColor: colorScheme.onSurface,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        titleTextStyle: TextStyle(
          fontSize: 22,
          fontWeight: FontWeight.w800,
          letterSpacing: -0.4,
          color: colorScheme.onSurface,
        ),
      ),
      // * Superficies opacas con borde fino: nada de transparencias sobre el degradado del fondo
      cardTheme: CardThemeData(
        color: colorScheme.surface,
        elevation: 0,
        margin: EdgeInsets.zero,
        clipBehavior: Clip.antiAlias,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppRadios.tarjeta),
          side: BorderSide(color: colores.borde),
        ),
      ),
      dividerTheme: DividerThemeData(color: colores.borde, space: 1, thickness: 1),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: colorScheme.surface,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        border: bordeCampo,
        enabledBorder: bordeCampo,
        disabledBorder: bordeCampo,
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadios.control),
          borderSide: const BorderSide(color: AppColors.ambar, width: 2),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadios.control),
          borderSide: BorderSide(color: colorScheme.error),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadios.control),
          borderSide: BorderSide(color: colorScheme.error, width: 2),
        ),
        labelStyle: TextStyle(color: colorScheme.onSurfaceVariant),
        floatingLabelStyle: TextStyle(
          color: colorScheme.onSurface,
          fontWeight: FontWeight.w700,
        ),
        prefixIconColor: WidgetStateColor.resolveWith(
          (estados) => estados.contains(WidgetState.focused)
              ? colorScheme.onSurface
              : colorScheme.onSurfaceVariant,
        ),
        suffixIconColor: colorScheme.onSurfaceVariant,
        errorStyle: TextStyle(color: colorScheme.error, fontSize: 12.5),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: colorScheme.primary,
          foregroundColor: colorScheme.onPrimary,
          disabledBackgroundColor: colorScheme.primary.withValues(alpha: 0.45),
          disabledForegroundColor: colorScheme.onPrimary.withValues(alpha: 0.6),
          minimumSize: const Size.fromHeight(50),
          shape: controlForma,
          textStyle: const TextStyle(fontWeight: FontWeight.w800, fontSize: 15),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: colorScheme.onSurface,
          minimumSize: const Size.fromHeight(50),
          shape: controlForma,
          side: BorderSide(color: colores.borde),
          textStyle: const TextStyle(fontWeight: FontWeight.w700, fontSize: 15),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: colores.enlace,
          shape: controlForma,
          textStyle: const TextStyle(fontWeight: FontWeight.w700),
        ),
      ),
      iconButtonTheme: IconButtonThemeData(
        style: IconButton.styleFrom(foregroundColor: colorScheme.onSurface),
      ),
      // * Barra inferior opaca; la pestaña activa lleva el ámbar de marca (con texto legible)
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: colorScheme.surface,
        surfaceTintColor: Colors.transparent,
        indicatorColor: Colors.transparent,
        height: 68,
        elevation: 0,
        iconTheme: WidgetStateProperty.resolveWith((estados) {
          final activo = estados.contains(WidgetState.selected);
          return IconThemeData(
            size: 24,
            color: activo ? colores.acento : colorScheme.onSurfaceVariant,
          );
        }),
        labelTextStyle: WidgetStateProperty.resolveWith((estados) {
          final activo = estados.contains(WidgetState.selected);
          return TextStyle(
            fontSize: 11,
            fontWeight: FontWeight.w800,
            letterSpacing: 1.1,
            color: activo ? colores.acento : colorScheme.onSurfaceVariant,
          );
        }),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        backgroundColor: brillo == Brightness.dark
            ? AppColors.grisClaro
            : AppColors.asfalto,
        contentTextStyle: TextStyle(
          color: brillo == Brightness.dark
              ? AppColors.asfalto
              : Colors.white,
          fontWeight: FontWeight.w600,
        ),
        shape: controlForma,
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: colorScheme.surface,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppRadios.tarjeta),
          side: BorderSide(color: colores.borde),
        ),
        titleTextStyle: base.textTheme.titleLarge?.copyWith(
          fontWeight: FontWeight.w800,
          color: colorScheme.onSurface,
        ),
      ),
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: colorScheme.surface,
        surfaceTintColor: Colors.transparent,
        showDragHandle: true,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(
            top: Radius.circular(AppRadios.tarjeta),
          ),
        ),
      ),
      progressIndicatorTheme: ProgressIndicatorThemeData(
        color: colores.acento,
        linearTrackColor: colores.superficieSuave,
      ),
      segmentedButtonTheme: SegmentedButtonThemeData(
        style: SegmentedButton.styleFrom(
          backgroundColor: colores.superficieSuave,
          foregroundColor: colorScheme.onSurfaceVariant,
          selectedBackgroundColor: colorScheme.surface,
          selectedForegroundColor: colorScheme.onSurface,
          side: BorderSide.none,
          shape: controlForma,
          textStyle: const TextStyle(fontWeight: FontWeight.w700),
        ),
      ),
    );
  }
}
