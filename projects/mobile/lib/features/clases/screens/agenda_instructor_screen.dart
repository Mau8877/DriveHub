import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../../horarios/models/fechas.dart';
import '../providers/agenda_fecha_provider.dart';
import '../providers/agenda_provider.dart';
import '../widgets/formato_clases.dart';
import '../widgets/lista_de_agenda.dart';

/// La pestaña «Agenda» del instructor (CU16): las clases de un día, por hora, con el estudiante y el estado de cada
/// una. Arranca en hoy y se mueve de día con las flechas o eligiendo una fecha.
///
/// * Un día sin clases no es un error: muestra un aviso vacío.
/// * Si falla (sin red, Identidad caída) muestra el mensaje con «Reintentar». Deslizar hacia abajo vuelve a pedir.
class AgendaInstructorScreen extends ConsumerWidget {
  const AgendaInstructorScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final agenda = ref.watch(agendaListProvider);

    return RefreshIndicator(
      onRefresh: () async {
        ref.invalidate(agendaListProvider);
        await ref.read(agendaListProvider.future).catchError((_) => <Never>[]);
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
        children: [
          const _EncabezadoDeDia(),
          const SizedBox(height: 16),
          agenda.when(
            loading: () => const Padding(
              padding: EdgeInsets.all(32),
              child: Center(child: CircularProgressIndicator()),
            ),
            error: (error, _) => AvisoCockpit(
              key: const Key('errorAgenda'),
              tono: TonoAviso.error,
              texto: mensajeDeError(error),
              accion: TextButton(
                onPressed: () => ref.invalidate(agendaListProvider),
                child: const Text('Reintentar'),
              ),
            ),
            data: (clases) {
              if (clases.isEmpty) {
                return const Card(
                  key: Key('agendaVacia'),
                  child: EstadoVacio(
                    icono: Icons.event_available_outlined,
                    titulo: 'No tienes clases este día',
                    texto: 'Mira otro día con las flechas, o elige una fecha.',
                  ),
                );
              }
              return ListaDeAgenda(clases: clases);
            },
          ),
        ],
      ),
    );
  }
}

/// El día elegido con «día anterior» y «día siguiente»; tocar el título abre el selector de fecha y «Hoy» vuelve al
/// día de hoy (solo se ve cuando se está mirando otro día).
class _EncabezadoDeDia extends ConsumerWidget {
  const _EncabezadoDeDia();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tema = Theme.of(context);
    final fecha = ref.watch(agendaFechaUiProvider);
    final notifier = ref.read(agendaFechaUiProvider.notifier);
    final ahora = DateTime.now();
    final esHoy = fecha == soloFecha(ahora);

    return Row(
      children: [
        IconButton(
          key: const Key('diaAnterior'),
          tooltip: 'Día anterior',
          onPressed: notifier.anterior,
          icon: const Icon(Icons.chevron_left),
        ),
        Expanded(
          child: InkWell(
            key: const Key('elegirDia'),
            borderRadius: BorderRadius.circular(12),
            onTap: () async {
              final elegida = await showDatePicker(
                context: context,
                initialDate: fecha,
                firstDate: DateTime(2020),
                lastDate: DateTime(2100),
              );
              if (elegida != null) notifier.elegir(elegida);
            },
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 6),
              child: Column(
                children: [
                  Text(tituloDeDiaLargo(fecha, ahora), style: tema.textTheme.titleMedium),
                  Text(
                    fechaParaMostrar(fecha),
                    style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            ),
          ),
        ),
        if (!esHoy)
          TextButton(key: const Key('irAHoy'), onPressed: notifier.hoy, child: const Text('Hoy')),
        IconButton(
          key: const Key('diaSiguiente'),
          tooltip: 'Día siguiente',
          onPressed: notifier.siguiente,
          icon: const Icon(Icons.chevron_right),
        ),
      ],
    );
  }
}
