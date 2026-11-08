import 'dart:convert';

import 'package:dio/dio.dart';

import 'modelos_offline.dart';
import 'nucleo_offline.dart';

/// Cómo una `service` pide que una escritura se encole si no hay conexión.
///
/// * Solo las acciones que se pueden validar después en el servidor llevan esta marca (cerrar una clase, reservar...):
/// * el resto exige internet y falla como siempre.
class OpcionesOffline {
  static const claveDescripcion = 'offline';
  static const claveTipo = 'offlineTipo';
  static const claveSinOffline = 'sinOffline';
  static const claveSinCache = 'sinCache';
  static const claveDesdeCache = 'desdeCache';

  /// Opciones de Dio para una escritura que, sin conexión, se guarda y se envía al volver internet. [descripcion]
  /// es lo que ve la persona en la lista de pendientes («Cierre de clase del 12 nov»).
  ///
  /// * [tipo] solo hace falta si el servidor puede responder 200 habiendo rechazado una parte: el núcleo revisa la
  /// * respuesta con el verificador registrado para ese tipo (ver `NucleoOffline.verificadores`).
  static Options escritura(String descripcion, {String? tipo}) =>
      Options(extra: {claveDescripcion: descripcion, claveTipo: ?tipo});

  /// Opciones para una lectura que nunca debe responderse desde lo guardado (un dato que cambia a cada rato).
  static Options sinCache() => Options(extra: {claveSinCache: true});
}

/// El interceptor que le da modo offline al `ApiClient`.
///
/// * Lecturas (`GET`): cada respuesta correcta se guarda; si la petición no puede salir, se responde con lo último
/// * guardado (la respuesta lleva `extra['desdeCache'] = true`). Sin nada guardado, el error de red sigue su camino.
/// * Escrituras marcadas con [OpcionesOffline.escritura]: si no pueden salir, se encolan y se rechaza con
/// * [AccionEncolada] (la pantalla avisa «guardado, se enviará al volver internet»).
/// * Cualquier respuesta del servidor (aunque sea un error) cuenta como «hay conexión».
class InterceptorOffline extends Interceptor {
  final NucleoOffline? Function() _nucleo;

  InterceptorOffline(this._nucleo);

  @override
  void onResponse(Response<dynamic> response, ResponseInterceptorHandler handler) {
    final nucleo = _nucleo();
    if (nucleo != null) {
      nucleo.marcarConexion();
      if (_esCacheable(response.requestOptions) && _esJson(response.data)) {
        // * Sin esperar: guardar en disco no frena la pantalla, y un fallo al guardar no es un fallo de la lectura
        nucleo.guardarCache(_clave(response.requestOptions), response.data as Object).catchError((_) {});
      }
    }
    handler.next(response);
  }

  @override
  Future<void> onError(DioException err, ErrorInterceptorHandler handler) async {
    final error = err;
    final nucleo = _nucleo();
    if (nucleo == null) {
      handler.next(error);
      return;
    }
    if (!NucleoOffline.esFalloDeRed(error)) {
      // * El servidor respondió (aunque sea con un error): hay conexión
      if (error.response != null) nucleo.marcarConexion();
      handler.next(error);
      return;
    }

    nucleo.marcarSinConexion();
    final opciones = error.requestOptions;

    if (_esCacheable(opciones)) {
      final guardado = await nucleo.leerCache(_clave(opciones));
      if (guardado != null) {
        handler.resolve(
          Response<dynamic>(
            requestOptions: opciones,
            statusCode: 200,
            data: guardado,
            extra: {OpcionesOffline.claveDesdeCache: true},
          ),
        );
        return;
      }
    }

    final descripcion = opciones.extra[OpcionesOffline.claveDescripcion];
    if (descripcion is String && _seEncola(opciones, error, nucleo)) {
      await nucleo.encolar(
        metodo: opciones.method,
        ruta: opciones.path,
        descripcion: descripcion,
        cuerpo: opciones.data,
        tipo: opciones.extra[OpcionesOffline.claveTipo] as String?,
      );
      handler.reject(
        DioException(
          requestOptions: opciones,
          error: AccionEncolada(descripcion),
          type: DioExceptionType.unknown,
        ),
      );
      return;
    }

    handler.next(error);
  }

  /// Solo se guardan las lecturas JSON de la API de negocio (no el login, ni descargas de bytes).
  static bool _esCacheable(RequestOptions o) =>
      o.method.toUpperCase() == 'GET' &&
      o.responseType == ResponseType.json &&
      !o.path.startsWith('/auth/') &&
      o.extra[OpcionesOffline.claveSinCache] != true;

  static bool _esJson(Object? datos) => datos is Map || datos is List;

  /// Una escritura se encola solo si no salió nunca (sin conexión, no «se cortó a la mitad»: ahí no se sabe si el
  /// servidor la recibió), si no es un reintento de la propia cola y si su cuerpo se puede guardar como JSON.
  static bool _seEncola(RequestOptions o, DioException error, NucleoOffline nucleo) {
    if (!nucleo.conSesion) return false;
    if (o.extra[OpcionesOffline.claveSinOffline] == true) return false;
    if (o.method.toUpperCase() == 'GET') return false;
    if (o.queryParameters.isNotEmpty) return false;
    if (error.type != DioExceptionType.connectionError && error.type != DioExceptionType.connectionTimeout) {
      return false;
    }
    final cuerpo = o.data;
    if (cuerpo == null) return true;
    if (cuerpo is! Map && cuerpo is! List) return false;
    try {
      jsonEncode(cuerpo);
      return true;
    } catch (_) {
      return false;
    }
  }

  /// La URL completa con los parámetros ordenados: la misma lectura siempre da la misma clave.
  static String _clave(RequestOptions o) {
    final uri = o.uri;
    if (uri.queryParameters.isEmpty) return uri.toString();
    final ordenados = Map.fromEntries(
      uri.queryParametersAll.entries.toList()..sort((a, b) => a.key.compareTo(b.key)),
    );
    return uri.replace(queryParameters: ordenados).toString();
  }
}
