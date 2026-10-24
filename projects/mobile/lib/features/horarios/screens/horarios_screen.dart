import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/app_theme.dart';
import '../models/estado_plantilla.dart';
import '../models/fechas.dart';
import '../models/plantilla_horario.dart';
import '../providers/horarios_provider.dart';
import '../widgets/dias_semana.dart';
import '../widgets/errores.dart';
import '../widgets/status_chip.dart';
import 'franjas_view.dart';

/// La sección «Horarios» del instructor (CU12): dos pestañas, Plantillas y Franjas.
///
/// * Es un widget de cuerpo, sin `Scaffold`: se muestra dentro de la sección «Horarios» del
/// * `InstructorShell`. El formulario es una pantalla aparte (`/schedules/new`).
class HorariosScreen extends ConsumerWidget {
  const HorariosScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final pestana = ref.watch(horariosPestanaUiProvider);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
          child: SegmentedButton<PestanaHorarios>(
            key: const Key('pestanasHorarios'),
            showSelectedIcon: false,
            segments: const [
              ButtonSegment(value: PestanaHorarios.plantillas, label: Text('Plantillas')),
              ButtonSegment(value: PestanaHorarios.franjas, label: Text('Franjas')),
            ],
            selected: {pestana},
            onSelectionChanged: (elegida) => ref.read(horariosPestanaUiProvider.notifier).elegir(elegida.first),
          ),
        ),
        Expanded(child: pestana == PestanaHorarios.plantillas ? const _Plantillas() : const FranjasView()),
      ],
    );
  }
}

class _Plantillas extends ConsumerWidget {
  const _Plantillas();

  Future<bool> _confirmar(BuildContext context, String titulo, String cuerpo, String accion) async {
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (dialogo) => AlertDialog(
        title: Text(titulo),
        content: Text(cuerpo),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogo, false), child: const Text('Cancelar')),
          FilledButton(
            onPressed: () => Navigator.pop(dialogo, true),
            style: FilledButton.styleFrom(minimumSize: const Size(0, 48)),
            child: Text(accion),
          ),
        ],
      ),
    );
    return confirmado == true;
  }

  Future<void> _pausar(BuildContext context, WidgetRef ref, PlantillaHorario horario) async {
    final mensajes = ScaffoldMessenger.of(context);
    final confirmado = await _confirmar(
      context,
      'Pausar horario',
      'Se quitarán las franjas futuras que nadie reservó. Las reservadas se conservan y podrás reanudarlo cuando quieras.',
      'Pausar',
    );
    if (!confirmado) return;
    try {
      await ref.read(horariosProvider.notifier).pausar(horario.codigo);
      mensajes.showSnackBar(const SnackBar(content: Text('Horario pausado')));
    } catch (e) {
      mensajes.showSnackBar(SnackBar(content: Text(mensajeDeError(e))));
    }
  }

  Future<void> _reanudar(BuildContext context, WidgetRef ref, PlantillaHorario horario) async {
    final mensajes = ScaffoldMessenger.of(context);
    try {
      await ref.read(horariosProvider.notifier).reanudar(horario.codigo);
      mensajes.showSnackBar(const SnackBar(content: Text('Horario reanudado')));
    } catch (e) {
      mensajes.showSnackBar(SnackBar(content: Text(mensajeDeError(e))));
    }
  }

  Future<void> _desactivar(BuildContext context, WidgetRef ref, PlantillaHorario horario) async {
    final mensajes = ScaffoldMessenger.of(context);
    final confirmado = await _confirmar(
      context,
      'Desactivar horario',
      'Se quitarán las franjas futuras que nadie reservó. Las reservadas se conservan. Un horario desactivado no se puede volver a activar.',
      'Desactivar',
    );
    if (!confirmado) return;
    try {
      await ref.read(horariosProvider.notifier).desactivar(horario.codigo);
      mensajes.showSnackBar(const SnackBar(content: Text('Horario desactivado')));
    } catch (e) {
      mensajes.showSnackBar(SnackBar(content: Text(mensajeDeError(e))));
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final horarios = ref.watch(horariosListProvider);
    final incluirInactivos = ref.watch(horariosIncluirInactivosUiProvider);

    return Column(
      children: [
        SwitchListTile(
          key: const Key('interruptorInactivos'),
          title: const Text('Ver inactivos'),
          value: incluirInactivos,
          onChanged: (valor) => ref.read(horariosIncluirInactivosUiProvider.notifier).fijar(valor),
        ),
        Expanded(
          child: horarios.when(
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (error, _) => Center(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(mensajeDeError(error), textAlign: TextAlign.center),
                    const SizedBox(height: 8),
                    OutlinedButton(
                      onPressed: () => ref.invalidate(horariosListProvider),
                      style: OutlinedButton.styleFrom(minimumSize: const Size(0, 48)),
                      child: const Text('Reintentar'),
                    ),
                  ],
                ),
              ),
            ),
            data: (lista) {
              if (lista.isEmpty) {
                return const Center(child: Text('Todavía no tienes horarios. Crea el primero.'));
              }
              return ListView.separated(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                itemCount: lista.length,
                separatorBuilder: (_, _) => const SizedBox(height: 12),
                itemBuilder: (_, i) => _TarjetaHorario(
                  horario: lista[i],
                  alPausar: () => _pausar(context, ref, lista[i]),
                  alReanudar: () => _reanudar(context, ref, lista[i]),
                  alDesactivar: () => _desactivar(context, ref, lista[i]),
                ),
              );
            },
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
          child: FilledButton.icon(
            key: const Key('botonNuevoHorario'),
            onPressed: () => context.push('/schedules/new'),
            icon: const Icon(Icons.add),
            label: const Text('Nuevo horario'),
          ),
        ),
      ],
    );
  }
}

class _TarjetaHorario extends StatelessWidget {
  final PlantillaHorario horario;
  final VoidCallback alPausar;
  final VoidCallback alReanudar;
  final VoidCallback alDesactivar;

  const _TarjetaHorario({
    required this.horario,
    required this.alPausar,
    required this.alReanudar,
    required this.alDesactivar,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final vigencia = horario.vigenteHasta == null
        ? 'Desde el ${fechaParaMostrar(horario.vigenteDesde)}'
        : 'Del ${fechaParaMostrar(horario.vigenteDesde)} al ${fechaParaMostrar(horario.vigenteHasta!)}';
    final franjas = horario.franjasPorSemana;
    final apagada = horario.estado != EstadoPlantilla.activa;

    // * Pausada e inactiva se dibujan como tarjeta vacía: borde y sin sombra 
    return Container(
      padding: const EdgeInsets.fromLTRB(20, 18, 8, 18),
      decoration: BoxDecoration(
        color: apagada ? Colors.transparent : tema.colorScheme.surface,
        borderRadius: BorderRadius.circular(AppRadios.tarjeta),
        border: Border.all(color: tema.colorScheme.outline, width: apagada ? 1.5 : 1),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(horario.nombre, style: tema.textTheme.titleLarge),
                const SizedBox(height: 8),
                _ChipDeEstado(estado: horario.estado),
                const SizedBox(height: 12),
                DiasDeLaSemana(dias: horario.diasSemana),
                const SizedBox(height: 12),
                Text(
                  '${horario.horaInicio} – ${horario.horaFin} · ${horario.duracionClaseMin} min',
                  style: tema.textTheme.labelLarge?.copyWith(color: context.cockpit.enlace, fontWeight: FontWeight.w800),
                ),
                const SizedBox(height: 4),
                Text('$vigencia · $franjas franjas por semana', style: tema.textTheme.bodyMedium),
              ],
            ),
          ),
          if (horario.estado != EstadoPlantilla.inactiva)
            PopupMenuButton<String>(
              tooltip: 'Opciones del horario',
              onSelected: (opcion) {
                switch (opcion) {
                  case 'pausar':
                    alPausar();
                  case 'reanudar':
                    alReanudar();
                  case 'desactivar':
                    alDesactivar();
                }
              },
              itemBuilder: (_) => [
                if (horario.estado == EstadoPlantilla.activa)
                  const PopupMenuItem(value: 'pausar', child: Text('Pausar')),
                if (horario.estado == EstadoPlantilla.pausada)
                  const PopupMenuItem(value: 'reanudar', child: Text('Reanudar')),
                const PopupMenuItem(value: 'desactivar', child: Text('Desactivar')),
              ],
            ),
        ],
      ),
    );
  }
}

class _ChipDeEstado extends StatelessWidget {
  final EstadoPlantilla estado;

  const _ChipDeEstado({required this.estado});

  @override
  Widget build(BuildContext context) {
    return switch (estado) {
      EstadoPlantilla.activa => StatusChip.exito(context, 'Activa'),
      EstadoPlantilla.pausada => StatusChip.neutro(context, 'Pausada', Icons.schedule),
      EstadoPlantilla.inactiva => StatusChip.neutro(context, 'Inactivo', Icons.block, atenuado: true),
    };
  }
}
