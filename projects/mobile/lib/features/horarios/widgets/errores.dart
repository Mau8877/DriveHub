import '../../../core/network/api_client.dart';

/// Mensaje para mostrar al instructor cuando una llamada de horarios o franjas falla.
///
/// * Un error de negocio trae su mensaje en español desde el backend (por ejemplo, "El horario se
/// * cruza con otro horario activo tuyo"); cualquier otro (sin red, tiempo agotado) es genérico.
String mensajeDeError(Object error) {
  if (error is ApiException) return error.message;
  return 'No pudimos conectar con el servidor. Intenta de nuevo.';
}
