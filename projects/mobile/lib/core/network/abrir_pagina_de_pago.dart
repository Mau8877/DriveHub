import 'package:flutter/foundation.dart';
import 'package:url_launcher/url_launcher.dart';

/// Abre la página de pago de la pasarela (CU26) en una pestaña del navegador dentro de la app.
///
/// * Solo abre direcciones `https` con servidor: cualquier otra cosa (`http`, `javascript:`, un esquema
/// * propio, vacío) se rechaza y devuelve `false`. La dirección sale del backend, pero nunca se abre a ciegas.
/// * Ningún dato de tarjeta pasa por la app: se ingresa solo en la página de la pasarela.
/// * Un error de la plataforma (sin navegador, por ejemplo) devuelve `false`, no una excepción.
/// * [lanzar] es lo que de verdad abre la dirección; solo se cambia en los tests.
Future<bool> abrirPaginaDePago(
  String url, {
  Future<bool> Function(Uri direccion) lanzar = _abrirEnElNavegador,
}) async {
  final direccion = Uri.tryParse(url.trim());
  if (direccion == null || direccion.scheme != 'https' || direccion.host.isEmpty) {
    return false;
  }
  try {
    return await lanzar(direccion);
  } catch (e) {
    debugPrint('No se pudo abrir la página de pago: ${e.runtimeType}');
    return false;
  }
}

Future<bool> _abrirEnElNavegador(Uri direccion) =>
    launchUrl(direccion, mode: LaunchMode.inAppBrowserView);
