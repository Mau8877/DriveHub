import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/saldo_horas_provider.dart';

/// Las horas que le quedan al estudiante, junto a la campanita del encabezado (CU27).
///
/// * Mientras carga o si falla muestra «–»: el encabezado no se rompe ni avisa de un error por esto.
/// * Tocarlo vuelve a pedir el saldo.
class SaldoHorasChip extends ConsumerWidget {
  const SaldoHorasChip({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final saldo = ref.watch(saldoHorasProvider);
    final tema = Theme.of(context);
    final texto = saldo.maybeWhen(data: (h) => '$h h', orElse: () => '– h');

    return Tooltip(
      message: 'Horas disponibles',
      child: InkWell(
        key: const Key('saldoHoras'),
        borderRadius: BorderRadius.circular(20),
        onTap: () => ref.invalidate(saldoHorasProvider),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.schedule,
                size: 18,
                color: tema.colorScheme.onSurfaceVariant,
              ),
              const SizedBox(width: 4),
              Text(
                texto,
                style: tema.textTheme.labelLarge?.copyWith(
                  fontWeight: FontWeight.w800,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
