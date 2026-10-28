import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/archivos/abridor_de_pdf.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../../../shared/widgets/insignia.dart';
import '../models/manual.dart';
import '../providers/manuales_providers.dart';

/// El texto de aviso de un error al bajar o abrir un manual (CU34), en español y sin términos técnicos.
String mensajeDeErrorDeManual(Object error) {
  if (error is SinVisorDePdf) {
    return 'No hay ninguna app para abrir PDF en tu teléfono.';
  }
  if (error is DioException &&
      (error.type == DioExceptionType.connectionError ||
          error.type == DioExceptionType.connectionTimeout ||
          error.type == DioExceptionType.sendTimeout ||
          error.type == DioExceptionType.receiveTimeout)) {
    return 'No hay conexión. Revisa tu internet e inténtalo de nuevo.';
  }
  return 'No pudimos abrir el manual. Inténtalo de nuevo más tarde.';
}

/// «Manuales» (CU34, dentro de la sección «Estudio»): los manuales publicados por la escuela, con búsqueda y un
/// filtro por categoría. Tocar «Abrir PDF» baja el archivo y lo abre en el visor del teléfono.
///
/// * La búsqueda se envía al pulsar «buscar» en el teclado, no en cada letra.
/// * Deslizar hacia abajo lo vuelve a pedir.
class ManualesScreen extends ConsumerStatefulWidget {
  const ManualesScreen({super.key});

  /// Los chips del filtro: el texto y la categoría que piden (`null` = todas).
  static const _categorias = <(String, String?)>[
    ('Todos', null),
    ('General', 'GENERAL'),
    ('Categoría A', 'A'),
    ('Categoría B', 'B'),
    ('Categoría C', 'C'),
  ];

  @override
  ConsumerState<ManualesScreen> createState() => _ManualesScreenState();
}

class _ManualesScreenState extends ConsumerState<ManualesScreen> {
  final _busqueda = TextEditingController();

  @override
  void dispose() {
    _busqueda.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final manuales = ref.watch(manualesListProvider);
    final filtro = ref.watch(manualesFiltroUiProvider);
    final hayFiltro =
        filtro.busqueda.trim().isNotEmpty || filtro.categoria != null;

    return RefreshIndicator(
      onRefresh: () async {
        ref.invalidate(manualesListProvider);
        await ref.read(manualesListProvider.future);
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
        children: [
          TextField(
            key: const Key('campoBuscarManual'),
            controller: _busqueda,
            textInputAction: TextInputAction.search,
            decoration: const InputDecoration(
              hintText: 'Buscar por título, tema o etiqueta',
              prefixIcon: Icon(Icons.search),
            ),
            onSubmitted: ref.read(manualesFiltroUiProvider.notifier).buscar,
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 4,
            children: [
              for (final (texto, categoria) in ManualesScreen._categorias)
                ChoiceChip(
                  key: Key('categoria-${categoria ?? 'TODOS'}'),
                  label: Text(texto),
                  selected: filtro.categoria == categoria,
                  onSelected: (_) => ref
                      .read(manualesFiltroUiProvider.notifier)
                      .elegirCategoria(categoria),
                ),
            ],
          ),
          const SizedBox(height: 12),
          ...manuales.when(
            loading: () => const [
              Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: CircularProgressIndicator()),
              ),
            ],
            error: (_, _) => [
              const AvisoCockpit(
                texto:
                    'No pudimos cargar los manuales. Revisa tu conexión e inténtalo de nuevo.',
                tono: TonoAviso.error,
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                key: const Key('botonReintentarManuales'),
                onPressed: () => ref.invalidate(manualesListProvider),
                child: const Text('Reintentar'),
              ),
            ],
            data: (lista) => lista.isEmpty
                ? [
                    EstadoVacio(
                      icono: Icons.menu_book_outlined,
                      titulo: hayFiltro
                          ? 'No encontramos manuales'
                          : 'Todavía no hay manuales',
                      texto: hayFiltro
                          ? 'Prueba con otra búsqueda o con otra categoría.'
                          : 'Cuando tu escuela publique un manual, lo verás aquí.',
                    ),
                  ]
                : [for (final manual in lista) _TarjetaManual(manual: manual)],
          ),
        ],
      ),
    );
  }
}

String _tamano(int bytes) => bytes >= 1024 * 1024
    ? '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB'
    : '${(bytes / 1024).ceil()} KB';

class _TarjetaManual extends ConsumerWidget {
  final Manual manual;

  const _TarjetaManual({required this.manual});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final llave = (manual.codigo, manual.archivoUrl);
    final descargando = ref.watch(manualDescargaProvider(llave)).isLoading;
    final detalle = [
      ?manual.tema,
      if (manual.tamanoBytes != null) _tamano(manual.tamanoBytes!),
      'versión ${manual.version}',
    ].join(' · ');

    return Card(
      key: Key('manual-${manual.codigo}'),
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
                    manual.titulo,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                const SizedBox(width: 8),
                Insignia(
                  manual.categoria == 'GENERAL' ? 'General' : manual.categoria,
                  tono: TonoInsignia.info,
                ),
              ],
            ),
            if (manual.descripcion != null) ...[
              const SizedBox(height: 6),
              Text(manual.descripcion!),
            ],
            const SizedBox(height: 6),
            Text(detalle, style: Theme.of(context).textTheme.bodySmall),
            if (manual.etiquetas.isNotEmpty) ...[
              const SizedBox(height: 8),
              Wrap(
                spacing: 6,
                runSpacing: 4,
                children: [
                  for (final etiqueta in manual.etiquetas) Insignia(etiqueta),
                ],
              ),
            ],
            const SizedBox(height: 12),
            OutlinedButton.icon(
              key: Key('botonAbrirManual-${manual.codigo}'),
              // * El tema estira los botones a todo el ancho; este va debajo del texto, sin necesidad de más
              style: OutlinedButton.styleFrom(minimumSize: const Size(0, 40)),
              onPressed: descargando
                  ? null
                  : () async {
                      final abierto = await ref
                          .read(manualDescargaProvider(llave).notifier)
                          .descargarYAbrir();
                      if (abierto || !context.mounted) return;
                      final error = ref
                          .read(manualDescargaProvider(llave))
                          .error;
                      if (error != null) {
                        avisar(context, mensajeDeErrorDeManual(error));
                      }
                    },
              icon: descargando
                  ? const IndicadorBoton()
                  : const Icon(Icons.picture_as_pdf_outlined),
              label: const Text('Abrir PDF'),
            ),
          ],
        ),
      ),
    );
  }
}
