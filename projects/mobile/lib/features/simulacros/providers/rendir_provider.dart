import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/intento_iniciado.dart';
import '../models/intento_resultado.dart';
import 'simulacros_list_provider.dart';
import 'simulacros_service_provider.dart';

/// El intento que se está rindiendo (CU21): las preguntas, lo que lleva marcado y en qué pregunta está.
class RendirEstado {
  final IntentoIniciado intento;

  /// Cuándo recibió la app las preguntas. El tiempo se cuenta desde acá y no desde la hora del servidor, para que
  /// un reloj mal puesto en el teléfono no acorte ni alargue el intento.
  final DateTime recibidoEn;

  /// Código de la pregunta -> ids de las opciones marcadas.
  final Map<String, Set<String>> elegidas;
  final int indice;

  const RendirEstado({
    required this.intento,
    required this.recibidoEn,
    this.elegidas = const {},
    this.indice = 0,
  });

  PreguntaParaRendir get actual => intento.preguntas[indice];
  bool get esUltima => indice == intento.preguntas.length - 1;
  int get respondidas => elegidas.values.where((v) => v.isNotEmpty).length;
  DateTime get venceEn =>
      recibidoEn.add(Duration(minutes: intento.duracionMin));

  RendirEstado copiar({Map<String, Set<String>>? elegidas, int? indice}) =>
      RendirEstado(
        intento: intento,
        recibidoEn: recibidoEn,
        elegidas: elegidas ?? this.elegidas,
        indice: indice ?? this.indice,
      );
}

/// Rendir un simulacro: iniciar, marcar respuestas, moverse entre preguntas y finalizar (CU21).
///
/// * `null` es que no hay ningún intento en curso en la app.
/// * Los errores de la llamada se propagan: la pantalla decide cómo mostrarlos.
class RendirNotifier extends Notifier<RendirEstado?> {
  @override
  RendirEstado? build() => null;

  Future<void> iniciar(String simulacroCodigo) async {
    final intento = await ref
        .read(simulacrosServiceProvider)
        .iniciar(simulacroCodigo);
    state = RendirEstado(intento: intento, recibidoEn: DateTime.now());
  }

  /// Marca o desmarca una opción: en una pregunta de varias respuestas se agrega o quita; en las demás reemplaza.
  void elegir(String preguntaCodigo, String opcionId) {
    final actual = state;
    if (actual == null) return;
    final pregunta = actual.intento.preguntas.firstWhere(
      (p) => p.codigo == preguntaCodigo,
    );
    final previas = actual.elegidas[preguntaCodigo] ?? const <String>{};
    final nuevas = pregunta.tipo.admiteVarias
        ? (previas.contains(opcionId)
              ? ({...previas}..remove(opcionId))
              : {...previas, opcionId})
        : {opcionId};
    state = actual.copiar(
      elegidas: {...actual.elegidas, preguntaCodigo: nuevas},
    );
  }

  void irA(int indice) {
    final actual = state;
    if (actual == null ||
        indice < 0 ||
        indice >= actual.intento.preguntas.length) {
      return;
    }
    state = actual.copiar(indice: indice);
  }

  /// Envía las respuestas y devuelve la corrección. Si falla (sin red), el intento sigue en pantalla para reintentar.
  Future<IntentoResultado> finalizar() async {
    final actual = state!;
    final resultado = await ref
        .read(simulacrosServiceProvider)
        .finalizar(actual.intento.codigo, actual.elegidas);
    state = null;
    ref.invalidate(intentosHistorialProvider);
    return resultado;
  }

  /// Descarta el intento de la app (no lo cierra en el servidor: seguirá contando su tiempo).
  void descartar() => state = null;
}

final rendirProvider = NotifierProvider<RendirNotifier, RendirEstado?>(
  RendirNotifier.new,
);
