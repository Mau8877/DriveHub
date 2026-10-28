import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../../../shared/widgets/insignia.dart';
import '../../pagos/widgets/formato_fecha.dart';
import '../models/intento_resumen.dart';
import '../models/simulacro_disponible.dart';
import '../providers/rendir_provider.dart';
import '../providers/simulacros_list_provider.dart';
import '../widgets/mensajes_de_simulacro.dart';

/// Los simulacros escritos del estudiante (CU21, dentro de la sección «Estudio»): los que ofrece su escuela con el
/// botón «Rendir» y el historial de sus intentos.
///
/// * Al pulsar «Rendir» se pide confirmar (el tiempo empieza a contar), se inicia el intento y se abre la pantalla
///   de rendir. Un intento terminado se abre en su corrección.
class SimulacrosScreen extends ConsumerWidget {
  const SimulacrosScreen({super.key});

  Future<void> _rendir(
    BuildContext context,
    WidgetRef ref,
    SimulacroDisponible simulacro,
  ) async {
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (contexto) => AlertDialog(
        title: Text(simulacro.nombre),
        content: Text(
          '${simulacro.nroPreguntas} preguntas en ${simulacro.duracionMin} minutos. '
          'Apruebas con ${formatearPuntaje(simulacro.puntajeAprobacion)}% de aciertos. '
          'El tiempo empieza a contar al comenzar.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(contexto, false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            key: const Key('botonComenzarSimulacro'),
            onPressed: () => Navigator.pop(contexto, true),
            child: const Text('Comenzar'),
          ),
        ],
      ),
    );
    if (confirmado != true || !context.mounted) return;
    try {
      await ref.read(rendirProvider.notifier).iniciar(simulacro.codigo);
    } catch (error) {
      if (context.mounted) avisar(context, mensajeDeErrorDeSimulacro(error));
      return;
    }
    if (context.mounted) context.push('/study/simulacros/rendir');
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final disponibles = ref.watch(simulacrosDisponiblesProvider);
    final historial = ref.watch(intentosHistorialProvider);

    return RefreshIndicator(
      onRefresh: () async {
        ref.invalidate(intentosHistorialProvider);
        ref.invalidate(simulacrosDisponiblesProvider);
        await ref.read(simulacrosDisponiblesProvider.future);
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
        children: [
          Text('Simulacros', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 8),
          ...disponibles.when(
            loading: () => const [
              Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: CircularProgressIndicator()),
              ),
            ],
            error: (_, _) => [
              const AvisoCockpit(
                texto:
                    'No pudimos cargar los simulacros. Revisa tu conexión e inténtalo de nuevo.',
                tono: TonoAviso.error,
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                key: const Key('botonReintentarSimulacros'),
                onPressed: () => ref.invalidate(simulacrosDisponiblesProvider),
                child: const Text('Reintentar'),
              ),
            ],
            data: (lista) => lista.isEmpty
                ? const [
                    EstadoVacio(
                      icono: Icons.quiz_outlined,
                      titulo: 'No hay simulacros disponibles',
                      texto: 'Cuando tu escuela publique uno, aparecerá aquí.',
                    ),
                  ]
                : [
                    for (final s in lista)
                      _TarjetaSimulacro(
                        simulacro: s,
                        onRendir: () => _rendir(context, ref, s),
                      ),
                  ],
          ),
          const SizedBox(height: 20),
          Text('Mis intentos', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 8),
          ...historial.when(
            loading: () => const [
              Padding(
                padding: EdgeInsets.all(16),
                child: Center(child: CircularProgressIndicator()),
              ),
            ],
            error: (_, _) => [
              const AvisoCockpit(
                texto: 'No pudimos cargar tus intentos.',
                tono: TonoAviso.error,
              ),
              const SizedBox(height: 8),
              OutlinedButton(
                onPressed: () => ref.invalidate(intentosHistorialProvider),
                child: const Text('Reintentar'),
              ),
            ],
            data: (lista) => lista.isEmpty
                ? const [
                    EstadoVacio(
                      icono: Icons.history,
                      titulo: 'Todavía no rindes ningún simulacro',
                      texto: 'Tus intentos y sus correcciones aparecerán aquí.',
                    ),
                  ]
                : [for (final i in lista) _TarjetaIntento(intento: i)],
          ),
        ],
      ),
    );
  }
}

class _TarjetaSimulacro extends StatelessWidget {
  final SimulacroDisponible simulacro;
  final VoidCallback onRendir;

  const _TarjetaSimulacro({required this.simulacro, required this.onRendir});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Card(
      key: Key('simulacro-${simulacro.codigo}'),
      margin: const EdgeInsets.only(bottom: 10),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    simulacro.nombre,
                    style: tema.textTheme.titleMedium,
                  ),
                ),
                Insignia(simulacro.categoria, tono: TonoInsignia.info),
              ],
            ),
            const SizedBox(height: 6),
            Text(
              '${simulacro.nroPreguntas} preguntas · ${simulacro.duracionMin} min · '
              'aprueba con ${formatearPuntaje(simulacro.puntajeAprobacion)}%',
              style: tema.textTheme.bodySmall,
            ),
            const SizedBox(height: 12),
            FilledButton(onPressed: onRendir, child: const Text('Rendir')),
          ],
        ),
      ),
    );
  }
}

class _TarjetaIntento extends StatelessWidget {
  final IntentoResumen intento;

  const _TarjetaIntento({required this.intento});

  @override
  Widget build(BuildContext context) {
    final (texto, tono) = intento.expirado
        ? ('Sin tiempo', TonoInsignia.advertencia)
        : intento.aprobado
        ? ('Aprobado', TonoInsignia.exito)
        : ('No aprobado', TonoInsignia.neutro);
    return Card(
      key: Key('intento-${intento.codigo}'),
      margin: const EdgeInsets.only(bottom: 10),
      child: ListTile(
        onTap: () => context.push('/study/attempts/${intento.codigo}'),
        title: Text(intento.simulacroNombre),
        subtitle: Text(
          '${formatearFechaCorta(intento.iniciadoEn)} · ${intento.aciertos} de ${intento.total} · '
          '${formatearPuntaje(intento.puntaje)}%',
        ),
        trailing: Insignia(texto, tono: tono),
      ),
    );
  }
}
