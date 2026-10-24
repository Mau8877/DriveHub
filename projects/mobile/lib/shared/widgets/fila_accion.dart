import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';

/// Fila de lista con ficha de icono, título, detalle y flecha (`fila` del mockup). Se agrupan
/// dentro de una `Card`, separadas por un `Divider`.
class FilaAccion extends StatelessWidget {
  final IconData icono;
  final String titulo;
  final String? detalle;
  final VoidCallback? onTap;
  final bool peligro;

  /// Reemplaza la flecha del final (por ejemplo, una insignia).
  final Widget? trailing;

  const FilaAccion({
    super.key,
    required this.icono,
    required this.titulo,
    this.detalle,
    this.onTap,
    this.peligro = false,
    this.trailing,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final color = peligro ? tema.colorScheme.error : context.cockpit.acento;

    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        child: Row(
          children: [
            Container(
              width: 42,
              height: 42,
              decoration: BoxDecoration(
                color: color.withValues(alpha: 0.14),
                borderRadius: BorderRadius.circular(13),
              ),
              child: Icon(icono, size: 21, color: color),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    titulo,
                    style: tema.textTheme.bodyLarge?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: peligro ? tema.colorScheme.error : null,
                    ),
                  ),
                  if (detalle != null)
                    Text(detalle!, style: tema.textTheme.bodySmall),
                ],
              ),
            ),
            trailing ??
                (onTap != null && !peligro
                    ? Icon(
                        Icons.chevron_right,
                        color: tema.colorScheme.onSurfaceVariant,
                      )
                    : const SizedBox.shrink()),
          ],
        ),
      ),
    );
  }
}
