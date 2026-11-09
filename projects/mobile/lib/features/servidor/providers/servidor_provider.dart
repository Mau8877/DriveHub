import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../services/probar_servidor.dart';
import '../services/servidor_storage.dart';
import '../validators/servidor_validators.dart';

/// Si la app ya sabe con qué backend habla.
@immutable
class EstadoServidor {
  /// `false` solo la primera vez que se abre la app (o si se borra la configuración): el router lleva a la
  /// pantalla de servidor antes que a ninguna otra.
  final bool configurado;

  /// La dirección completa de la API que eligió la persona; `null` si usa la del `.env`.
  final String? url;

  const EstadoServidor({required this.configurado, this.url});
}

/// Cómo arranca la app. `main()` lo sustituye con lo que leyó del teléfono.
///
/// * Por defecto, ya configurada: así los tests de las demás pantallas no pasan por la configuración.
final servidorInicialProvider = Provider<EstadoServidor>((ref) => const EstadoServidor(configurado: true));

/// El backend con el que habla la app (ver `ServidorScreen`).
///
/// * Guardar normaliza lo que escribió la persona, lo guarda en el teléfono y cambia la dirección del `ApiClient` en
///   el momento: no hace falta reiniciar la app.
class ServidorNotifier extends Notifier<EstadoServidor> {
  @override
  EstadoServidor build() => ref.read(servidorInicialProvider);

  /// Guarda la dirección y la deja en uso. Devuelve la dirección completa de la API.
  ///
  /// ! Lanza [ArgumentError] si [entrada] no es una dirección válida: la pantalla ya la validó antes de llamar.
  Future<String> guardar(String entrada) async {
    final url = normalizarDireccionServidor(entrada);
    if (url == null) throw ArgumentError.value(entrada, 'entrada', 'no es una dirección de servidor válida');
    await ref.read(servidorStorageProvider).guardar(url);
    ApiClient.configurarBaseUrl(url);
    state = EstadoServidor(configurado: true, url: url);
    return url;
  }
}

final servidorProvider = NotifierProvider<ServidorNotifier, EstadoServidor>(ServidorNotifier.new);

/// La prueba de conexión; es un provider aparte para sustituirla en los tests (ninguno sale a la red).
final probadorServidorProvider = Provider<Future<ResultadoPrueba> Function(String direccionApi)>(
  (ref) => probarServidor,
);
