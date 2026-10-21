import 'package:flutter/material.dart';

import '../../../core/theme/app_theme.dart';

/// Chip informativo de estado: siempre ícono y texto, nunca solo color (design.md 3.6).
///
/// * No es tocable. Los colores se toman del tema, no se pasan literales.
class StatusChip extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color fondo;
  final Color color;

  const StatusChip({
    super.key,
    required this.icon,
    required this.label,
    required this.fondo,
    required this.color,
  });

  /// «Activa» de una plantilla.
  factory StatusChip.exito(BuildContext context, String label) => StatusChip(
    icon: Icons.check,
    label: label,
    fondo: context.semantica.successContainer,
    color: context.semantica.success,
  );

  /// Estado neutro: libre, bloqueada, inactiva.
  factory StatusChip.neutro(
    BuildContext context,
    String label,
    IconData icon, {
    bool atenuado = false,
  }) {
    final esquema = Theme.of(context).colorScheme;
    return StatusChip(
      icon: icon,
      label: label,
      fondo: context.semantica.neutralContainer,
      color: atenuado ? esquema.onSurfaceVariant : esquema.onSurface,
    );
  }

  /// Estado azul: reservada, en curso.
  factory StatusChip.azul(BuildContext context, String label, IconData icon) {
    final esquema = Theme.of(context).colorScheme;
    return StatusChip(
      icon: icon,
      label: label,
      fondo: esquema.tertiaryContainer,
      color: esquema.onTertiaryContainer,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minHeight: 28),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      decoration: BoxDecoration(
        color: fondo,
        borderRadius: BorderRadius.circular(AppRadius.chip),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 15, color: color),
          const SizedBox(width: 4),
          Flexible(
            child: Text(
              label,
              style: Theme.of(
                context,
              ).textTheme.labelSmall?.copyWith(color: color),
            ),
          ),
        ],
      ),
    );
  }
}
