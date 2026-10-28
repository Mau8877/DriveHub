import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/tutorial.dart';
import '../models/tutorial_request.dart';
import '../services/tutoriales_service.dart';

/// Servicio de los tutoriales; es un provider aparte para poder sustituirlo en tests.
final tutorialesServiceProvider = Provider<TutorialesService>(
  (ref) => TutorialesService(),
);

// -- Los tutoriales del instructor (CU33) -------------------------------------------------------------------------

/// El estado elegido en «Mis tutoriales»; `null` = todos.
class MisTutorialesFiltroUiNotifier extends Notifier<EstadoTutorial?> {
  @override
  EstadoTutorial? build() => null;

  void elegir(EstadoTutorial? estado) => state = estado;
}

final misTutorialesFiltroUiProvider =
    NotifierProvider<MisTutorialesFiltroUiNotifier, EstadoTutorial?>(
      MisTutorialesFiltroUiNotifier.new,
    );

/// Los tutoriales del instructor según el filtro (página 1 de 50). `autoDispose`: al abrir la pestaña se piden de
/// nuevo.
final misTutorialesListProvider = FutureProvider.autoDispose<List<Tutorial>>((
  ref,
) async {
  final estado = ref.watch(misTutorialesFiltroUiProvider);
  final pagina = await ref
      .watch(tutorialesServiceProvider)
      .mios(estado: estado);
  return pagina.items;
});

/// Las acciones del instructor sobre sus tutoriales (CU33). Cada una deja la lista al día al terminar.
///
/// * Un error de negocio llega como `ApiException` y se deja subir: la pantalla muestra su mensaje.
class TutorialAcciones {
  final Ref _ref;

  TutorialAcciones(this._ref);

  TutorialesService get _servicio => _ref.read(tutorialesServiceProvider);

  Future<ArchivoSubido> subirPdf(Uint8List bytes, {required String nombre}) =>
      _servicio.subirPdf(bytes, nombre: nombre);

  Future<Tutorial> crear(TutorialRequest request) =>
      _actualizar(_servicio.crear(request));

  Future<Tutorial> editar(String codigo, TutorialRequest request) =>
      _actualizar(_servicio.editar(codigo, request));

  Future<Tutorial> publicar(String codigo) =>
      _actualizar(_servicio.publicar(codigo));

  Future<Tutorial> archivar(String codigo) =>
      _actualizar(_servicio.archivar(codigo));

  Future<Tutorial> _actualizar(Future<Tutorial> accion) async {
    final tutorial = await accion;
    _ref.invalidate(misTutorialesListProvider);
    return tutorial;
  }
}

final tutorialAccionesProvider = Provider<TutorialAcciones>(
  TutorialAcciones.new,
);

// -- La biblioteca de tutoriales publicados (CU35) ----------------------------------------------------------------

/// La búsqueda y el tipo elegidos en la biblioteca; `null` = sin filtro.
class BibliotecaFiltroUi {
  final String busqueda;
  final TipoTutorial? tipo;

  const BibliotecaFiltroUi({this.busqueda = '', this.tipo});
}

class BibliotecaFiltroUiNotifier extends Notifier<BibliotecaFiltroUi> {
  @override
  BibliotecaFiltroUi build() => const BibliotecaFiltroUi();

  void buscar(String texto) =>
      state = BibliotecaFiltroUi(busqueda: texto, tipo: state.tipo);

  void elegirTipo(TipoTutorial? tipo) =>
      state = BibliotecaFiltroUi(busqueda: state.busqueda, tipo: tipo);
}

final bibliotecaFiltroUiProvider =
    NotifierProvider<BibliotecaFiltroUiNotifier, BibliotecaFiltroUi>(
      BibliotecaFiltroUiNotifier.new,
    );

/// Los tutoriales publicados para quien pide, según el filtro (página 1 de 50).
final bibliotecaTutorialesProvider = FutureProvider.autoDispose<List<Tutorial>>(
  (ref) async {
    final filtro = ref.watch(bibliotecaFiltroUiProvider);
    final pagina = await ref
        .watch(tutorialesServiceProvider)
        .publicados(busqueda: filtro.busqueda, tipo: filtro.tipo);
    return pagina.items;
  },
);
