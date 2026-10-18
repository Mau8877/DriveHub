import 'package:flutter/material.dart';

/// Paleta e identidad visual de la app.
///
/// * Espejo manual de las variables de `projects/frontend/src/index.css` --
/// * no hay forma de compartir el archivo de tokens entre Tailwind/CSS y
/// * Dart (ver WIDGETS_UI_MOBILE.md), así que todo cambio de paleta ahí se
/// * replica acá a mano en el mismo cambio.
/// ! Paleta neutra de placeholder -- reemplazar por la identidad de marca
/// ! real del proyecto cuando exista (ver TAILWIND_STYLES_FRONTEND.md).
abstract final class AppColors {
  // Modo claro
  static const lightPrimary = Color(0xFF262626);
  static const lightAccent = Color(0xFF3B82F6);
  static const lightBackground = Color(0xFFFFFFFF);
  static const lightSurface = Color(0xFFFFFFFF);
  static const lightTextPrimary = Color(0xFF171717);
  static const lightTextSecondary = Color(0xFF737373);
  static const lightSuccess = Color(0xFF16A34A);
  static const lightError = Color(0xFFDC2626);
  static const lightWarning = Color(0xFFD97706);
  static const lightInfo = Color(0xFF2563EB);

  // Modo oscuro
  static const darkPrimary = Color(0xFFE5E5E5);
  static const darkAccent = Color(0xFF60A5FA);
  static const darkBackground = Color(0xFF171717);
  static const darkSurface = Color(0xFF262626);
  static const darkTextPrimary = Color(0xFFF5F5F5);
  static const darkTextSecondary = Color(0xFFA3A3A3);
  static const darkSuccess = Color(0xFF4ADE80);
  static const darkError = Color(0xFFF87171);
  static const darkWarning = Color(0xFFFBBF24);
  static const darkInfo = Color(0xFF93C5FD);

  // Header -- superficie fija de marca, no varía entre claro/oscuro (mismo
  // criterio que `--header` en TAILWIND_STYLES_FRONTEND.md: si el AppBar
  // usara `primary`, perdería la identidad visual de marca al pasar a modo
  // oscuro, donde `primary` se aclara para mantener contraste).
  static const header = Color(0xFF262626);
  static const headerForeground = Colors.white;
}

abstract final class AppTheme {
  static ThemeData get light {
    const colorScheme = ColorScheme.light(
      primary: AppColors.lightPrimary,
      onPrimary: Colors.white,
      secondary: AppColors.lightAccent,
      onSecondary: Colors.white,
      surface: AppColors.lightSurface,
      onSurface: AppColors.lightTextPrimary,
      error: AppColors.lightError,
      onError: Colors.white,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: AppColors.lightBackground,
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.header,
        foregroundColor: AppColors.headerForeground,
      ),
    );
  }

  static ThemeData get dark {
    const colorScheme = ColorScheme.dark(
      primary: AppColors.darkPrimary,
      onPrimary: AppColors.darkBackground,
      secondary: AppColors.darkAccent,
      onSecondary: Colors.white,
      surface: AppColors.darkSurface,
      onSurface: AppColors.darkTextPrimary,
      error: AppColors.darkError,
      onError: AppColors.darkBackground,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: AppColors.darkBackground,
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.header,
        foregroundColor: AppColors.headerForeground,
      ),
    );
  }
}
