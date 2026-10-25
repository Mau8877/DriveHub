import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../models/estado_pago_visible.dart';
import '../models/pago_detalle.dart';
import '../providers/pago_detail_provider.dart';
import '../providers/reembolso_provider.dart';
import '../widgets/dialogo_reembolso.dart';
import '../widgets/formato_dinero.dart';
import '../widgets/formato_fecha.dart';
import '../widgets/insignia_de_estado_pago.dart';
import '../widgets/mensajes_de_reembolso.dart';

/// El detalle de una compra (CU28, `/payments/:codigo`): plan, horas compradas y restantes, montos, estado y
/// fechas.
///
/// * Una compra ajena o inexistente (`ERR_PAG_07`) vuelve a la lista con un aviso.
/// * El reembolso (CU31) aparece solo si el backend dice que la compra es reembolsable; los comprobantes (CU30)
///   se suman con su caso de uso móvil.
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
    // * Una compra con el reembolso en proceso se sigue hasta que la pasarela avise (CU31)
    ref.listen(pagoDetailProvider(codigo), (_, nuevo) {
      if (nuevo.valueOrNull?.estado == EstadoPagoVisible.reembolsoEnProceso) {
        ref.read(reembolsoProvider(codigo).notifier).vigilar();
      }
    });
    ref.listen(reembolsoProvider(codigo), (anterior, nuevo) {
      if (anterior?.valueOrNull == nuevo.valueOrNull) return;
      switch (nuevo.valueOrNull) {
        case ResultadoReembolso.reembolsado:
          avisar(context, 'Tu reembolso se completó.');
        case ResultadoReembolso.fallo:
          avisar(
            context,
            'No pudimos devolver el dinero, las horas siguen en tu cuenta.',
          );
        default:
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
          data: (pago) => _Contenido(pago: pago, codigo: codigo),
        ),
      ),
    );
  }
}

class _Contenido extends StatelessWidget {
  final PagoDetalle pago;
  final String codigo;

  const _Contenido({required this.pago, required this.codigo});

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
        _SeccionReembolso(pago: pago, codigo: codigo),
      ],
    );
  }
}

/// La sección «Reembolso» (CU31): solo aparece si el backend dice que la compra es reembolsable (la app no lo
/// recalcula) o si su reembolso está en proceso.
class _SeccionReembolso extends ConsumerWidget {
  final PagoDetalle pago;
  final String codigo;

  const _SeccionReembolso({required this.pago, required this.codigo});

  Future<void> _solicitar(BuildContext context, WidgetRef ref) async {
    final elegido = await pedirMotivoDeReembolso(context, pago);
    if (elegido == null) return;
    final aceptado = await ref
        .read(reembolsoProvider(codigo).notifier)
        .solicitar(elegido.motivo, motivoDetalle: elegido.detalle);
    if (aceptado) return;
    final error = ref.read(reembolsoProvider(codigo)).error;
    if (error == null) return;
    if (context.mounted) avisar(context, mensajeDeErrorDeReembolso(error));
    if (errorDeReembolsoPideRefrescar(error)) {
      ref.invalidate(pagoDetailProvider(codigo));
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tema = Theme.of(context);

    if (pago.estado == EstadoPagoVisible.reembolsoEnProceso) {
      return const Padding(
        padding: EdgeInsets.only(top: 12),
        child: AvisoCockpit(
          key: Key('avisoReembolsoEnProceso'),
          texto: 'Estamos procesando tu reembolso.',
        ),
      );
    }
    final reembolso = pago.reembolso;
    if (!pago.reembolsable || reembolso == null) return const SizedBox.shrink();

    final enCurso = ref.watch(reembolsoProvider(codigo)).isLoading;
    return Padding(
      padding: const EdgeInsets.only(top: 12),
      child: Card(
        key: const Key('seccionReembolso'),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Reembolso', style: tema.textTheme.titleMedium),
              const SizedBox(height: 8),
              Text(
                'Recibirás ${formatearUsd(reembolso.montoDevuelto)}; se retiene '
                '${formatearUsd(reembolso.montoRetenido)} '
                '(${reembolso.porcentajeRetenido} %).',
                key: const Key('textoReembolsoPrevisto'),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton(
                  key: const Key('botonSolicitarReembolso'),
                  onPressed: enCurso ? null : () => _solicitar(context, ref),
                  child: enCurso
                      ? const IndicadorBoton()
                      : const Text('Solicitar reembolso'),
                ),
              ),
            ],
          ),
        ),
      ),
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
