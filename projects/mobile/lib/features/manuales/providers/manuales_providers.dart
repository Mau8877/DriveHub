import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/manual.dart';
import '../services/manuales_service.dart';

/// Servicio de los manuales; es un provider aparte para poder sustituirlo en tests.
final manualesServiceProvider = Provider<ManualesService>(
  (ref) => ManualesService(),
);

/// La búsqueda y la categoría elegidas en la lista de manuales (CU34); `null` = sin filtro.
class ManualesFiltroUi {
  final String busqueda;
  final String? categoria;

  const ManualesFiltroUi({this.busqueda = '', this.categoria});
}

class ManualesFiltroUiNotifier extends Notifier<ManualesFiltroUi> {
  @override
  ManualesFiltroUi build() => const ManualesFiltroUi();

  void buscar(String texto) =>
      state = ManualesFiltroUi(busqueda: texto, categoria: state.categoria);

  void elegirCategoria(String? categoria) =>
      state = ManualesFiltroUi(busqueda: state.busqueda, categoria: categoria);
}

final manualesFiltroUiProvider =
    NotifierProvider<ManualesFiltroUiNotifier, ManualesFiltroUi>(
      ManualesFiltroUiNotifier.new,
    );

/// Los manuales publicados según el filtro (página 1 de 50). `autoDispose`: al abrir la pestaña se piden de nuevo,
/// así uno recién publicado aparece sin recargar nada.
final manualesListProvider = FutureProvider.autoDispose<List<Manual>>((
  ref,
) async {
  final filtro = ref.watch(manualesFiltroUiProvider);
  final pagina = await ref
      .watch(manualesServiceProvider)
      .publicados(busqueda: filtro.busqueda, categoria: filtro.categoria);
  return pagina.items;
});
