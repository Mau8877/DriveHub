import 'dart:typed_data';

import '../models/analisis_tablero.dart';
import '../models/luz_tablero.dart';
import 'clasificador_tablero.dart';
import 'procesar_imagen.dart';

/// Analiza la foto de un tablero en el teléfono (CU40): la prepara, la pasa por el modelo y une el
/// resultado con el catálogo de luces.
class AnalizadorTablero {
  final ClasificadorTablero clasificador;

  /// Prepara la foto; es un parámetro para sustituirlo en los tests (sin otro hilo).
  final Future<ImagenProcesada> Function(Uint8List bytes) preparar;

  AnalizadorTablero({
    required this.clasificador,
    this.preparar = procesarImagenAparte,
  });

  /// Lanza [ImagenInvalida] si no es una imagen y [ModeloNoDisponible] si el modelo no está.
  ///
  /// * Las detecciones cuyo código no está en [catalogo] se descartan: sin ficha no hay qué explicar.
  /// * [conMiniatura] deja la miniatura en el resultado para guardarla en el historial.
  Future<AnalisisTablero> analizar(
    Uint8List foto,
    CatalogoTablero catalogo, {
    bool conMiniatura = true,
  }) async {
    final imagen = await preparar(foto);
    final reloj = Stopwatch()..start();
    final detecciones = await clasificador.clasificar(imagen.entrada);
    reloj.stop();

    final luces = [
      for (final d in detecciones)
        if (catalogo.luz(d.codigo) case final luz?)
          LuzReconocida(luz: luz, confianza: d.confianza),
    ];
    return AnalisisTablero(
      luces: luces,
      latenciaMs: reloj.elapsedMilliseconds,
      modelo: clasificador.modelo,
      miniatura: conMiniatura ? imagen.miniatura : null,
    );
  }
}
