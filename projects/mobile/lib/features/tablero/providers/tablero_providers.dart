import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../models/analisis_tablero.dart';
import '../models/luz_tablero.dart';
import '../services/analizador_tablero.dart';
import '../services/ayudas_tablero_service.dart';
import '../services/clasificador_tablero.dart';

/// El catálogo de luces que viene en la app (`assets/ml/luces_tablero.json`).
final catalogoTableroProvider = FutureProvider<CatalogoTablero>((ref) async {
  final json = jsonDecode(
    await rootBundle.loadString('assets/ml/luces_tablero.json'),
  );
  return CatalogoTablero.fromJson(json as Map<String, dynamic>);
});

/// El modelo del teléfono; es un provider aparte para sustituirlo en los tests (ninguno carga el `.tflite`).
final clasificadorTableroProvider = Provider<ClasificadorTablero>(
  (ref) => ClasificadorTflite(),
);

final analizadorTableroProvider = Provider<AnalizadorTablero>(
  (ref) =>
      AnalizadorTablero(clasificador: ref.watch(clasificadorTableroProvider)),
);

final ayudasTableroServiceProvider = Provider<AyudasTableroService>(
  (ref) => AyudasTableroService(),
);

/// Abre la cámara o la galería y devuelve la foto, o `null` si la persona canceló.
///
/// * Es un provider para sustituirlo en los tests: ninguno abre la cámara de verdad.
/// * Se pide `maxWidth`/`maxHeight` de 2000: el modelo usa 224 px, no hace falta más.
final selectorDeFotoProvider =
    Provider<Future<Uint8List?> Function(ImageSource origen)>(
      (ref) => (origen) async {
        final foto = await ImagePicker().pickImage(
          source: origen,
          maxWidth: 2000,
          maxHeight: 2000,
        );
        return foto?.readAsBytes();
      },
    );

/// Qué pasó al guardar el análisis en el historial.
enum GuardadoAyuda {
  /// Todavía no se intentó o no corresponde (el modelo no estaba seguro).
  noAplica,
  guardando,
  guardado,

  /// Sin conexión o el servidor falló: el resultado igual se muestra y se puede reintentar.
  noGuardado,
}

/// El resultado de analizar una foto, con la foto original para mostrarla.
class ResultadoTablero {
  final Uint8List foto;
  final AnalisisTablero analisis;
  final double umbral;
  final GuardadoAyuda guardado;

  const ResultadoTablero({
    required this.foto,
    required this.analisis,
    required this.umbral,
    this.guardado = GuardadoAyuda.noAplica,
  });

  /// ¿El modelo está seguro de la luz principal? Si no, la app pide otra foto.
  bool get seguro => analisis.seguro(umbral);

  ResultadoTablero conGuardado(GuardadoAyuda nuevo) => ResultadoTablero(
    foto: foto,
    analisis: analisis,
    umbral: umbral,
    guardado: nuevo,
  );
}

/// Analiza una foto del tablero (CU40).
///
/// * `analizar` no lanza: deja el resultado, o el error (`ImagenInvalida`, `ModeloNoDisponible` u otro) en el
///   estado para que la pantalla lo muestre.
/// * El análisis corre en el teléfono. Si el modelo está seguro, el resultado se guarda en el historial; si el
///   guardado falla (sin conexión), el resultado se queda en pantalla con un aviso y se puede reintentar.
class TableroNotifier extends AsyncNotifier<ResultadoTablero?> {
  @override
  ResultadoTablero? build() => null;

  Future<void> analizar(ImageSource origen, {bool guardarFoto = true}) async {
    final foto = await ref.read(selectorDeFotoProvider)(origen);
    if (foto == null) return;

    state = const AsyncLoading<ResultadoTablero?>();
    try {
      final catalogo = await ref.read(catalogoTableroProvider.future);
      final analisis = await ref
          .read(analizadorTableroProvider)
          .analizar(foto, catalogo, conMiniatura: guardarFoto);
      final resultado = ResultadoTablero(
        foto: foto,
        analisis: analisis,
        umbral: catalogo.umbralConfianza,
      );
      state = AsyncData(resultado);
      if (resultado.seguro) await _guardar(resultado);
    } catch (error, pila) {
      state = AsyncError(error, pila);
    }
  }

  Future<void> reintentarGuardado() async {
    final actual = state.value;
    if (actual == null || actual.guardado == GuardadoAyuda.guardando) return;
    await _guardar(actual);
  }

  void limpiar() => state = const AsyncData(null);

  Future<void> _guardar(ResultadoTablero resultado) async {
    state = AsyncData(resultado.conGuardado(GuardadoAyuda.guardando));
    try {
      await ref.read(ayudasTableroServiceProvider).crear(resultado.analisis);
      state = AsyncData(resultado.conGuardado(GuardadoAyuda.guardado));
    } catch (_) {
      state = AsyncData(resultado.conGuardado(GuardadoAyuda.noGuardado));
    }
  }
}

final tableroProvider =
    AsyncNotifierProvider<TableroNotifier, ResultadoTablero?>(
      TableroNotifier.new,
    );
