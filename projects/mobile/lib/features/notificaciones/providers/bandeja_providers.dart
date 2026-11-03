import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../auth/providers/sesion_provider.dart';
import '../models/notificacion.dart';
import '../models/preferencias_notificacion.dart';
import '../services/notificaciones_service.dart';

const int _tamanoDePagina = 20;

/// El código con el que el backend responde «el aviso no existe, ya expiró o es de otra persona».
const String _avisoNoEncontrado = 'ERR_NOT_04';

/// Servicio de la bandeja; es un provider aparte para poder sustituirlo en tests.
final notificacionesServiceProvider = Provider<NotificacionesService>(
  (ref) => NotificacionesService(),
);

/// Qué avisos muestra la bandeja: «No leídas» y/o un tipo. Sin nada, todos.
@immutable
class BandejaFiltro {
  final bool soloNoLeidas;
  final TipoNotificacion? tipo;

  const BandejaFiltro({this.soloNoLeidas = false, this.tipo});

  bool get esTodas => !soloNoLeidas && tipo == null;

  @override
  bool operator ==(Object other) =>
      other is BandejaFiltro &&
      other.soloNoLeidas == soloNoLeidas &&
      other.tipo == tipo;

  @override
  int get hashCode => Object.hash(soloNoLeidas, tipo);
}

/// El filtro elegido en la pantalla. Se descarta al salir de la bandeja.
class BandejaFiltroUiNotifier extends AutoDisposeNotifier<BandejaFiltro> {
  @override
  BandejaFiltro build() => const BandejaFiltro();

  void todas() => state = const BandejaFiltro();

  /// «No leídas» se combina con el tipo que ya estuviera elegido.
  void alternarNoLeidas() =>
      state = BandejaFiltro(soloNoLeidas: !state.soloNoLeidas, tipo: state.tipo);

  /// Elegir el mismo tipo otra vez lo quita.
  void elegirTipo(TipoNotificacion tipo) => state = BandejaFiltro(
    soloNoLeidas: state.soloNoLeidas,
    tipo: state.tipo == tipo ? null : tipo,
  );
}

final bandejaFiltroUiProvider =
    AutoDisposeNotifierProvider<BandejaFiltroUiNotifier, BandejaFiltro>(
      BandejaFiltroUiNotifier.new,
    );

/// Lo ya cargado de la bandeja y cómo seguir.
class Bandeja {
  final List<Notificacion> items;
  final int page;
  final int totalPages;
  final bool cargandoMas;

  /// El error de la última página que no cargó: lo ya cargado se conserva y se puede reintentar.
  final Object? errorAlCargarMas;

  const Bandeja({
    required this.items,
    required this.page,
    required this.totalPages,
    this.cargandoMas = false,
    this.errorAlCargarMas,
  });

  bool get hayMas => page < totalPages;

  Bandeja copiar({
    List<Notificacion>? items,
    bool? cargandoMas,
    Object? errorAlCargarMas,
  }) => Bandeja(
    items: items ?? this.items,
    page: page,
    totalPages: totalPages,
    cargandoMas: cargandoMas ?? this.cargandoMas,
    errorAlCargarMas: errorAlCargarMas,
  );
}

/// La bandeja de avisos de la persona, de a 20 (CU38).
///
/// * Cambiar el filtro, o la persona de la sesión, vuelve a pedir la primera página.
/// * [cargarMas] AGREGA la siguiente página; si falla, conserva lo ya cargado y no deja el estado en error.
/// * [marcarLeida] cambia la lista enseguida; si el servicio falla, vuelve a dejar el aviso sin leer (así se
///   reintenta la próxima vez que se abra). Con `ERR_NOT_04` el aviso se quita de la lista.
/// * `autoDispose`: al salir de la bandeja se descarta, y al volver se pide de nuevo.
class BandejaNotifier extends AutoDisposeAsyncNotifier<Bandeja> {
  @override
  Future<Bandeja> build() async {
    ref.watch(sesionProvider.select((s) => s.usuario?.codigo));
    final filtro = ref.watch(bandejaFiltroUiProvider);

    final pagina = await ref
        .read(notificacionesServiceProvider)
        .listar(
          pageSize: _tamanoDePagina,
          soloNoLeidas: filtro.soloNoLeidas,
          tipo: filtro.tipo,
        );
    return Bandeja(
      items: pagina.items,
      page: pagina.page,
      totalPages: pagina.totalPages,
    );
  }

  Future<void> cargarMas() async {
    final actual = state.valueOrNull;
    if (actual == null || !actual.hayMas || actual.cargandoMas) return;

    final filtro = ref.read(bandejaFiltroUiProvider);
    state = AsyncData(actual.copiar(cargandoMas: true));
    try {
      final pagina = await ref
          .read(notificacionesServiceProvider)
          .listar(
            page: actual.page + 1,
            pageSize: _tamanoDePagina,
            soloNoLeidas: filtro.soloNoLeidas,
            tipo: filtro.tipo,
          );
      // * Si el filtro cambió mientras tanto, esta página es de otra lista: la nueva ya se está armando
      if (ref.read(bandejaFiltroUiProvider) != filtro) return;
      final vistos = actual.items.map((n) => n.codigoNotificacion).toSet();
      state = AsyncData(
        Bandeja(
          items: [
            ...actual.items,
            ...pagina.items.where((n) => !vistos.contains(n.codigoNotificacion)),
          ],
          page: pagina.page,
          totalPages: pagina.totalPages,
        ),
      );
    } catch (e) {
      if (ref.read(bandejaFiltroUiProvider) != filtro) return;
      state = AsyncData(actual.copiar(errorAlCargarMas: e));
    }
  }

  /// Vuelve a pedir la primera página.
  Future<void> recargar() async {
    ref.invalidateSelf();
    await future;
  }

  /// Marca el aviso como leído. Uno que ya lo estaba no llama al servicio. Un rechazo del backend
  /// ([ApiException]) o un fallo de red se relanza para que la pantalla lo avise.
  Future<void> marcarLeida(String codigoNotificacion) async {
    final actual = state.valueOrNull;
    final aviso = actual?.items
        .where((n) => n.codigoNotificacion == codigoNotificacion)
        .firstOrNull;
    if (actual == null || aviso == null || aviso.leida) return;

    state = AsyncData(actual.copiar(items: _conLeida(actual.items, codigoNotificacion, true)));
    try {
      await ref.read(notificacionesServiceProvider).leer(codigoNotificacion);
      ref.invalidate(conteoNoLeidasProvider);
    } catch (e) {
      final ahora = state.valueOrNull ?? actual;
      if (e is ApiException && e.code == _avisoNoEncontrado) {
        state = AsyncData(
          ahora.copiar(
            items: ahora.items
                .where((n) => n.codigoNotificacion != codigoNotificacion)
                .toList(),
          ),
        );
        ref.invalidate(conteoNoLeidasProvider);
      } else {
        state = AsyncData(
          ahora.copiar(items: _conLeida(ahora.items, codigoNotificacion, false)),
        );
      }
      rethrow;
    }
  }

  /// Deja todos los avisos leídos y devuelve cuántos estaban pendientes.
  Future<int> marcarTodas() async {
    final marcadas = await ref.read(notificacionesServiceProvider).leerTodas();
    final actual = state.valueOrNull;
    if (actual != null) {
      state = AsyncData(
        actual.copiar(items: [for (final n in actual.items) n.copiar(leida: true)]),
      );
    }
    ref.invalidate(conteoNoLeidasProvider);
    return marcadas;
  }

  List<Notificacion> _conLeida(List<Notificacion> items, String codigo, bool leida) => [
    for (final n in items)
      n.codigoNotificacion == codigo ? n.copiar(leida: leida) : n,
  ];
}

final bandejaProvider =
    AsyncNotifierProvider.autoDispose<BandejaNotifier, Bandeja>(
      BandejaNotifier.new,
    );

/// Cuántos avisos sin leer tiene la persona: la insignia de la campana.
///
/// * Si no se puede saber (sin red, sin sesión) es `null` y la campana no muestra insignia: no es un error.
/// * Es de UNA persona: si cambia la sesión se vuelve a pedir. Se invalida al marcar avisos, al volver de la
///   bandeja y al volver la app del segundo plano.
final conteoNoLeidasProvider = FutureProvider<int?>((ref) async {
  final codigo = ref.watch(sesionProvider.select((s) => s.usuario?.codigo));
  if (codigo == null) return null;
  try {
    return await ref.read(notificacionesServiceProvider).contarNoLeidas();
  } catch (_) {
    return null;
  }
});
