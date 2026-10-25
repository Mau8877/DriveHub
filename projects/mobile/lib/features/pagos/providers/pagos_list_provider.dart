import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/pago_resumen.dart';
import 'pagos_filtro_ui_provider.dart';
import 'pagos_service_provider.dart';

const int _tamanoDePagina = 20;

/// Lo ya cargado del historial de compras (CU28) y cómo seguir.
class PagosListado {
  final List<PagoResumen> items;
  final int page;
  final int totalPages;
  final bool cargandoMas;

  /// El error del último «Cargar más», si falló: lo ya cargado se conserva y se puede reintentar.
  final Object? errorAlCargarMas;

  const PagosListado({
    required this.items,
    required this.page,
    required this.totalPages,
    this.cargandoMas = false,
    this.errorAlCargarMas,
  });

  bool get hayMas => page < totalPages;

  PagosListado copiar({bool? cargandoMas, Object? errorAlCargarMas}) =>
      PagosListado(
        items: items,
        page: page,
        totalPages: totalPages,
        cargandoMas: cargandoMas ?? this.cargandoMas,
        errorAlCargarMas: errorAlCargarMas,
      );
}

/// Las compras del estudiante, de a 20, con el filtro de estado.
///
/// * Cambiar el filtro reinicia la lista (la reconstruye desde la primera página).
/// * [cargarMas] AGREGA la siguiente página; no pide una página dos veces a la vez ni más allá de la última.
/// * `autoDispose`: al cerrar sesión se descarta y otra persona no ve las compras anteriores.
class PagosListNotifier extends AutoDisposeAsyncNotifier<PagosListado> {
  @override
  Future<PagosListado> build() async {
    final estado = ref.watch(pagosFiltroUiProvider);
    final pagina = await ref
        .read(pagosServiceProvider)
        .listarPagos(estado: estado, pageSize: _tamanoDePagina);
    return PagosListado(
      items: pagina.items,
      page: pagina.page,
      totalPages: pagina.totalPages,
    );
  }

  Future<void> cargarMas() async {
    final actual = state.valueOrNull;
    if (actual == null || !actual.hayMas || actual.cargandoMas) return;

    final filtro = ref.read(pagosFiltroUiProvider);
    state = AsyncData(actual.copiar(cargandoMas: true));
    try {
      final pagina = await ref
          .read(pagosServiceProvider)
          .listarPagos(
            estado: filtro,
            page: actual.page + 1,
            pageSize: _tamanoDePagina,
          );
      // * Si el filtro cambió mientras tanto, esta página es de otra lista: la lista nueva ya se está armando
      if (ref.read(pagosFiltroUiProvider) != filtro) return;
      final vistos = actual.items.map((p) => p.codigo).toSet();
      state = AsyncData(
        PagosListado(
          items: [
            ...actual.items,
            ...pagina.items.where((p) => !vistos.contains(p.codigo)),
          ],
          page: pagina.page,
          totalPages: pagina.totalPages,
        ),
      );
    } catch (e) {
      if (ref.read(pagosFiltroUiProvider) != filtro) return;
      state = AsyncData(actual.copiar(errorAlCargarMas: e));
    }
  }

  /// Vuelve a pedir la primera página.
  Future<void> refrescar() async {
    ref.invalidateSelf();
    await future;
  }
}

final pagosListProvider =
    AsyncNotifierProvider.autoDispose<PagosListNotifier, PagosListado>(
      PagosListNotifier.new,
    );
