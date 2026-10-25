import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../providers/abridor_provider.dart';
import '../providers/compra_provider.dart';
import '../widgets/formato_dinero.dart';
import '../widgets/mensajes_de_pago.dart';

/// Resumen de la compra (CU26, `/payments/summary`): el plan, las horas y lo que se va a pagar. Al confirmar
/// se crea el pago, se abre la página de pago de la pasarela y se pasa a la pantalla del resultado.
///
/// * Se cobra en dólares (la pasarela no cobra en bolivianos): se muestra el monto en dólares y su equivalente
/// * en bolivianos ANTES de pagar.
/// * Un toque doble no crea dos cobros: la compra lleva su clave de idempotencia y `iniciar` ignora una
/// * segunda llamada mientras hay una en curso.
/// * La app nunca ve datos de tarjeta: se ingresan solo en la página de la pasarela.
class ResumenCompraScreen extends ConsumerWidget {
  const ResumenCompraScreen({super.key});

  Future<void> _confirmar(BuildContext context, WidgetRef ref) async {
    final router = GoRouter.of(context);
    final mensajes = ScaffoldMessenger.of(context);
    final abrir = ref.read(abridorDePaginaProvider);

    final pago = await ref.read(compraProvider.notifier).iniciar();
    if (pago == null) {
      // * El plan ya no está: se vuelve a la lista (que se vuelve a pedir) con un aviso
      final error = ref.read(compraProvider).error;
      if (error is ApiException && error.code == 'ERR_PAG_05') {
        mensajes
          ..hideCurrentSnackBar()
          ..showSnackBar(
            SnackBar(content: Text(mensajeDeErrorDeCompra(error))),
          );
        router.go('/payments/plans');
      } else if (error is ApiException && error.code == 'ERR_PAG_18') {
        ref.read(compraUiProvider.notifier).terminar();
      }
      return;
    }
    if (pago.urlPago != null) {
      final abrio = await abrir(pago.urlPago!);
      if (!abrio) {
        mensajes
          ..hideCurrentSnackBar()
          ..showSnackBar(
            const SnackBar(
              content: Text(
                'No pudimos abrir la página de pago. Puedes volver a intentarlo desde aquí.',
              ),
            ),
          );
      }
    }
    router.go('/payments/result');
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final plan = ref.watch(compraUiProvider).plan;
    final compra = ref.watch(compraProvider);
    final tema = Theme.of(context);

    if (plan == null) {
      return CockpitScaffold(
        appBar: AppBar(title: const Text('Resumen de la compra')),
        body: const Center(
          child: EstadoVacio(
            icono: Icons.receipt_long_outlined,
            titulo: 'No hay una compra en curso',
            texto: 'Elige un plan para comprar horas.',
          ),
        ),
      );
    }
    final enCurso = compra.isLoading;

    return CockpitScaffold(
      appBar: AppBar(title: const Text('Resumen de la compra')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(plan.nombre, style: tema.textTheme.titleLarge),
                    const SizedBox(height: 2),
                    Text(
                      plan.horas == 1 ? '1 hora' : '${plan.horas} horas',
                      style: tema.textTheme.bodyMedium,
                    ),
                    const Divider(height: 28),
                    Text('Pagarás', style: tema.textTheme.bodySmall),
                    Text(
                      formatearUsd(plan.costoUsd),
                      key: const Key('montoAPagar'),
                      style: tema.textTheme.headlineSmall?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    Text(
                      '≈ ${formatearBs(plan.costoBs)}',
                      key: const Key('equivalenteEnBs'),
                      style: tema.textTheme.bodyMedium?.copyWith(
                        color: tema.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),
            const AvisoCockpit(
              texto:
                  'El pago es seguro: se hace con tarjeta en la página de la pasarela de pago. DriveHub no ve los '
                  'datos de tu tarjeta.',
            ),
            if (compra.hasError && !enCurso) ...[
              const SizedBox(height: 12),
              AvisoCockpit(
                texto: mensajeDeErrorDeCompra(compra.error!),
                tono: TonoAviso.error,
              ),
            ],
            const SizedBox(height: 20),
            FilledButton(
              key: const Key('botonConfirmarPago'),
              onPressed: enCurso ? null : () => _confirmar(context, ref),
              child: enCurso
                  ? const Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        IndicadorBoton(),
                        SizedBox(width: 10),
                        Text('Procesando…'),
                      ],
                    )
                  : const Text('Confirmar y pagar'),
            ),
            const SizedBox(height: 8),
            TextButton(
              onPressed: enCurso ? null : () => context.pop(),
              child: const Text('Elegir otro plan'),
            ),
          ],
        ),
      ),
    );
  }
}
