import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/franja_disponible.dart';

/// La franja que el estudiante eligió como destino al reprogramar una clase (CU15), o ninguna (estado puro de UI).
///
/// * Es UNA sola franja, no el mapa de varias de «Reservar» (`franjasSeleccionadasUiProvider`).
/// * Elegir no mueve nada: la clase se mueve al confirmar.
class FranjaParaReprogramarNotifier extends Notifier<FranjaDisponible?> {
  @override
  FranjaDisponible? build() => null;

  /// Elige la franja o, si ya era la elegida, la quita.
  void elegir(FranjaDisponible franja) {
    state = state?.codigo == franja.codigo ? null : franja;
  }

  void vaciar() {
    if (state != null) state = null;
  }
}

final franjaParaReprogramarUiProvider = NotifierProvider<FranjaParaReprogramarNotifier, FranjaDisponible?>(
  FranjaParaReprogramarNotifier.new,
);
