import 'package:flutter/material.dart';

import '../models/reserva.dart';
import 'formato_clases.dart';

/// Las acciones de una clase de «Mis clases» (CU15): «Cancelar» y «Reprogramar».
///
/// * «Cancelar» se ofrece mientras la clase no haya empezado según el teléfono (solo UX: el servidor decide).
/// * «Reprogramar» solo si el servidor dice que todavía está a tiempo ([Reserva.cancelacionATiempo]) y la pantalla
///   le pasó qué hacer; sin [alReprogramar] no se dibuja.
/// * No usa Riverpod: recibe qué hacer, así se prueba sin montar nada más.
class AccionesReserva extends StatelessWidget {
  final Reserva reserva;
  final VoidCallback alCancelar;
  final VoidCallback? alReprogramar;

  /// Para probar con una hora fija; en la app es la del teléfono.
  final DateTime? ahora;

  const AccionesReserva({
    super.key,
    required this.reserva,
    required this.alCancelar,
    this.alReprogramar,
    this.ahora,
  });

  @override
  Widget build(BuildContext context) {
    if (laClaseYaEmpezo(reserva, ahora ?? DateTime.now())) return const SizedBox.shrink();
    final tema = Theme.of(context);
    final puedeReprogramar = alReprogramar != null && reserva.cancelacionATiempo;

    return Padding(
      padding: const EdgeInsets.only(top: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          TextButton(
            key: Key('cancelar-${reserva.codigo}'),
            onPressed: alCancelar,
            style: TextButton.styleFrom(foregroundColor: tema.colorScheme.error),
            child: const Text('Cancelar'),
          ),
          if (puedeReprogramar) ...[
            const SizedBox(width: 8),
            FilledButton.tonal(
              key: Key('reprogramar-${reserva.codigo}'),
              onPressed: alReprogramar,
              style: FilledButton.styleFrom(minimumSize: const Size(0, 40)),
              child: const Text('Reprogramar'),
            ),
          ],
        ],
      ),
    );
  }
}
