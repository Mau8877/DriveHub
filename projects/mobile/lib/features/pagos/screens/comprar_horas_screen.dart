import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../models/plan_disponible.dart';
import '../providers/compra_provider.dart';
import '../providers/planes_provider.dart';
import '../widgets/formato_dinero.dart';

/// «Comprar horas» (CU26, `/payments/plans`): los planes activos de la escuela del estudiante, de menos a más
/// horas, cada uno con su precio en bolivianos y en dólares y el botón «Comprar».
///
/// * Al elegir un plan se guarda en la compra en curso (con su clave de idempotencia) y se va al resumen.
/// * La lista se vuelve a pedir cada vez que se abre la pantalla.
class ComprarHorasScreen extends ConsumerWidget {
  const ComprarHorasScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final planes = ref.watch(planesDisponiblesProvider);

    return CockpitScaffold(
      appBar: AppBar(title: const Text('Comprar horas')),
      body: SafeArea(
        child: planes.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (_, _) => ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
            children: [
              const AvisoCockpit(
                texto:
                    'No pudimos cargar los planes. Revisa tu conexión e inténtalo de nuevo.',
                tono: TonoAviso.error,
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                key: const Key('botonReintentarPlanes'),
                onPressed: () => ref.invalidate(planesDisponiblesProvider),
                child: const Text('Reintentar'),
              ),
            ],
          ),
          data: (lista) => lista.isEmpty
              ? const Center(
                  child: EstadoVacio(
                    icono: Icons.receipt_long_outlined,
                    titulo: 'Sin planes por ahora',
                    texto: 'Tu escuela todavía no ofrece planes de horas.',
                  ),
                )
              : RefreshIndicator(
                  onRefresh: () =>
                      ref.refresh(planesDisponiblesProvider.future),
                  child: ListView.separated(
                    padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
                    itemCount: lista.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 12),
                    itemBuilder: (context, i) => _TarjetaPlan(plan: lista[i]),
                  ),
                ),
        ),
      ),
    );
  }
}

class _TarjetaPlan extends ConsumerWidget {
  final PlanDisponible plan;

  const _TarjetaPlan({required this.plan});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tema = Theme.of(context);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(plan.nombre, style: tema.textTheme.titleMedium),
                  const SizedBox(height: 2),
                  Text(
                    plan.horas == 1 ? '1 hora' : '${plan.horas} horas',
                    style: tema.textTheme.bodyMedium,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    formatearBs(plan.costoBs),
                    style: tema.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  Text(
                    '≈ ${formatearUsd(plan.costoUsd)}',
                    style: tema.textTheme.bodySmall?.copyWith(
                      color: tema.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            FilledButton(
              key: Key('comprar-${plan.codigo}'),
              // * El tema da a los botones ancho infinito (ocupan toda la fila); dentro de este Row eso rompe el
              // * layout, así que acá el botón mide lo que su texto
              style: FilledButton.styleFrom(minimumSize: const Size(0, 44)),
              onPressed: () {
                ref.read(compraProvider.notifier).limpiar();
                ref.read(compraUiProvider.notifier).elegirPlan(plan);
                context.push('/payments/summary');
              },
              child: const Text('Comprar'),
            ),
          ],
        ),
      ),
    );
  }
}
