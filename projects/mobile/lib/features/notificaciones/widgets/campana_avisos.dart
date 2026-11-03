import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../providers/bandeja_providers.dart';

/// La campana de la barra superior (CU38): abre la bandeja y muestra cuántos avisos sin leer hay.
///
/// * Sin avisos, o si no se pudo saber el contador, no hay insignia. Desde 100 se muestra «99+».
/// * Al volver de la bandeja se vuelve a pedir el contador.
class CampanaAvisos extends ConsumerWidget {
  const CampanaAvisos({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final conteo = ref.watch(conteoNoLeidasProvider).valueOrNull ?? 0;
    final etiqueta = conteo > 99 ? '99+' : '$conteo';

    return IconButton(
      tooltip: 'Avisos',
      icon: Badge(
        key: const Key('insigniaAvisos'),
        isLabelVisible: conteo > 0,
        label: Text(etiqueta),
        child: Icon(
          Icons.notifications_none,
          semanticLabel: conteo > 0 ? '$etiqueta sin leer' : null,
        ),
      ),
      onPressed: () async {
        await context.push('/notificaciones');
        ref.invalidate(conteoNoLeidasProvider);
      },
    );
  }
}
