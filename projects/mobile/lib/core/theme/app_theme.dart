import 'package:flutter/material.dart';

/// Paleta e identidad visual de la app.
///
/// * Espejo manual de `packages/design-tokens/tokens.css` -- no hay forma de
/// * compartir el archivo de tokens entre CSS y Dart (ver WIDGETS_UI_MOBILE.md),
/// * así que todo cambio de paleta ahí se replica acá a mano en el mismo cambio.
/// * Paleta de marca: Gris Asfalto #1E293B (estructura, tipografía), Ámbar / Vial
/// * #F59E0B (acción clave, resaltados), Azul Técnico #0284C7 (estados), Gris Claro
/// * #F1F5F9 (fondo), Blanco #FFFFFF (superficies) y Gris Medio #475569 (texto secundario).
abstract final class AppColors {
  // Marca
  static const asfalto = Color(0xFF1E293B);
  static const ambar = Color(0xFFF59E0B);
  static const azulTecnico = Color(0xFF0284C7);
  static const grisClaro = Color(0xFFF1F5F9);
  static const grisMedio = Color(0xFF475569);

  // Modo claro
  static const lightPrimary = ambar; // acción clave
  static const lightOnPrimary = asfalto; // sobre ámbar el texto va en asfalto
  static const lightAccent = azulTecnico;
  static const lightBackground = grisClaro;
  static const lightSurface = Color(0xFFFFFFFF);
  static const lightTextPrimary = asfalto;
  static const lightTextSecondary = grisMedio;
  static const lightSuccess = Color(0xFF16A34A);
  static const lightError = Color(0xFFDC2626);
  static const lightWarning = ambar;
  static const lightInfo = azulTecnico;

  // Modo oscuro (tonos derivados de la misma familia; hoy no se activa)
  static const darkPrimary = ambar;
  static const darkOnPrimary = asfalto;
  static const darkAccent = Color(0xFF38BDF8);
  static const darkBackground = Color(0xFF0F172A);
  static const darkSurface = asfalto;
  static const darkTextPrimary = grisClaro;
  static const darkTextSecondary = Color(0xFF94A3B8);
  static const darkSuccess = Color(0xFF4ADE80);
  static const darkError = Color(0xFFF87171);
  static const darkWarning = ambar;
  static const darkInfo = Color(0xFF38BDF8);

  // Header -- superficie fija de marca (asfalto), no varía entre claro/oscuro: así el AppBar
  // conserva la identidad y el ámbar (acción clave) se ve sobre él.
  static const header = asfalto;
  static const headerForeground = Colors.white;
}

abstract final class AppTheme {
  static ThemeData get light {
    const colorScheme = ColorScheme.light(
      primary: AppColors.lightPrimary,
      onPrimary: AppColors.lightOnPrimary,
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
      onPrimary: AppColors.darkOnPrimary,
      secondary: AppColors.darkAccent,
      onSecondary: AppColors.asfalto,
      surface: AppColors.darkSurface,
      onSurface: AppColors.darkTextPrimary,
      error: AppColors.darkError,
      onError: AppColors.asfalto,
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
