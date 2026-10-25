import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/estado_pago_visible.dart';

/// El estado elegido en el filtro del historial (CU28), o ninguno para ver todas las compras.
class PagosFiltroNotifier extends Notifier<EstadoPagoVisible?> {
  @override
  EstadoPagoVisible? build() => null;

  void elegir(EstadoPagoVisible? estado) => state = estado;
}

/// Estado puro de UI: no guarda datos del backend.
final pagosFiltroUiProvider =
    NotifierProvider<PagosFiltroNotifier, EstadoPagoVisible?>(
      PagosFiltroNotifier.new,
    );
