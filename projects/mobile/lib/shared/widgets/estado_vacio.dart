import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';

/// Estado vacío de una lista o pantalla: ficha de icono, título y una línea de explicación.
class EstadoVacio extends StatelessWidget {
  final IconData icono;
  final String titulo;
  final String texto;

  const EstadoVacio({
    super.key,
    required this.icono,
    required this.titulo,
    required this.texto,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final acento = context.cockpit.acento;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 36),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              color: acento.withValues(alpha: 0.14),
              borderRadius: BorderRadius.circular(18),
            ),
            child: Icon(icono, color: acento, size: 26),
          ),
          const SizedBox(height: 14),
          Text(
            titulo,
            style: tema.textTheme.titleMedium,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 4),
          Text(
            texto,
            style: tema.textTheme.bodyMedium?.copyWith(
              color: tema.colorScheme.onSurfaceVariant,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}
