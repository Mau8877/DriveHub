import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/archivos/abridor_de_pdf.dart';
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

/// Descargar y abrir el PDF de un manual (CU34), por su (código, dirección).
///
/// * `descargarYAbrir` no lanza: devuelve `true` si el PDF se abrió; si no, deja el error en el estado
///   ([SinVisorDePdf] o uno de red) para que la tarjeta lo muestre.
/// * Atiende una descarga a la vez: un segundo toque mientras hay una en curso no hace nada.
/// * Reutiliza el abridor de PDF de los comprobantes (`core/archivos/abridor_de_pdf.dart`).
class ManualDescargaNotifier
    extends AutoDisposeFamilyAsyncNotifier<void, (String, String)> {
  bool _enCurso = false;

  @override
  FutureOr<void> build((String, String) arg) {}

  Future<bool> descargarYAbrir() async {
    if (_enCurso) return false;
    _enCurso = true;
    state = const AsyncLoading<void>();
    try {
      final (codigo, url) = arg;
      final bytes = await ref.read(manualesServiceProvider).descargarPdf(url);
      final abierto = await ref.read(abridorDePdfProvider)(
        bytes,
        'manual-$codigo.pdf',
      );
      if (!abierto) throw const SinVisorDePdf();
      state = const AsyncData(null);
      return true;
    } catch (error, pila) {
      state = AsyncError(error, pila);
      return false;
    } finally {
      _enCurso = false;
    }
  }
}

final manualDescargaProvider = AsyncNotifierProvider.autoDispose
    .family<ManualDescargaNotifier, void, (String, String)>(
      ManualDescargaNotifier.new,
    );
