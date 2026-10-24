import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';

enum TonoInsignia { exito, info, advertencia, neutro }

/// Insignia de estado (`badge` del mockup): punto + texto en una píldora con borde.
class Insignia extends StatelessWidget {
  final String texto;
  final TonoInsignia tono;

  const Insignia(this.texto, {super.key, this.tono = TonoInsignia.neutro});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final colores = context.cockpit;
    final color = switch (tono) {
      TonoInsignia.exito => colores.exito,
      TonoInsignia.info => colores.enlace,
      TonoInsignia.advertencia => colores.acento,
      TonoInsignia.neutro => tema.colorScheme.onSurfaceVariant,
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
      decoration: BoxDecoration(
        color: Color.alphaBlend(color.withValues(alpha: 0.10), tema.colorScheme.surface),
        borderRadius: BorderRadius.circular(99),
        border: Border.all(color: color.withValues(alpha: 0.5)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 6,
            height: 6,
            decoration: BoxDecoration(color: color, shape: BoxShape.circle),
          ),
          const SizedBox(width: 6),
          Text(
            texto,
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w700,
              color: color,
            ),
          ),
        ],
      ),
    );
  }
}
