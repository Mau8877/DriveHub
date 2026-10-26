import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/app_theme.dart';
import '../../pagos/providers/saldo_horas_provider.dart';
import '../providers/franjas_seleccionadas_provider.dart';

/// Las horas del estudiante, siempre a la vista en la sección «Clases» (CU14).
///
/// * Sin franjas elegidas muestra el saldo. Con franjas elegidas muestra «5 h → 3 h» (las que tiene y las que le
/// * quedarían): es solo una vista previa calculada en el teléfono, no reserva ni descuenta nada. Al confirmar,
/// * `ReservasNotifier` invalida el saldo y este indicador vuelve a mostrar el real.
/// * Con cero horas ofrece comprar (CU26). Mientras carga o si falla muestra «– h» y tocarlo vuelve a pedirlo.
class HorasDisponiblesBanner extends ConsumerWidget {
  const HorasDisponiblesBanner({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final saldo = ref.watch(saldoHorasProvider);
    final elegidas = ref.watch(franjasSeleccionadasUiProvider).length;
    final quedarian = ref.watch(horasQueQuedanProvider).maybeWhen(data: (h) => h, orElse: () => null);

    // * `.value` de un error lanza: con el saldo caído la pantalla sigue viva y muestra «– h»
    final horas = saldo.maybeWhen(data: (h) => h, orElse: () => null);
    final sinHoras = horas != null && horas == 0 && elegidas == 0;
    final mostrandoVistaPrevia = horas != null && elegidas > 0 && quedarian != null;

    final descripcion = horas == null
        ? 'Horas disponibles: no se pudieron cargar'
        : mostrandoVistaPrevia
        ? 'Horas disponibles: $horas. Quedarían $quedarian'
        : 'Horas disponibles: $horas';

    return Semantics(
      container: true,
      label: descripcion,
      child: Card(
        margin: EdgeInsets.zero,
        child: InkWell(
          key: const Key('bannerHoras'),
          borderRadius: BorderRadius.circular(16),
          onTap: horas == null ? () => ref.invalidate(saldoHorasProvider) : null,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            child: Row(
              children: [
                Icon(Icons.schedule, color: context.cockpit.acento),
                const SizedBox(width: 12),
                Expanded(
                  child: ExcludeSemantics(
                    child: AnimatedSwitcher(
                      duration: const Duration(milliseconds: 200),
                      child: _Contenido(
                        key: ValueKey('$horas-$elegidas'),
                        horas: horas,
                        elegidas: elegidas,
                        quedarian: quedarian,
                      ),
                    ),
                  ),
                ),
                if (sinHoras)
                  TextButton(
                    key: const Key('comprarHoras'),
                    onPressed: () => context.push('/payments/plans'),
                    child: const Text('Comprar horas'),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _Contenido extends StatelessWidget {
  final int? horas;
  final int elegidas;
  final int? quedarian;

  const _Contenido({super.key, required this.horas, required this.elegidas, required this.quedarian});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final acento = context.cockpit.acento;
    final apagado = tema.colorScheme.onSurfaceVariant;

    if (horas == null) {
      return Text('– h', key: const Key('horasDesconocidas'), style: tema.textTheme.titleMedium);
    }
    if (elegidas > 0 && quedarian != null) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text.rich(
            key: const Key('horasVistaPrevia'),
            TextSpan(
              style: tema.textTheme.titleMedium,
              children: [
                TextSpan(text: '$horas h'),
                TextSpan(
                  text: '  →  $quedarian h',
                  style: tema.textTheme.titleMedium?.copyWith(color: acento, fontWeight: FontWeight.w800),
                ),
              ],
            ),
          ),
          Text(
            elegidas == 1 ? '1 clase elegida' : '$elegidas clases elegidas',
            style: tema.textTheme.bodySmall?.copyWith(color: apagado),
          ),
        ],
      );
    }
    if (horas == 0) {
      return Text('No tienes horas disponibles', key: const Key('sinHoras'), style: tema.textTheme.titleSmall);
    }
    return Text.rich(
      key: const Key('horasSaldo'),
      TextSpan(
        style: tema.textTheme.bodyLarge,
        children: [
          const TextSpan(text: 'Tienes '),
          TextSpan(
            text: '$horas h',
            style: tema.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
          ),
          const TextSpan(text: ' disponibles'),
        ],
      ),
    );
  }
}
