import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../models/pago_detalle.dart';
import '../providers/pago_detail_provider.dart';
import '../widgets/formato_dinero.dart';
import '../widgets/formato_fecha.dart';
import '../widgets/insignia_de_estado_pago.dart';

/// El detalle de una compra (CU28, `/payments/:codigo`): plan, horas compradas y restantes, montos, estado y
/// fechas.
///
/// * Una compra ajena o inexistente (`ERR_PAG_07`) vuelve a la lista con un aviso.
/// * Los comprobantes (CU30) y el reembolso (CU31) se suman a esta pantalla con sus casos de uso: hasta
///   entonces no se muestra nada de eso.
class PagoDetalleScreen extends ConsumerWidget {
  final String codigo;

  const PagoDetalleScreen({super.key, required this.codigo});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    ref.listen(pagoDetailProvider(codigo), (_, nuevo) {
      final error = nuevo.error;
      if (error is ApiException && error.code == 'ERR_PAG_07') {
        avisar(context, 'Esa compra no existe.');
        if (context.canPop()) {
          context.pop();
        } else {
          context.go('/inicio');
        }
      }
    });
    final detalle = ref.watch(pagoDetailProvider(codigo));

    return CockpitScaffold(
      appBar: AppBar(title: const Text('Detalle de la compra')),
      body: SafeArea(
        child: detalle.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (_, _) => ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
            children: [
              const AvisoCockpit(
                texto:
                    'No pudimos cargar la compra. Revisa tu conexión e inténtalo de nuevo.',
                tono: TonoAviso.error,
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                key: const Key('botonReintentarDetalle'),
                onPressed: () => ref.invalidate(pagoDetailProvider(codigo)),
                child: const Text('Reintentar'),
              ),
            ],
          ),
          data: (pago) => _Contenido(pago: pago),
        ),
      ),
    );
  }
}

class _Contenido extends StatelessWidget {
  final PagoDetalle pago;

  const _Contenido({required this.pago});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        pago.planNombre,
                        style: tema.textTheme.titleLarge,
                      ),
                    ),
                    InsigniaDeEstadoPago(pago.estado),
                  ],
                ),
                const SizedBox(height: 16),
                _Dato('Horas compradas', '${pago.horas}'),
                if (pago.horasRestantes != null)
                  _Dato('Horas restantes', '${pago.horasRestantes}'),
                _Dato('Monto', formatearUsd(pago.montoUsd)),
                _Dato('Costo del plan', formatearBs(pago.montoBs)),
                _Dato('Fecha de compra', formatearFechaCorta(pago.creadoEn)),
                if (pago.confirmadoEn != null)
                  _Dato(
                    'Fecha de confirmación',
                    formatearFechaCorta(pago.confirmadoEn!),
                  ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _Dato extends StatelessWidget {
  final String etiqueta;
  final String valor;

  const _Dato(this.etiqueta, this.valor);

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            etiqueta,
            style: tema.textTheme.bodyMedium?.copyWith(
              color: tema.colorScheme.onSurfaceVariant,
            ),
          ),
          Text(
            valor,
            style: tema.textTheme.bodyMedium?.copyWith(
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}
