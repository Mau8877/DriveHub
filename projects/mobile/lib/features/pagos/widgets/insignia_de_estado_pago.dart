import 'package:flutter/material.dart';

import '../../../shared/widgets/insignia.dart';
import '../models/estado_pago_visible.dart';

/// El estado de una compra como insignia (CU28): el texto va en español, el color solo lo refuerza.
class InsigniaDeEstadoPago extends StatelessWidget {
  final EstadoPagoVisible estado;

  const InsigniaDeEstadoPago(this.estado, {super.key});

  @override
  Widget build(BuildContext context) {
    final tono = switch (estado) {
      EstadoPagoVisible.confirmado => TonoInsignia.exito,
      EstadoPagoVisible.pendiente => TonoInsignia.advertencia,
      EstadoPagoVisible.reembolsoEnProceso ||
      EstadoPagoVisible.reembolsado => TonoInsignia.info,
      EstadoPagoVisible.fallido ||
      EstadoPagoVisible.desconocido => TonoInsignia.neutro,
    };
    return Insignia(estado.etiqueta, tono: tono);
  }
}
