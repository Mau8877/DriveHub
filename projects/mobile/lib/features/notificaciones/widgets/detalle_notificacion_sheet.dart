import 'package:flutter/material.dart';

import '../models/notificacion.dart';
import 'fecha_aviso.dart';

/// El aviso completo en una hoja inferior (CU38): tipo, título, mensaje y fecha con hora.
///
/// * Marcarlo como leído lo hace quien la abre; la hoja solo lo muestra.
Future<void> mostrarDetalleNotificacion(BuildContext context, Notificacion aviso) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    showDragHandle: true,
    builder: (_) => _DetalleNotificacion(aviso: aviso),
  );
}

class _DetalleNotificacion extends StatelessWidget {
  final Notificacion aviso;

  const _DetalleNotificacion({required this.aviso});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
        child: Column(
          key: const Key('detalleNotificacion'),
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              aviso.etiquetaTipo,
              style: tema.textTheme.labelLarge?.copyWith(
                color: tema.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 4),
            Text(aviso.titulo, style: tema.textTheme.titleLarge),
            const SizedBox(height: 12),
            Text(aviso.mensaje, style: tema.textTheme.bodyLarge),
            const SizedBox(height: 16),
            Text(
              fechaYHoraDeAviso(aviso.creadaEn),
              style: tema.textTheme.bodySmall?.copyWith(
                color: tema.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
