import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/hora_alumno.dart';
import 'alumnos_busqueda_provider.dart';
import 'horas_alumnos_service_provider.dart';

const int _tamanoDePagina = 20;

/// Cuánto se espera desde la última tecla antes de pedir la búsqueda: no se llama en cada letra.
const Duration esperaDeBusqueda = Duration(milliseconds: 400);

/// Lo ya cargado de «Mis alumnos» y cómo seguir.
class AlumnosListado {
  final List<HoraAlumno> items;
  final int page;
  final int totalPages;
  final bool cargandoMas;

  /// El error del último «Cargar más», si falló: lo ya cargado se conserva y se puede reintentar.
  final Object? errorAlCargarMas;

  const AlumnosListado({
    required this.items,
    required this.page,
    required this.totalPages,
    this.cargandoMas = false,
    this.errorAlCargarMas,
  });

  bool get hayMas => page < totalPages;

  AlumnosListado copiar({bool? cargandoMas, Object? errorAlCargarMas}) =>
      AlumnosListado(
        items: items,
        page: page,
        totalPages: totalPages,
        cargandoMas: cargandoMas ?? this.cargandoMas,
        errorAlCargarMas: errorAlCargarMas,
      );
}

/// Los alumnos del instructor, de a 20, con búsqueda por nombre (CU29).
///
/// * Cambiar la búsqueda reinicia la lista, esperando [esperaDeBusqueda] desde la última tecla: varias letras
///   seguidas piden una sola vez. Borrar el texto pide enseguida.
/// * [cargarMas] AGREGA la siguiente página; no pide una página dos veces a la vez ni más allá de la última.
/// * La búsqueda la hace el backend, no el dispositivo.
/// * `autoDispose`: al cerrar sesión se descarta y otra persona no ve los alumnos anteriores.
class MisAlumnosNotifier extends AutoDisposeAsyncNotifier<AlumnosListado> {
  @override
  Future<AlumnosListado> build() async {
    final texto = ref.watch(alumnosBusquedaUiProvider).trim();
    if (texto.isNotEmpty) await _esperar();

    final pagina = await ref
        .read(horasAlumnosServiceProvider)
        .listar(search: texto, pageSize: _tamanoDePagina);
    return AlumnosListado(
      items: pagina.items,
      page: pagina.page,
      totalPages: pagina.totalPages,
    );
  }

  /// Espera la pausa de la búsqueda. Si la lista se reconstruye antes (otra tecla), se suelta enseguida: su
  /// resultado se descarta y no llega a pedir nada.
  Future<void> _esperar() {
    final pausa = Completer<void>();
    final temporizador = Timer(esperaDeBusqueda, pausa.complete);
    ref.onDispose(() {
      temporizador.cancel();
      if (!pausa.isCompleted) pausa.complete();
    });
    return pausa.future;
  }

  Future<void> cargarMas() async {
    final actual = state.valueOrNull;
    if (actual == null || !actual.hayMas || actual.cargandoMas) return;

    final texto = ref.read(alumnosBusquedaUiProvider).trim();
    state = AsyncData(actual.copiar(cargandoMas: true));
    try {
      final pagina = await ref
          .read(horasAlumnosServiceProvider)
          .listar(
            search: texto,
            page: actual.page + 1,
            pageSize: _tamanoDePagina,
          );
      // * Si la búsqueda cambió mientras tanto, esta página es de otra lista: la lista nueva ya se está armando
      if (ref.read(alumnosBusquedaUiProvider).trim() != texto) return;
      final vistos = actual.items.map((a) => a.estudianteCodigo).toSet();
      state = AsyncData(
        AlumnosListado(
          items: [
            ...actual.items,
            ...pagina.items.where((a) => !vistos.contains(a.estudianteCodigo)),
          ],
          page: pagina.page,
          totalPages: pagina.totalPages,
        ),
      );
    } catch (e) {
      if (ref.read(alumnosBusquedaUiProvider).trim() != texto) return;
      state = AsyncData(actual.copiar(errorAlCargarMas: e));
    }
  }

  /// Vuelve a pedir la primera página.
  Future<void> refrescar() async {
    ref.invalidateSelf();
    await future;
  }
}

final misAlumnosProvider =
    AsyncNotifierProvider.autoDispose<MisAlumnosNotifier, AlumnosListado>(
      MisAlumnosNotifier.new,
    );
