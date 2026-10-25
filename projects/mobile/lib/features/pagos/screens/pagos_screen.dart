import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../../../shared/widgets/fila_accion.dart';
import '../models/estado_pago_visible.dart';
import '../models/pago_resumen.dart';
import '../providers/pagos_filtro_ui_provider.dart';
import '../providers/pagos_list_provider.dart';
import '../providers/saldo_horas_provider.dart';
import '../widgets/formato_dinero.dart';
import '../widgets/formato_fecha.dart';
import '../widgets/insignia_de_estado_pago.dart';

/// La sección «Pagos» del estudiante (pestaña del `EstudianteShell`, CU26 y CU28): la acción «Comprar horas»,
/// las horas disponibles, el filtro por estado y el historial de compras.
///
/// * «Cargar más» agrega la siguiente página (no hay scroll infinito); deslizar hacia abajo lo vuelve a pedir todo.
/// * La app solo muestra lo que informa el backend: no calcula estados ni dinero.
class PagosScreen extends ConsumerWidget {
  const PagosScreen({super.key});

  /// Los chips del filtro: el texto y el estado que piden (`null` = todas).
  static const _filtros = <(String, EstadoPagoVisible?)>[
    ('Todos', null),
    ('En proceso', EstadoPagoVisible.pendiente),
    ('Confirmados', EstadoPagoVisible.confirmado),
    ('Fallidos', EstadoPagoVisible.fallido),
    ('Reembolsos', EstadoPagoVisible.reembolsado),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final listado = ref.watch(pagosListProvider);
    final filtro = ref.watch(pagosFiltroUiProvider);

    return RefreshIndicator(
      onRefresh: () async {
        ref.invalidate(saldoHorasProvider);
        await ref.read(pagosListProvider.notifier).refrescar();
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
        children: [
          Card(
            child: FilaAccion(
              icono: Icons.add_card_outlined,
              titulo: 'Comprar horas',
              detalle: 'Elige un plan y paga con tarjeta',
              onTap: () => context.push('/payments/plans'),
            ),
          ),
          const SizedBox(height: 12),
          const _TarjetaSaldo(),
          const SizedBox(height: 16),
          Wrap(
            spacing: 8,
            runSpacing: 4,
            children: [
              for (final (texto, estado) in _filtros)
                ChoiceChip(
                  key: Key('filtro-${estado?.valorApi ?? 'TODOS'}'),
                  label: Text(texto),
                  selected: filtro == estado,
                  onSelected: (_) =>
                      ref.read(pagosFiltroUiProvider.notifier).elegir(estado),
                ),
            ],
          ),
          const SizedBox(height: 12),
          ...listado.when(
            loading: () => const [
              Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: CircularProgressIndicator()),
              ),
            ],
            error: (_, _) => [
              const AvisoCockpit(
                texto:
                    'No pudimos cargar tus compras. Revisa tu conexión e inténtalo de nuevo.',
                tono: TonoAviso.error,
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                key: const Key('botonReintentarPagos'),
                onPressed: () => ref.invalidate(pagosListProvider),
                child: const Text('Reintentar'),
              ),
            ],
            data: (datos) => _lista(context, ref, datos, filtro),
          ),
        ],
      ),
    );
  }

  List<Widget> _lista(
    BuildContext context,
    WidgetRef ref,
    PagosListado datos,
    EstadoPagoVisible? filtro,
  ) {
    if (datos.items.isEmpty) {
      return [
        EstadoVacio(
          icono: Icons.account_balance_wallet_outlined,
          titulo: filtro == null
              ? 'Todavía no tienes compras'
              : 'No hay compras en este estado',
          texto: filtro == null
              ? 'Cuando compres un plan de horas, aparecerá aquí.'
              : 'Prueba con otro filtro.',
        ),
      ];
    }
    return [
      for (final pago in datos.items) ...[
        _TarjetaCompra(pago: pago),
        const SizedBox(height: 10),
      ],
      if (datos.errorAlCargarMas != null) ...[
        const AvisoCockpit(
          texto: 'No pudimos cargar más compras. Inténtalo de nuevo.',
          tono: TonoAviso.error,
        ),
        const SizedBox(height: 8),
      ],
      if (datos.hayMas)
        OutlinedButton(
          key: const Key('botonCargarMas'),
          onPressed: datos.cargandoMas
              ? null
              : () => ref.read(pagosListProvider.notifier).cargarMas(),
          child: datos.cargandoMas
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : Text(datos.errorAlCargarMas != null ? 'Reintentar' : 'Cargar más'),
        ),
    ];
  }
}

/// Las horas que el estudiante puede gastar hoy.
class _TarjetaSaldo extends ConsumerWidget {
  const _TarjetaSaldo();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final saldo = ref.watch(saldoHorasProvider);
    final tema = Theme.of(context);
    final texto = saldo.maybeWhen(data: (h) => '$h h', orElse: () => '– h');

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Icon(Icons.schedule, color: tema.colorScheme.onSurfaceVariant),
            const SizedBox(width: 12),
            Expanded(
              child: Text('Horas disponibles', style: tema.textTheme.bodyLarge),
            ),
            Text(
              texto,
              key: const Key('saldoDelHistorial'),
              style: tema.textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.w800,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _TarjetaCompra extends StatelessWidget {
  final PagoResumen pago;

  const _TarjetaCompra({required this.pago});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    return Card(
      child: InkWell(
        key: Key('compra-${pago.codigo}'),
        borderRadius: BorderRadius.circular(16),
        onTap: () => context.push('/payments/${pago.codigo}'),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(pago.planNombre, style: tema.textTheme.titleMedium),
                    const SizedBox(height: 2),
                    Text(
                      pago.horas == 1 ? '1 hora' : '${pago.horas} horas',
                      style: tema.textTheme.bodyMedium,
                    ),
                    const SizedBox(height: 6),
                    Text(
                      formatearUsd(pago.montoUsd),
                      style: tema.textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    Text(
                      '${formatearBs(pago.montoBs)} · ${formatearFechaCorta(pago.creadoEn)}',
                      style: tema.textTheme.bodySmall?.copyWith(
                        color: tema.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              InsigniaDeEstadoPago(pago.estado),
            ],
          ),
        ),
      ),
    );
  }
}
