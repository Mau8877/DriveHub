import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../models/tutorial.dart';
import '../providers/tutoriales_providers.dart';
import '../widgets/tarjeta_tutorial.dart';

/// «Tutoriales» (CU35): los tutoriales publicados por los instructores de la escuela y dirigidos a quien los mira
/// (estudiantes o instructores), con búsqueda y un filtro por tipo. Tocar uno lo abre dentro de la app: el PDF en el
/// visor y el video en el reproductor.
///
/// * La búsqueda se envía al pulsar «buscar» en el teclado, no en cada letra.
/// * Deslizar hacia abajo lo vuelve a pedir.
class BibliotecaTutorialesView extends ConsumerStatefulWidget {
  const BibliotecaTutorialesView({super.key});

  /// Los chips del filtro: el texto y el tipo que piden (`null` = todos).
  static const _tipos = <(String, TipoTutorial?)>[
    ('Todos', null),
    ('Videos', TipoTutorial.enlace),
    ('PDF', TipoTutorial.pdf),
  ];

  @override
  ConsumerState<BibliotecaTutorialesView> createState() =>
      _BibliotecaTutorialesViewState();
}

class _BibliotecaTutorialesViewState
    extends ConsumerState<BibliotecaTutorialesView> {
  final _busqueda = TextEditingController();

  @override
  void dispose() {
    _busqueda.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final tutoriales = ref.watch(bibliotecaTutorialesProvider);
    final filtro = ref.watch(bibliotecaFiltroUiProvider);
    final hayFiltro = filtro.busqueda.trim().isNotEmpty || filtro.tipo != null;

    return RefreshIndicator(
      onRefresh: () async {
        ref.invalidate(bibliotecaTutorialesProvider);
        await ref.read(bibliotecaTutorialesProvider.future);
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
        children: [
          TextField(
            key: const Key('campoBuscarTutorial'),
            controller: _busqueda,
            textInputAction: TextInputAction.search,
            decoration: const InputDecoration(
              hintText: 'Buscar por título, tema o etiqueta',
              prefixIcon: Icon(Icons.search),
            ),
            onSubmitted: ref.read(bibliotecaFiltroUiProvider.notifier).buscar,
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 4,
            children: [
              for (final (texto, tipo) in BibliotecaTutorialesView._tipos)
                ChoiceChip(
                  key: Key('tipo-${tipo?.valorApi ?? 'TODOS'}'),
                  label: Text(texto),
                  selected: filtro.tipo == tipo,
                  onSelected: (_) => ref
                      .read(bibliotecaFiltroUiProvider.notifier)
                      .elegirTipo(tipo),
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
                    'No pudimos cargar los tutoriales. Revisa tu conexión e inténtalo de nuevo.',
                tono: TonoAviso.error,
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                key: const Key('botonReintentarTutoriales'),
                onPressed: () => ref.invalidate(bibliotecaTutorialesProvider),
                child: const Text('Reintentar'),
              ),
            ],
            data: (lista) => lista.isEmpty
                ? [
                    EstadoVacio(
                      icono: Icons.ondemand_video_outlined,
                      titulo: hayFiltro
                          ? 'No encontramos tutoriales'
                          : 'Todavía no hay tutoriales',
                      texto: hayFiltro
                          ? 'Prueba con otra búsqueda o con otro tipo.'
                          : 'Cuando un instructor de tu escuela publique un tutorial, lo verás aquí.',
                    ),
                  ]
                : [for (final t in lista) TarjetaTutorial(tutorial: t)],
          ),
        ],
      ),
    );
  }
}
