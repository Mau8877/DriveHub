import 'package:flutter/material.dart';

import '../models/fechas.dart';

/// Los siete círculos L M X J V S D donde se eligen los días de un horario .
///
/// * Cada círculo mide 44 dp, se alterna con un toque y lleva la clave `dia1` (lunes) a `dia7`.
/// * El elegido va en ámbar con texto asfalto; el resto, la superficie con borde suave.
class SelectorDeDias extends StatelessWidget {
  final Set<int> seleccionados;
  final ValueChanged<int> alAlternar;

  const SelectorDeDias({super.key, required this.seleccionados, required this.alAlternar});

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        for (var dia = 1; dia <= 7; dia++)
          _CirculoDeDia(dia: dia, elegido: seleccionados.contains(dia), alTocar: () => alAlternar(dia)),
      ],
    );
  }
}

class _CirculoDeDia extends StatelessWidget {
  final int dia;
  final bool elegido;
  final VoidCallback alTocar;

  const _CirculoDeDia({required this.dia, required this.elegido, required this.alTocar});

  @override
  Widget build(BuildContext context) {
    final esquema = Theme.of(context).colorScheme;

    return Semantics(
      button: true,
      selected: elegido,
      label: nombreDeDiaCapitalizado(dia),
      child: ExcludeSemantics(
        child: GestureDetector(
          key: Key('dia$dia'),
          onTap: alTocar,
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 150),
            curve: Curves.easeOut,
            width: 44,
            height: 44,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: elegido ? esquema.primary : esquema.surface,
              border: Border.all(color: elegido ? Colors.transparent : esquema.outline, width: 2),
            ),
            child: Text(
              letraDeDia(dia),
              style: Theme.of(
                context,
              ).textTheme.labelMedium?.copyWith(color: elegido ? esquema.onPrimary : esquema.onSurface, fontWeight: FontWeight.w800),
            ),
          ),
        ),
      ),
    );
  }
}

/// Los siete días en pequeño, solo lectura: los de [dias] resaltados (tarjeta de un horario).
class DiasDeLaSemana extends StatelessWidget {
  final List<int> dias;

  const DiasDeLaSemana({super.key, required this.dias});

  @override
  Widget build(BuildContext context) {
    final esquema = Theme.of(context).colorScheme;
    final nombres = dias.map(nombreDeDiaCapitalizado).join(', ');

    return Semantics(
      label: 'Días: $nombres',
      child: ExcludeSemantics(
        child: Wrap(
          spacing: 6,
          runSpacing: 6,
          children: [
            for (var dia = 1; dia <= 7; dia++)
              Container(
                width: 28,
                height: 28,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: dias.contains(dia) ? esquema.primary : Colors.transparent,
                  border: Border.all(color: dias.contains(dia) ? Colors.transparent : esquema.outline, width: 1.5),
                ),
                child: Text(
                  letraDeDia(dia),
                  style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    fontSize: 12,
                    color: dias.contains(dia) ? esquema.onPrimary : esquema.onSurfaceVariant,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
