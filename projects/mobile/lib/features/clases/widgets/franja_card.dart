import 'package:flutter/material.dart';

import '../../../core/theme/app_theme.dart';
import '../models/franja_disponible.dart';
import 'formato_clases.dart';

/// Una franja libre que el estudiante puede elegir (CU14): la hora, el instructor y si está elegida.
///
/// * Elegida se marca con borde, fondo suave y una marca de verificación (el color solo refuerza). Si no se pueden
/// * elegir más franjas ([deshabilitada]) se ve apagada, pero sigue respondiendo al toque para explicar por qué.
class FranjaCard extends StatelessWidget {
  final FranjaDisponible franja;
  final bool elegida;
  final bool deshabilitada;
  final VoidCallback alTocar;

  const FranjaCard({
    super.key,
    required this.franja,
    required this.elegida,
    required this.deshabilitada,
    required this.alTocar,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final colores = tema.colorScheme;
    final borde = elegida ? colores.primary : context.cockpit.borde;
    final apagada = deshabilitada && !elegida;

    return Semantics(
      button: true,
      selected: elegida,
      enabled: !apagada,
      label:
          '${rangoDeHoras(franja.horaInicio, franja.horaFin)}, ${franja.instructorNombre}'
          '${elegida ? ', elegida' : ''}${apagada ? ', no disponible para elegir' : ''}',
      child: ExcludeSemantics(
        child: Opacity(
          opacity: apagada ? 0.55 : 1,
          child: Material(
            color: elegida ? Color.alphaBlend(colores.primary.withValues(alpha: 0.10), colores.surface) : colores.surface,
            borderRadius: BorderRadius.circular(AppRadios.control),
            child: InkWell(
              borderRadius: BorderRadius.circular(AppRadios.control),
              onTap: alTocar,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(AppRadios.control),
                  border: Border.all(color: borde, width: elegida ? 2 : 1),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(rangoDeHoras(franja.horaInicio, franja.horaFin), style: tema.textTheme.titleMedium),
                          const SizedBox(height: 2),
                          Text(
                            franja.instructorNombre,
                            style: tema.textTheme.bodyMedium?.copyWith(color: colores.onSurfaceVariant),
                          ),
                        ],
                      ),
                    ),
                    Icon(
                      elegida ? Icons.check_circle : Icons.radio_button_unchecked,
                      color: elegida ? colores.primary : colores.onSurfaceVariant,
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
