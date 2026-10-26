import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/franjas_seleccionadas_provider.dart';
import 'resumen_reserva_sheet.dart';

/// La barra fija de abajo de «Reservar» (CU14): «Confirmar (N)» y «Quitar todas». Solo aparece con al menos una franja
/// elegida; «Confirmar» abre el resumen y recién ahí se gastan las horas.
///
/// * Es el único botón relleno de ámbar de la pantalla (una sola acción principal por pantalla).
class BarraConfirmarReserva extends ConsumerWidget {
  const BarraConfirmarReserva({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final elegidas = ref.watch(franjasSeleccionadasUiProvider).length;
    if (elegidas == 0) return const SizedBox.shrink();
    final tema = Theme.of(context);

    return Material(
      key: const Key('barraConfirmar'),
      color: tema.colorScheme.surface,
      elevation: 8,
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 10, 20, 10),
          child: Row(
            children: [
              TextButton(
                key: const Key('quitarTodas'),
                onPressed: () => ref.read(franjasSeleccionadasUiProvider.notifier).vaciar(),
                child: const Text('Quitar todas'),
              ),
              const SizedBox(width: 12),
              // * El tema da a los botones rellenos ancho infinito: dentro de una fila tiene que ir en un Expanded
              Expanded(
                child: FilledButton(
                  key: const Key('confirmar'),
                  onPressed: () => mostrarResumenReserva(context),
                  child: Text('Confirmar ($elegidas)'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
