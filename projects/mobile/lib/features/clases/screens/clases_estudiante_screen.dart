import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shared/widgets/estado_vacio.dart';
import '../../pagos/providers/saldo_horas_provider.dart';
import '../models/vista_reservas.dart';
import '../providers/clases_pestana_provider.dart';
import '../providers/franjas_disponibles_provider.dart';
import '../widgets/barra_confirmar_reserva.dart';
import '../widgets/horas_disponibles_banner.dart';
import '../widgets/lista_de_reservas.dart';
import '../widgets/reservar_tab.dart';

/// La sección «Clases» del estudiante (CU14): sus horas disponibles siempre a la vista y tres pestañas, «Mis clases»
/// (las que ya reservó, de la más próxima a la más lejana), «Reservar» e «Historial».
///
/// * Abre en «Mis clases» si ya tiene clases por venir y en «Reservar» si no; en cuanto elige una pestaña, esa manda.
/// * Deslizar hacia abajo vuelve a pedir las clases y el saldo.
class ClasesEstudianteScreen extends ConsumerWidget {
  const ClasesEstudianteScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final elegida = ref.watch(clasesPestanaUiProvider);
    final PestanaClases pestana =
        elegida ??
        ref
            .watch(reservasListProvider(VistaReservas.proximas))
            .maybeWhen<PestanaClases>(
              data: (lista) => lista.isEmpty ? PestanaClases.reservar : PestanaClases.misClases,
              orElse: () => PestanaClases.misClases,
            );

    final lista = RefreshIndicator(
      onRefresh: () async {
        ref.invalidate(saldoHorasProvider);
        ref.invalidate(reservasListProvider(VistaReservas.proximas));
        ref.invalidate(reservasListProvider(VistaReservas.historial));
        ref.invalidate(franjasDisponiblesListProvider);
        await ref.read(reservasListProvider(VistaReservas.proximas).future).catchError((_) => <Never>[]);
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
        children: [
          const HorasDisponiblesBanner(),
          const SizedBox(height: 16),
          SegmentedButton<PestanaClases>(
            key: const Key('pestanasClases'),
            showSelectedIcon: false,
            segments: [for (final p in PestanaClases.values) ButtonSegment(value: p, label: Text(p.etiqueta))],
            selected: {pestana},
            onSelectionChanged: (elegidas) => ref.read(clasesPestanaUiProvider.notifier).elegir(elegidas.first),
          ),
          const SizedBox(height: 16),
          switch (pestana) {
            PestanaClases.misClases => ListaDeReservas(
              vista: VistaReservas.proximas,
              vacio: const EstadoVacio(
                icono: Icons.calendar_today_outlined,
                titulo: 'Aún no tienes clases reservadas',
                texto: 'Elige un horario libre y reserva tu primera clase.',
              ),
              accionVacia: FilledButton(
                key: const Key('irAReservar'),
                onPressed: () => ref.read(clasesPestanaUiProvider.notifier).elegir(PestanaClases.reservar),
                child: const Text('Reservar una clase'),
              ),
            ),
            PestanaClases.reservar => const ReservarTab(),
            PestanaClases.historial => const ListaDeReservas(
              vista: VistaReservas.historial,
              vacio: EstadoVacio(
                icono: Icons.history,
                titulo: 'Todavía no tienes clases pasadas',
                texto: 'Tus clases terminadas y canceladas aparecerán acá.',
              ),
            ),
          },
        ],
      ),
    );

    // * La barra «Confirmar» queda fija abajo, fuera de la lista que se desliza
    return Column(
      children: [
        Expanded(child: lista),
        if (pestana == PestanaClases.reservar) const BarraConfirmarReserva(),
      ],
    );
  }
}
