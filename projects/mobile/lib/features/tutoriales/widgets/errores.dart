import '../../../core/network/api_client.dart';

/// Mensaje para mostrar al instructor cuando una llamada de tutoriales falla.
///
/// * Un error de negocio trae su mensaje en español desde el backend (por ejemplo, "El enlace debe ser de un video
///   de YouTube"); cualquier otro (sin red, tiempo agotado) es genérico.
String mensajeDeErrorDeTutorial(Object error) {
  if (error is ApiException) return error.message;
  return 'No pudimos conectar con el servidor. Intenta de nuevo.';
}
