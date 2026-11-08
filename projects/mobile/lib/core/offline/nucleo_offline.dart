// ignore_for_file: prefer_initializing_formals
// * Los parámetros son nombrados y los campos privados: Dart no permite `this._x` en un parámetro nombrado.
import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../network/api_client.dart';
import 'almacen_offline.dart';
import 'modelos_offline.dart';

/// Todo lo que hace que la app funcione sin internet: la caché de lecturas, la cola de acciones pendientes, el
/// estado de la conexión y la sincronización al volver.
///
/// * No sabe de Riverpod ni de pantallas: el `ApiClient` (interceptor) lo alimenta y la interfaz lo observa por
/// * [estado]. Todo se guarda por persona ([abrirSesion]), así que dos cuentas en un teléfono no se mezclan.
/// * Quien manda es el servidor: al volver internet cada acción pendiente se envía como una petición normal y, si
/// * el servidor la rechaza (4xx), se descarta y se le avisa a la persona ([AccionRechazada]).
class NucleoOffline {
  static const claveCola = 'cola';
  static const claveRechazadas = 'rechazadas';
  static const claveCompletadas = 'completadas';
  static const _prefijoCache = 'GET ';
  static const _maxRechazadas = 30;

  final AlmacenOffline _almacen;
  final Dio Function() _dio;
  final bool Function() _hayToken;
  final Future<bool> Function() _sondeo;
  final Duration _cadaSondeo;
  final DateTime Function() _ahora;

  /// Se llama cuando conviene volver a pedir los datos a la red (volvió internet, o se enviaron acciones).
  void Function()? alCambiarDatos;

  /// Revisores de la respuesta por tipo de acción (ver [AccionPendiente.tipo]). Los registra quien conoce el
  /// formato de la respuesta (la feature), no el núcleo.
  final Map<String, VerificadorDeRespuesta> verificadores = {};

  /// Lo que observa la interfaz.
  final ValueNotifier<EstadoOffline> estado = ValueNotifier(const EstadoOffline());

  String? _usuario;
  List<AccionPendiente> _cola = [];
  List<AccionRechazada> _rechazadas = [];
  List<AccionCompletada> _completadas = [];
  bool _sincronizando = false;
  Timer? _temporizadorSondeo;
  int _contador = 0;
  Future<void> _escrituras = Future.value();

  /// * [dio]: el cliente con el que se reenvían las acciones pendientes.
  /// * [hayToken]: sin token no se sincroniza (arranque sin internet): se espera a que la sesión se renueve.
  /// * [sondeo]: pregunta si el servidor responde; mientras no hay conexión se repite cada [cadaSondeo].
  NucleoOffline({
    required AlmacenOffline almacen,
    required Dio Function() dio,
    required bool Function() hayToken,
    required Future<bool> Function() sondeo,
    Duration cadaSondeo = const Duration(seconds: 15),
    DateTime Function()? ahora,
  }) : _almacen = almacen,
       _dio = dio,
       _hayToken = hayToken,
       _sondeo = sondeo,
       _cadaSondeo = cadaSondeo,
       _ahora = ahora ?? DateTime.now;

  bool get sinConexion => estado.value.sinConexion;

  /// Hay una persona con sesión abierta: sin ella no hay dónde guardar nada.
  bool get conSesion => _usuario != null;
  List<AccionPendiente> get pendientes => List.unmodifiable(_cola);

  // * ---- Sesión ----

  /// Empieza a trabajar con los datos de esta persona: carga su cola y sus avisos guardados.
  Future<void> abrirSesion(String usuarioCodigo) async {
    if (_usuario == usuarioCodigo) return;
    _usuario = usuarioCodigo;
    _cola = _leerLista(await _almacen.leer(_espacio, claveCola), AccionPendiente.fromJson);
    _rechazadas = _leerLista(await _almacen.leer(_espacio, claveRechazadas), AccionRechazada.fromJson);
    _completadas = _leerLista(await _almacen.leer(_espacio, claveCompletadas), AccionCompletada.fromJson);
    _emitir();
  }

  /// La persona cerró sesión a propósito: se borra todo lo suyo (caché, pendientes y avisos).
  ///
  /// ! Una sesión perdida por el servidor (token vencido) NO pasa por acá: sus pendientes se conservan.
  Future<void> cerrarSesion() async {
    final usuario = _usuario;
    _usuario = null;
    _cola = [];
    _rechazadas = [];
    _completadas = [];
    _emitir();
    if (usuario != null) await _almacen.borrarEspacio(_espacioDe(usuario));
  }

  String get _espacio => _espacioDe(_usuario!);
  String _espacioDe(String usuario) => 'u_$usuario';

  // * ---- Conexión ----

  /// Una petición no pudo salir. Desde acá se sondea al servidor hasta que responda.
  void marcarSinConexion() {
    if (!estado.value.sinConexion) {
      estado.value = estado.value.copiar(sinConexion: true);
    }
    _temporizadorSondeo ??= Timer.periodic(_cadaSondeo, (_) => unawaited(sondear()));
  }

  /// Una petición llegó al servidor: hay conexión. Si no la había, se sincroniza y se refrescan los datos.
  void marcarConexion() {
    if (!estado.value.sinConexion) return;
    _temporizadorSondeo?.cancel();
    _temporizadorSondeo = null;
    estado.value = estado.value.copiar(sinConexion: false);
    unawaited(_alRecuperarConexion());
  }

  /// Pregunta al servidor si responde; si sí, da la conexión por recuperada. Lo usa el temporizador mientras no
  /// hay conexión y el aviso del sistema de que la red cambió.
  Future<void> sondear() async {
    bool responde;
    try {
      responde = await _sondeo();
    } catch (_) {
      responde = false;
    }
    if (responde) marcarConexion();
  }

  Future<void> _alRecuperarConexion() async {
    // * Si ya se está sincronizando (la conexión se notó con la primera acción enviada), esa sincronización
    // * refresca los datos al terminar: hacerlo también acá pediría todo dos veces.
    if (_sincronizando) return;
    await sincronizar();
    alCambiarDatos?.call();
  }

  // * ---- Caché de lecturas ----

  /// Lo último que respondió el servidor a esta lectura, o `null` si no hay (o no hay sesión).
  Future<Object?> leerCache(String clave) async {
    if (_usuario == null) return null;
    return _almacen.leer(_espacio, _prefijoCache + clave);
  }

  Future<void> guardarCache(String clave, Object valor) async {
    final usuario = _usuario;
    if (usuario == null) return;
    await _almacen.escribir(_espacioDe(usuario), _prefijoCache + clave, valor);
  }

  // * ---- Cola de acciones ----

  /// Guarda una acción para enviarla cuando vuelva internet.
  Future<AccionPendiente> encolar({
    required String metodo,
    required String ruta,
    required String descripcion,
    Object? cuerpo,
    String? tipo,
  }) async {
    final ahora = _ahora();
    final accion = AccionPendiente(
      id: '${ahora.microsecondsSinceEpoch}-${_contador++}',
      metodo: metodo,
      ruta: ruta,
      cuerpo: cuerpo,
      descripcion: descripcion,
      creadaEn: ahora,
      tipo: tipo,
    );
    _cola = [..._cola, accion];
    await _persistir();
    _emitir();
    return accion;
  }

  /// La persona ya leyó el resultado de lo que hizo sin conexión: se borran los avisos de lo cumplido y de lo
  /// rechazado.
  Future<void> descartarResultados() async {
    _rechazadas = [];
    _completadas = [];
    await _persistir();
    _emitir();
  }

  /// Quita un aviso de rechazo (la persona ya lo leyó).
  Future<void> descartarRechazada(String id) async {
    _rechazadas = _rechazadas.where((r) => r.id != id).toList();
    await _persistir();
    _emitir();
  }

  /// Envía las acciones pendientes, de a una y en orden, hasta que se acaben o algo corte la secuencia.
  ///
  /// * Éxito: sale de la cola. Rechazo del servidor (4xx con respuesta): sale de la cola y queda como rechazada.
  /// * Sin red, sesión vencida (401), 429 o 5xx: se detiene y la acción se conserva para el próximo intento.
  /// * Una sola sincronización a la vez.
  Future<void> sincronizar() async {
    if (_sincronizando || _usuario == null || _cola.isEmpty || !_hayToken()) return;
    _sincronizando = true;
    _emitir();
    var envio = false;
    try {
      while (_cola.isNotEmpty) {
        final accion = _cola.first;
        try {
          final respuesta = await _dio().request<dynamic>(
            accion.ruta,
            data: accion.cuerpo,
            options: Options(method: accion.metodo, extra: {'sinOffline': true}),
          );
          _cola = _cola.sublist(1);
          envio = true;
          _completadas = [
            AccionCompletada(id: accion.id, descripcion: accion.descripcion, completadaEn: _ahora()),
            ..._completadas,
          ].take(_maxRechazadas).toList();
          _registrarObservaciones(accion, respuesta.data);
        } on DioException catch (e) {
          if (!_esRechazo(e)) {
            if (_fueDeRed(e)) marcarSinConexion();
            break;
          }
          _cola = _cola.sublist(1);
          _rechazadas = [_rechazoDe(accion, e), ..._rechazadas].take(_maxRechazadas).toList();
        }
        await _persistir();
        _emitir();
      }
    } finally {
      _sincronizando = false;
      await _persistir();
      _emitir();
    }
    if (envio) alCambiarDatos?.call();
  }

  /// Una acción que el servidor respondió con 200 puede haber cumplido solo una parte (reservar 3 franjas y que 1
  /// ya esté tomada): lo no cumplido se avisa igual que un rechazo completo.
  void _registrarObservaciones(AccionPendiente accion, Object? respuesta) {
    final verificador = accion.tipo == null ? null : verificadores[accion.tipo];
    if (verificador == null) return;
    final List<ObservacionRechazo> observaciones;
    try {
      observaciones = verificador(respuesta);
    } catch (_) {
      // * Una respuesta que no se entiende no debe frenar la cola: la acción ya se envió
      return;
    }
    final ahora = _ahora();
    final nuevas = [
      for (var i = 0; i < observaciones.length; i++)
        AccionRechazada(
          id: '${accion.id}-$i',
          descripcion: accion.descripcion,
          motivo: observaciones[i].detalle,
          codigo: observaciones[i].codigo,
          rechazadaEn: ahora,
        ),
    ];
    _rechazadas = [...nuevas, ..._rechazadas].take(_maxRechazadas).toList();
  }

  /// El servidor respondió y dijo que no (400, 403, 404, 409, 423...): reintentar no cambiaría nada.
  bool _esRechazo(DioException e) {
    final codigo = e.response?.statusCode;
    if (codigo == null || codigo == 401 || codigo == 429 || codigo >= 500) return false;
    return codigo >= 400;
  }

  AccionRechazada _rechazoDe(AccionPendiente accion, DioException e) {
    final error = e.error;
    return AccionRechazada(
      id: accion.id,
      descripcion: accion.descripcion,
      motivo: error is ApiException ? error.message : 'El servidor no aceptó la acción.',
      codigo: error is ApiException ? error.code : null,
      rechazadaEn: _ahora(),
    );
  }

  /// Se vuelve a usar en el interceptor: ¿el fallo es de red (la petición no llegó o no se pudo completar)?
  static bool esFalloDeRed(DioException e) =>
      e.response == null &&
      (e.type == DioExceptionType.connectionError ||
          e.type == DioExceptionType.connectionTimeout ||
          e.type == DioExceptionType.sendTimeout ||
          e.type == DioExceptionType.receiveTimeout);

  bool _fueDeRed(DioException e) => esFalloDeRed(e);

  // * ---- Persistencia y estado ----

  Future<void> _persistir() {
    final usuario = _usuario;
    if (usuario == null) return _escrituras;
    final cola = _cola.map((a) => a.toJson()).toList();
    final rechazadas = _rechazadas.map((a) => a.toJson()).toList();
    final completadas = _completadas.map((a) => a.toJson()).toList();
    // * En serie: dos escrituras a la vez del mismo archivo se pisarían
    return _escrituras = _escrituras.then((_) async {
      await _almacen.escribir(_espacioDe(usuario), claveCola, cola);
      await _almacen.escribir(_espacioDe(usuario), claveRechazadas, rechazadas);
      await _almacen.escribir(_espacioDe(usuario), claveCompletadas, completadas);
    });
  }

  void _emitir() {
    estado.value = estado.value.copiar(
      pendientes: List.unmodifiable(_cola),
      rechazadas: List.unmodifiable(_rechazadas),
      completadas: List.unmodifiable(_completadas),
      sincronizando: _sincronizando,
    );
  }

  static List<T> _leerLista<T>(Object? crudo, T Function(Map<String, dynamic>) desde) {
    if (crudo is! List) return [];
    final salida = <T>[];
    for (final item in crudo) {
      try {
        salida.add(desde(Map<String, dynamic>.from(item as Map)));
      } catch (_) {
        // * Un elemento corrupto se omite: no se pierde toda la cola por uno
      }
    }
    return salida;
  }

  void dispose() {
    _temporizadorSondeo?.cancel();
    estado.dispose();
  }
}
