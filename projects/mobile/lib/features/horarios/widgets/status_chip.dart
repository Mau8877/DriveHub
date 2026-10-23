import 'package:flutter/material.dart';

import '../../../core/theme/app_theme.dart';

/// Chip informativo de estado: siempre ícono y texto, nunca solo color (diseño Cockpit).
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
    fondo: _tinte(context, context.cockpit.exito),
    color: context.cockpit.exito,
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
      fondo: context.cockpit.superficieSuave,
      color: atenuado ? esquema.onSurfaceVariant : esquema.onSurface,
    );
  }

  /// Estado azul: reservada, en curso.
  factory StatusChip.azul(BuildContext context, String label, IconData icon) {
    return StatusChip(
      icon: icon,
      label: label,
      fondo: _tinte(context, context.cockpit.enlace),
      color: context.cockpit.enlace,
    );
  }

  /// Fondo tenue del color, sobre la superficie opaca (nunca una transparencia sobre el degradado).
  static Color _tinte(BuildContext context, Color color) => Color.alphaBlend(
    color.withValues(alpha: 0.12),
    Theme.of(context).colorScheme.surface,
  );

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minHeight: 28),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      decoration: BoxDecoration(
        color: fondo,
        borderRadius: BorderRadius.circular(99),
        border: Border.all(color: color.withValues(alpha: 0.4)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 15, color: color),
          const SizedBox(width: 4),
          Flexible(
            child: Text(
              label,
              style: Theme.of(context).textTheme.labelSmall?.copyWith(
                color: color,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
