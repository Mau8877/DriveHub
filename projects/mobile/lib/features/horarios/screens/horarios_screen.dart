import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../models/fechas.dart';
import '../models/plantilla_horario.dart';
import '../providers/horarios_provider.dart';
import '../widgets/errores.dart';

/// Los horarios semanales del instructor (CU12).
///
/// * Es un widget de cuerpo, sin `Scaffold`: se muestra dentro de la sección "Horarios" del
/// * `UsuarioShell`. El formulario y las franjas son pantallas aparte (`/schedules/new` y
/// * `/schedules/slots`).
class HorariosScreen extends ConsumerWidget {
  const HorariosScreen({super.key});

  Future<void> _desactivar(BuildContext context, WidgetRef ref, PlantillaHorario horario) async {
    final mensajes = ScaffoldMessenger.of(context);
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (dialogo) => AlertDialog(
        title: const Text('Desactivar horario'),
        content: const Text('Se quitarán las franjas futuras que nadie reservó. Las reservadas se conservan.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogo, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(dialogo, true), child: const Text('Desactivar')),
        ],
      ),
    );
    if (confirmado != true) return;
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
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
          child: Row(
            children: [
              Expanded(
                child: FilledButton.icon(
                  key: const Key('botonNuevoHorario'),
                  onPressed: () => context.push('/schedules/new'),
                  icon: const Icon(Icons.add),
                  label: const Text('Nuevo horario'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton.icon(
                  key: const Key('botonVerFranjas'),
                  onPressed: () => context.push('/schedules/slots'),
                  icon: const Icon(Icons.event_note_outlined),
                  label: const Text('Ver franjas'),
                ),
              ),
            ],
          ),
        ),
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
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(mensajeDeError(error), textAlign: TextAlign.center),
                  const SizedBox(height: 8),
                  OutlinedButton(
                    onPressed: () => ref.invalidate(horariosListProvider),
                    child: const Text('Reintentar'),
                  ),
                ],
              ),
            ),
            data: (lista) {
              if (lista.isEmpty) {
                return const Center(child: Text('Todavía no tienes horarios. Crea el primero.'));
              }
              return ListView.separated(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                itemCount: lista.length,
                separatorBuilder: (_, _) => const SizedBox(height: 8),
                itemBuilder: (_, i) => _FilaHorario(
                  horario: lista[i],
                  alDesactivar: () => _desactivar(context, ref, lista[i]),
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}

class _FilaHorario extends StatelessWidget {
  final PlantillaHorario horario;
  final VoidCallback alDesactivar;

  const _FilaHorario({required this.horario, required this.alDesactivar});

  @override
  Widget build(BuildContext context) {
    final esquema = Theme.of(context).colorScheme;
    final dia = nombreDeDia(horario.diaSemana);
    final vigencia = horario.vigenteHasta == null
        ? 'desde ${fechaParaMostrar(horario.vigenteDesde)}'
        : 'del ${fechaParaMostrar(horario.vigenteDesde)} al ${fechaParaMostrar(horario.vigenteHasta!)}';

    return Card(
      child: ListTile(
        title: Text('${dia[0].toUpperCase()}${dia.substring(1)} · ${horario.horaInicio} – ${horario.horaFin}'),
        subtitle: Text('Clases de ${horario.duracionClaseMin} min · $vigencia'),
        leading: horario.activa
            ? Icon(Icons.event_available_outlined, color: esquema.primary)
            : Icon(Icons.event_busy_outlined, color: esquema.outline),
        trailing: horario.activa
            ? PopupMenuButton<String>(
                tooltip: 'Opciones del horario',
                onSelected: (_) => alDesactivar(),
                itemBuilder: (_) => const [PopupMenuItem(value: 'desactivar', child: Text('Desactivar'))],
              )
            : Chip(
                label: const Text('Inactivo'),
                backgroundColor: esquema.surfaceContainerHighest,
              ),
      ),
    );
  }
}
