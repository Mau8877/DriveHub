import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/resultado.dart';
import '../services/resultados_service.dart';

/// Servicio de los resultados del estudiante; es un provider aparte para poder sustituirlo en tests.
final resultadosServiceProvider = Provider<ResultadosService>(
  (ref) => ResultadosService(),
);

/// El filtro por tipo de evaluación de «Mis notas» y de las notas de los alumnos (CU23); `null` = todas.
class ResultadosFiltroUiNotifier extends Notifier<OrigenResultado?> {
  @override
  OrigenResultado? build() => null;

  void elegir(OrigenResultado? origen) => state = origen;
}

final resultadosFiltroUiProvider =
    NotifierProvider<ResultadosFiltroUiNotifier, OrigenResultado?>(
      ResultadosFiltroUiNotifier.new,
    );

/// Las notas del estudiante según el filtro (página 1 de 50). `autoDispose`: cada vez que se abre la pestaña se
/// vuelven a pedir, así una nota recién cerrada (un simulacro) aparece sin recargar nada.
final resultadosListProvider = FutureProvider.autoDispose<List<Resultado>>((
  ref,
) async {
  final origen = ref.watch(resultadosFiltroUiProvider);
  final pagina = await ref
      .watch(resultadosServiceProvider)
      .mios(origen: origen);
  return pagina.items;
});

/// Las notas de los alumnos del instructor según el filtro (página 1 de 50), con el nombre de cada alumno.
final resultadosAlumnosListProvider =
    FutureProvider.autoDispose<List<Resultado>>((ref) async {
      final origen = ref.watch(resultadosFiltroUiProvider);
      final pagina = await ref
          .watch(resultadosServiceProvider)
          .deMisAlumnos(origen: origen);
      return pagina.items;
    });
