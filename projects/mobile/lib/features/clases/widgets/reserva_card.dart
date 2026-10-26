import 'package:flutter/material.dart';

import '../../../shared/widgets/insignia.dart';
import '../../horarios/models/fechas.dart';
import '../models/estado_reserva.dart';
import '../models/reserva.dart';
import 'formato_clases.dart';

/// Una clase reservada (CU14): el día, la hora, el instructor y, en el historial, su estado.
///
/// * Los colores salen del tema; el estado va siempre en texto y el color solo lo refuerza.
class ReservaCard extends StatelessWidget {
  final Reserva reserva;

  /// En el historial se muestra la insignia con el estado; en «Mis clases» no hace falta (todas están confirmadas).
  final bool conEstado;

  const ReservaCard({super.key, required this.reserva, this.conEstado = false});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final fecha = reserva.fecha;

    return Card(
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Row(
          children: [
            Container(
              width: 52,
              padding: const EdgeInsets.symmetric(vertical: 8),
              decoration: BoxDecoration(
                color: tema.colorScheme.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Column(
                children: [
                  Text('${fecha.day}', style: tema.textTheme.titleLarge),
                  Text(
                    mesCorto(fecha.month),
                    style: tema.textTheme.labelMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(rangoDeHoras(reserva.horaInicio, reserva.horaFin), style: tema.textTheme.titleMedium),
                  const SizedBox(height: 2),
                  Text(
                    reserva.instructorNombre,
                    style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            ),
            if (conEstado) Insignia(etiquetaDeEstado(reserva), tono: _tono(reserva)),
          ],
        ),
      ),
    );
  }

  static TonoInsignia _tono(Reserva reserva) {
    switch (reserva.estado) {
      case EstadoReserva.completada:
        return TonoInsignia.exito;
      case EstadoReserva.noAsistio:
        return TonoInsignia.advertencia;
      case EstadoReserva.cancelada:
        return TonoInsignia.neutro;
      case EstadoReserva.confirmada:
        return TonoInsignia.info;
    }
  }
}
