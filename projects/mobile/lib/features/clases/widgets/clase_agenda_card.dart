import 'package:flutter/material.dart';

import '../../../shared/widgets/insignia.dart';
import '../models/clase_agenda.dart';
import '../models/estado_reserva.dart';
import 'formato_clases.dart';

/// Una clase de la agenda del instructor (CU16): la hora, el estudiante y el estado.
///
/// * Los colores salen del tema; el estado va siempre en texto y el color solo lo refuerza.
/// * Una clase cancelada se ve atenuada con el color secundario del tema, no con opacidad: sigue legible.
/// * «Cancelar» se ofrece solo si el servidor dice que la clase es [ClaseAgenda.cancelable] y la pantalla le pasó qué
///   hacer ([alCancelar]); no usa Riverpod, así se prueba sin montar nada más.
class ClaseAgendaCard extends StatelessWidget {
  final ClaseAgenda clase;
  final VoidCallback? alCancelar;

  const ClaseAgendaCard({super.key, required this.clase, this.alCancelar});

  bool get _cancelada => clase.estado == EstadoReserva.cancelada;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final colorSecundario = tema.colorScheme.onSurfaceVariant;

    return Card(
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    rangoDeHoras(clase.horaInicio, clase.horaFin),
                    style: tema.textTheme.titleMedium?.copyWith(color: _cancelada ? colorSecundario : null),
                  ),
                ),
                const SizedBox(width: 8),
                Insignia(etiquetaDeEstadoAgenda(clase), tono: _tono(clase)),
              ],
            ),
            const SizedBox(height: 6),
            Row(
              children: [
                Icon(Icons.person_outline, size: 18, color: colorSecundario),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    clase.estudianteNombre,
                    style: tema.textTheme.bodyMedium?.copyWith(color: _cancelada ? colorSecundario : null),
                  ),
                ),
              ],
            ),
            if (clase.cancelable && alCancelar != null)
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  key: Key('cancelar-${clase.codigo}'),
                  onPressed: alCancelar,
                  style: TextButton.styleFrom(foregroundColor: tema.colorScheme.error),
                  child: const Text('Cancelar'),
                ),
              ),
          ],
        ),
      ),
    );
  }

  static TonoInsignia _tono(ClaseAgenda clase) {
    switch (clase.estado) {
      case EstadoReserva.completada:
        return TonoInsignia.exito;
      case EstadoReserva.noAsistio:
        return TonoInsignia.advertencia;
      case EstadoReserva.cancelada:
        return TonoInsignia.neutro;
      case EstadoReserva.confirmada:
        return clase.terminada ? TonoInsignia.advertencia : TonoInsignia.info;
    }
  }
}
