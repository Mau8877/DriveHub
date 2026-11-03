import 'package:dio/dio.dart';

import 'package:mobile/core/network/pagina.dart';
import 'package:mobile/features/notificaciones/models/notificacion.dart';
import 'package:mobile/features/notificaciones/models/preferencias_notificacion.dart';
import 'package:mobile/features/notificaciones/providers/bandeja_providers.dart';
import 'package:mobile/features/notificaciones/services/notificaciones_service.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

Notificacion avisoDePrueba(
  int n, {
  bool leida = false,
  TipoNotificacion? tipo = TipoNotificacion.pago,
}) => Notificacion(
  codigoNotificacion: 'n$n',
  tipo: tipo,
  titulo: 'Aviso $n',
  mensaje: 'Mensaje completo del aviso $n',
  leida: leida,
  creadaEn: DateTime(2026, 11, 2, 10).subtract(Duration(minutes: n)),
);

/// Servicio de la bandeja controlado desde el test: responde lo configurado y anota cada llamada.
///
/// * Un `Dio` vacío en el constructor: el real (ApiClient) necesita dotenv cargado.
/// * `listar` filtra y parte en páginas como lo haría el backend.
class NotificacionesServiceFalso extends NotificacionesService {
  NotificacionesServiceFalso() : super(dio: Dio());

  List<Notificacion> avisos = [];
  Object? errorListado;

  /// Si es verdadero, [errorListado] solo se lanza al pedir una página mayor a la 1.
  bool errorListadoSoloDesdeLaPagina2 = false;
  final List<({int page, bool soloNoLeidas, TipoNotificacion? tipo})> listados = [];

  Object? errorConteo;
  int conteos = 0;

  Object? errorAlLeer;
  final List<String> leidos = [];

  Object? errorAlLeerTodas;
  int leerTodasLlamadas = 0;

  @override
  Future<Pagina<Notificacion>> listar({
    int page = 1,
    int pageSize = 20,
    bool soloNoLeidas = false,
    TipoNotificacion? tipo,
  }) async {
    listados.add((page: page, soloNoLeidas: soloNoLeidas, tipo: tipo));
    final error = errorListado;
    if (error != null && (!errorListadoSoloDesdeLaPagina2 || page > 1)) throw error;
    final filtrados = avisos
        .where((a) => (!soloNoLeidas || !a.leida) && (tipo == null || a.tipo == tipo))
        .toList();
    final total = filtrados.length;
    final paginas = total == 0 ? 1 : (total / pageSize).ceil();
    final desde = (page - 1) * pageSize;
    final items = desde >= total
        ? <Notificacion>[]
        : filtrados.sublist(desde, (desde + pageSize).clamp(0, total));
    return Pagina(items: items, page: page, pageSize: pageSize, totalItems: total, totalPages: paginas);
  }

  @override
  Future<int> contarNoLeidas() async {
    conteos++;
    final error = errorConteo;
    if (error != null) throw error;
    return avisos.where((a) => !a.leida).length;
  }

  @override
  Future<Notificacion> leer(String codigoNotificacion) async {
    leidos.add(codigoNotificacion);
    final error = errorAlLeer;
    if (error != null) throw error;
    final i = avisos.indexWhereCodigo(codigoNotificacion);
    avisos[i] = avisos[i].copiar(leida: true);
    return avisos[i];
  }

  @override
  Future<int> leerTodas() async {
    leerTodasLlamadas++;
    final error = errorAlLeerTodas;
    if (error != null) throw error;
    final pendientes = avisos.where((a) => !a.leida).length;
    avisos = [for (final a in avisos) a.copiar(leida: true)];
    return pendientes;
  }
}

extension on List<Notificacion> {
  int indexWhereCodigo(String codigo) => indexWhere((a) => a.codigoNotificacion == codigo);
}

Override notificacionesServiceFalso(NotificacionesServiceFalso falso) =>
    notificacionesServiceProvider.overrideWithValue(falso);
