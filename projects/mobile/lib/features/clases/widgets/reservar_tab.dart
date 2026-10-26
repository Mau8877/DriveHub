import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../../pagos/providers/saldo_horas_provider.dart';
import '../models/franja_disponible.dart';
import '../models/momento_dia.dart';
import '../providers/franjas_disponibles_provider.dart';
import '../providers/franjas_seleccionadas_provider.dart';
import 'filtros_reservar.dart';
import 'formato_clases.dart';
import 'franja_card.dart';

/// La pestaña «Reservar» (CU14): los horarios libres de los instructores de la escuela, con filtros de día, momento e
/// instructor, para elegir las franjas que el estudiante quiere.
///
/// * Elegir solo marca: las horas se gastan al confirmar (la barra «Confirmar» la pone la pantalla).
/// * No deja elegir más franjas que horas tiene ni más de [maximoPorConfirmacion]; al intentarlo explica por qué.
/// * Las elegidas se conservan al cambiar de filtro, aunque la franja ya no esté en la lista visible.
class ReservarTab extends ConsumerWidget {
  const ReservarTab({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final franjas = ref.watch(franjasDisponiblesListProvider);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const FiltrosReservar(),
        const SizedBox(height: 16),
        franjas.when(
          loading: () => const Padding(
            padding: EdgeInsets.all(32),
            child: Center(child: CircularProgressIndicator()),
          ),
          error: (error, _) => AvisoCockpit(
            key: const Key('errorFranjas'),
            tono: TonoAviso.error,
            texto: mensajeDeError(error),
            accion: TextButton(
              onPressed: () => ref.invalidate(franjasDisponiblesListProvider),
              child: const Text('Reintentar'),
            ),
          ),
          data: (lista) => lista.isEmpty ? _SinFranjas() : _ListaDeFranjas(lista),
        ),
      ],
    );
  }
}

class _SinFranjas extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final hayFiltros =
        ref.watch(reservarMomentoUiProvider) != MomentoDia.todos || ref.watch(reservarInstructorUiProvider) != null;

    return Card(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const EstadoVacio(
            icono: Icons.event_busy_outlined,
            titulo: 'No hay horarios libres con estos filtros',
            texto: 'Prueba con otro día, otro momento del día u otro instructor.',
          ),
          if (hayFiltros)
            Padding(
              padding: const EdgeInsets.only(bottom: 20),
              child: OutlinedButton(
                key: const Key('quitarFiltros'),
                onPressed: () {
                  ref.read(reservarMomentoUiProvider.notifier).elegir(MomentoDia.todos);
                  ref.read(reservarInstructorUiProvider.notifier).elegir(null);
                },
                child: const Text('Quitar filtros'),
              ),
            ),
        ],
      ),
    );
  }
}

class _ListaDeFranjas extends ConsumerWidget {
  final List<FranjaDisponible> franjas;

  const _ListaDeFranjas(this.franjas);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final elegidas = ref.watch(franjasSeleccionadasUiProvider);
    // * Mientras el saldo carga o falla no se deja elegir: sin saber las horas no se sabe cuántas caben
    final horas = ref.watch(saldoHorasProvider).maybeWhen(data: (h) => h, orElse: () => 0);
    final tope = math.min(horas, maximoPorConfirmacion);
    final alLimite = elegidas.length >= tope;
    final tema = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: Text(
            tituloDeDia(franjas.first.fecha, DateTime.now()),
            style: tema.textTheme.titleSmall?.copyWith(color: tema.colorScheme.onSurfaceVariant),
          ),
        ),
        for (final franja in franjas) ...[
          FranjaCard(
            key: Key('franja-${franja.codigo}'),
            franja: franja,
            elegida: elegidas.containsKey(franja.codigo),
            deshabilitada: alLimite,
            alTocar: () {
              final pudo = ref
                  .read(franjasSeleccionadasUiProvider.notifier)
                  .alternar(franja, horasDisponibles: horas);
              if (!pudo) avisar(context, _motivoDelLimite(horas));
            },
          ),
          const SizedBox(height: 10),
        ],
      ],
    );
  }

  static String _motivoDelLimite(int horas) {
    if (horas <= 0) return 'No tienes horas: compra un plan para poder reservar.';
    if (horas >= maximoPorConfirmacion) return 'Puedes reservar hasta $maximoPorConfirmacion clases a la vez.';
    return 'Ya elegiste todas las clases que te alcanzan.';
  }
}
