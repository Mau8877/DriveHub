import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/estado_asistente.dart';
import '../providers/asistente_provider.dart';

/// El botón del asistente de voz (CU50): se mantiene pulsado para hablar y se suelta para que procese.
///
/// * Usa el puntero directo (no un «pulsación larga»): graba desde el instante en que se toca, sin la espera de
///   medio segundo de un gesto de pulsación larga, que cortaría el comienzo de la frase.
/// * Si el dedo sale del botón o el sistema cancela el gesto, igual se termina: nunca queda una grabación abierta.
/// * No habla con ningún modelo hasta que se pulsa: dibujarlo no carga nada.
class BotonMicrofono extends ConsumerWidget {
  const BotonMicrofono({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(asistenteProvider);
    final activo = estado is Escuchando;
    final ocupado = estado is Entendiendo;
    final tema = Theme.of(context);
    final notificador = ref.read(asistenteProvider.notifier);

    return Semantics(
      button: true,
      label: activo ? 'Escuchando. Suelta para terminar' : 'Mantén pulsado para hablarle al asistente',
      child: Listener(
        onPointerDown: ocupado ? null : (_) => notificador.empezarAEscuchar(),
        onPointerUp: (_) => notificador.terminarDeEscuchar(),
        onPointerCancel: (_) => notificador.terminarDeEscuchar(),
        child: AnimatedContainer(
          key: const Key('botonMicrofono'),
          duration: const Duration(milliseconds: 150),
          width: 56,
          height: 56,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            // * Opaco siempre: el icono tiene que leerse sobre cualquier fondo
            color: activo ? tema.colorScheme.error : tema.colorScheme.primary,
          ),
          child: Icon(
            ocupado ? Icons.hourglass_top : (activo ? Icons.mic : Icons.mic_none),
            color: activo ? tema.colorScheme.onError : tema.colorScheme.onPrimary,
            size: 28,
          ),
        ),
      ),
    );
  }
}
