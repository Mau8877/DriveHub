import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';

/// Feature de diagnóstico -- no es una feature de negocio real, existe para
/// probar de punta a punta que `ApiClient` (Dio + interceptor de sobre) y
/// Riverpod (provider + AsyncValue) funcionan contra el backend real antes
/// de que exista la primera feature real. Se borra o se reemplaza el día
/// que haya un mejor lugar para un chequeo de conectividad.
class HealthService {
  /// `GET /health` -- vive FUERA de `/api/v1` (es un endpoint de
  /// infraestructura, ver ENDPOINTS_BACKEND.md), así que no pasa por el
  /// sobre `{status,data,message,error}` del contrato de negocio. Por eso
  /// usa un `Dio` aparte (sin el interceptor de `ApiClient`) apuntando al
  /// origin del backend, no a `ApiClient.instance` (que ya trae `/api/v1`
  /// como base y además intentaría desempaquetar un sobre que esta
  /// respuesta no tiene).
  Future<Map<String, dynamic>> checkHealth() async {
    final baseUrl = ApiClient.instance.options.baseUrl;
    final origin = baseUrl.replaceFirst(RegExp(r'/api/v1/?$'), '');
    final response = await Dio().get<Map<String, dynamic>>('$origin/health');
    return response.data!;
  }

  /// `GET` a una ruta que no existe bajo `/api/v1` -- a propósito, para
  /// probar qué hace `ApiClient` con un error real del backend (todavía no
  /// hay ningún `@RestController` de negocio real contra el cual probar el
  /// desempaquetado de un error catalogado, ver CONSUMO_API_MOBILE.md).
  Future<void> pingRutaInexistente() async {
    await ApiClient.instance.get<void>('/ping-ruta-que-no-existe');
  }
}
