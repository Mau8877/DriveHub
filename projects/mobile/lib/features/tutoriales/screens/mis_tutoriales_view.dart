import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../models/tutorial.dart';
import '../providers/tutoriales_providers.dart';
import '../widgets/errores.dart';
import '../widgets/tarjeta_tutorial.dart';

/// «Mis tutoriales» (CU33): los tutoriales del instructor con un filtro por estado y, en cada uno, editarlo,
/// publicarlo o archivarlo.
///
/// * Un tutorial archivado no se edita: primero se publica de nuevo. Archivar pide confirmación.
/// * Si una acción falla se avisa con el mensaje del backend y la lista queda como estaba.
class MisTutorialesView extends ConsumerWidget {
  const MisTutorialesView({super.key});

  /// Los chips del filtro: el texto y el estado que piden (`null` = todos).
  static const _estados = <(String, EstadoTutorial?)>[
    ('Todos', null),
    ('Borradores', EstadoTutorial.borrador),
    ('Publicados', EstadoTutorial.publicado),
    ('Archivados', EstadoTutorial.archivado),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tutoriales = ref.watch(misTutorialesListProvider);
    final filtro = ref.watch(misTutorialesFiltroUiProvider);

    return RefreshIndicator(
      onRefresh: () async {
        ref.invalidate(misTutorialesListProvider);
        await ref.read(misTutorialesListProvider.future);
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 88),
        children: [
          Wrap(
            spacing: 8,
            runSpacing: 4,
            children: [
              for (final (texto, estado) in _estados)
                ChoiceChip(
                  key: Key('estado-${estado?.valorApi ?? 'TODOS'}'),
                  label: Text(texto),
                  selected: filtro == estado,
                  onSelected: (_) => ref
                      .read(misTutorialesFiltroUiProvider.notifier)
                      .elegir(estado),
                ),
            ],
          ),
          const SizedBox(height: 12),
          ...tutoriales.when(
            loading: () => const [
              Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: CircularProgressIndicator()),
              ),
            ],
            error: (_, _) => [
              const AvisoCockpit(
                texto:
                    'No pudimos cargar tus tutoriales. Revisa tu conexión e inténtalo de nuevo.',
                tono: TonoAviso.error,
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                key: const Key('botonReintentarMisTutoriales'),
                onPressed: () => ref.invalidate(misTutorialesListProvider),
                child: const Text('Reintentar'),
              ),
            ],
            data: (lista) => lista.isEmpty
                ? [
                    EstadoVacio(
                      icono: Icons.video_library_outlined,
                      titulo: filtro == null
                          ? 'Todavía no tienes tutoriales'
                          : 'No hay tutoriales en este estado',
                      texto: filtro == null
                          ? 'Crea tu primer tutorial con el botón «Nuevo tutorial».'
                          : 'Prueba con otro filtro.',
                    ),
                  ]
                : [
                    for (final t in lista)
                      TarjetaTutorial(
                        tutorial: t,
                        mostrarEstado: true,
                        acciones: _acciones(context, ref, t),
                      ),
                  ],
          ),
        ],
      ),
    );
  }

  List<Widget> _acciones(BuildContext context, WidgetRef ref, Tutorial t) {
    const compacto = Size(0, 40);
    return [
      if (t.estado != EstadoTutorial.archivado)
        TextButton(
          key: Key('botonEditarTutorial-${t.codigo}'),
          style: TextButton.styleFrom(minimumSize: compacto),
          onPressed: () => context.push('/tutorials/edit', extra: t),
          child: const Text('Editar'),
        ),
      if (t.estado != EstadoTutorial.publicado)
        TextButton(
          key: Key('botonPublicarTutorial-${t.codigo}'),
          style: TextButton.styleFrom(minimumSize: compacto),
          onPressed: () => _ejecutar(
            context,
            ref,
            () => ref.read(tutorialAccionesProvider).publicar(t.codigo),
            'Tutorial publicado',
          ),
          child: Text(
            t.estado == EstadoTutorial.archivado
                ? 'Publicar de nuevo'
                : 'Publicar',
          ),
        ),
      if (t.estado == EstadoTutorial.publicado)
        TextButton(
          key: Key('botonArchivarTutorial-${t.codigo}'),
          style: TextButton.styleFrom(minimumSize: compacto),
          onPressed: () async {
            final confirmado = await _confirmarArchivar(context, t);
            if (confirmado != true || !context.mounted) return;
            await _ejecutar(
              context,
              ref,
              () => ref.read(tutorialAccionesProvider).archivar(t.codigo),
              'Tutorial archivado',
            );
          },
          child: const Text('Archivar'),
        ),
    ];
  }

  Future<bool?> _confirmarArchivar(
    BuildContext context,
    Tutorial t,
  ) => showDialog<bool>(
    context: context,
    builder: (_) => AlertDialog(
      title: const Text('¿Archivar este tutorial?'),
      content: Text(
        '«${t.titulo}» dejará de verse para los estudiantes e instructores. Podrás publicarlo de nuevo.',
      ),
      actions: [
        TextButton(
          key: const Key('botonCancelarArchivar'),
          onPressed: () => Navigator.of(context).pop(false),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          key: const Key('botonConfirmarArchivar'),
          style: FilledButton.styleFrom(minimumSize: const Size(0, 44)),
          onPressed: () => Navigator.of(context).pop(true),
          child: const Text('Archivar'),
        ),
      ],
    ),
  );

  Future<void> _ejecutar(
    BuildContext context,
    WidgetRef ref,
    Future<Tutorial> Function() accion,
    String exito,
  ) async {
    try {
      await accion();
      if (context.mounted) avisar(context, exito);
    } catch (error) {
      if (context.mounted) avisar(context, mensajeDeErrorDeTutorial(error));
    }
  }
}
