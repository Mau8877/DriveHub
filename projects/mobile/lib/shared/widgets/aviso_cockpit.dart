import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';

enum TonoAviso { info, advertencia, error, exito }

/// Aviso en línea (`notice` del mockup): icono + texto, con un fondo tenue del tono y borde.
///
/// * El texto va siempre en el color de texto del tema (no en el del tono) para que se lea bien en
/// * claro y en oscuro; solo el icono toma el color del tono.
class AvisoCockpit extends StatelessWidget {
  final String texto;
  final TonoAviso tono;
  final Widget? accion;

  const AvisoCockpit({
    super.key,
    required this.texto,
    this.tono = TonoAviso.info,
    this.accion,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final colores = context.cockpit;
    final (Color color, IconData icono) = switch (tono) {
      TonoAviso.info => (colores.enlace, Icons.info_outline),
      TonoAviso.advertencia => (colores.acento, Icons.mail_outline),
      TonoAviso.error => (tema.colorScheme.error, Icons.error_outline),
      TonoAviso.exito => (colores.exito, Icons.check_circle_outline),
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        // * Sobre la superficie opaca: un tinte, no una transparencia sobre el degradado
        color: Color.alphaBlend(color.withValues(alpha: 0.10), tema.colorScheme.surface),
        borderRadius: BorderRadius.circular(AppRadios.control),
        border: Border.all(color: color.withValues(alpha: 0.35)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(top: 1),
            child: Icon(icono, size: 20, color: color),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(top: 2),
              child: Text(texto, style: tema.textTheme.bodyMedium),
            ),
          ),
          if (accion != null) ...[const SizedBox(width: 8), accion!],
        ],
      ),
    );
  }
}
