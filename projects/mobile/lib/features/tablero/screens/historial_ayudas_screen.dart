import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../../../shared/widgets/insignia.dart';
import '../../pagos/widgets/formato_fecha.dart';
import '../models/ayuda_tablero.dart';
import '../models/luz_tablero.dart';
import '../providers/tablero_providers.dart';

TonoInsignia _tono(SeveridadTablero severidad) => switch (severidad) {
  SeveridadTablero.informativa => TonoInsignia.info,
  SeveridadTablero.advertencia => TonoInsignia.advertencia,
  SeveridadTablero.critica => TonoInsignia.advertencia,
};

/// «Historial de ayudas» (CU42, `/study/tablero/historial`): las ayudas del tablero que la persona guardó, de la más
/// reciente a la más antigua. Tocar una abre su detalle, donde puede decir si le sirvió.
///
/// * Solo se ven las propias. Deslizar hacia abajo lo vuelve a pedir.
/// * La miniatura es opcional: sin ella se muestra un icono.
class HistorialAyudasScreen extends ConsumerWidget {
  const HistorialAyudasScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ayudas = ref.watch(historialAyudasProvider);

    return CockpitScaffold(
      appBar: AppBar(title: const Text('Historial de ayudas')),
      body: RefreshIndicator(
        onRefresh: () async {
          ref.invalidate(historialAyudasProvider);
          await ref.read(historialAyudasProvider.future);
        },
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
          children: ayudas.when(
            loading: () => const [
              Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: CircularProgressIndicator()),
              ),
            ],
            error: (_, _) => [
              const AvisoCockpit(
                texto:
                    'No pudimos cargar tu historial. Revisa tu conexión e inténtalo de nuevo.',
                tono: TonoAviso.error,
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                key: const Key('botonReintentarHistorial'),
                onPressed: () => ref.invalidate(historialAyudasProvider),
                child: const Text('Reintentar'),
              ),
            ],
            data: (lista) => lista.isEmpty
                ? const [
                    EstadoVacio(
                      icono: Icons.history,
                      titulo: 'Todavía no tienes ayudas guardadas',
                      texto:
                          'Cuando analices una luz del tablero con conexión, aparecerá aquí.',
                    ),
                  ]
                : [for (final ayuda in lista) _TarjetaAyuda(ayuda: ayuda)],
          ),
        ),
      ),
    );
  }
}

class _Miniatura extends StatelessWidget {
  final String? url;
  final double lado;

  const _Miniatura({required this.url, required this.lado});

  @override
  Widget build(BuildContext context) {
    const icono = Icon(Icons.dashboard_outlined);
    return SizedBox(
      width: lado,
      height: lado,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: url == null
            ? const Center(child: icono)
            : Image.network(
                url!,
                fit: BoxFit.cover,
                // * Si la miniatura ya no carga (sin red, borrada), el icono ocupa su lugar
                errorBuilder: (_, _, _) => const Center(child: icono),
              ),
      ),
    );
  }
}

class _TarjetaAyuda extends StatelessWidget {
  final AyudaTablero ayuda;

  const _TarjetaAyuda({required this.ayuda});

  @override
  Widget build(BuildContext context) {
    final principal = ayuda.principal;

    return Card(
      key: Key('ayuda-${ayuda.codigo}'),
      margin: const EdgeInsets.only(bottom: 10),
      child: ListTile(
        leading: _Miniatura(url: ayuda.imagenUrl, lado: 48),
        title: Text(principal?.nombre ?? 'Luz del tablero'),
        subtitle: Text(
          '${formatearFechaCorta(ayuda.creadaEn.toLocal())}${ayuda.util == null
              ? ''
              : ayuda.util!
              ? ' · Te sirvió'
              : ' · No te sirvió'}',
        ),
        trailing: principal == null
            ? null
            : Insignia(
                principal.severidad.etiqueta,
                tono: _tono(principal.severidad),
              ),
        onTap: () => showModalBottomSheet<void>(
          context: context,
          isScrollControlled: true,
          showDragHandle: true,
          builder: (_) => _DetalleAyuda(ayuda: ayuda),
        ),
      ),
    );
  }
}

class _DetalleAyuda extends ConsumerStatefulWidget {
  final AyudaTablero ayuda;

  const _DetalleAyuda({required this.ayuda});

  @override
  ConsumerState<_DetalleAyuda> createState() => _DetalleAyudaState();
}

class _DetalleAyudaState extends ConsumerState<_DetalleAyuda> {
  late bool? _util = widget.ayuda.util;
  bool _enviando = false;

  Future<void> _marcar(bool util) async {
    setState(() => _enviando = true);
    try {
      await marcarAyudaUtil(ref, widget.ayuda.codigo, util: util);
      if (mounted) setState(() => _util = util);
    } catch (_) {
      if (mounted) {
        avisar(context, 'No pudimos guardar tu opinión. Inténtalo de nuevo.');
      }
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final ayuda = widget.ayuda;

    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (ayuda.imagenUrl != null) ...[
              _Miniatura(url: ayuda.imagenUrl, lado: 160),
              const SizedBox(height: 12),
            ],
            Text(
              ayuda.principal?.nombre ?? 'Luz del tablero',
              style: tema.textTheme.titleMedium,
            ),
            Text(
              '${formatearFechaCorta(ayuda.creadaEn.toLocal())} · Confianza ${((ayuda.principal?.confianza ?? 0) * 100).round()}%',
              style: tema.textTheme.bodySmall,
            ),
            const SizedBox(height: 10),
            Text(ayuda.respuesta, key: const Key('respuestaAyuda')),
            if (ayuda.indicadores.length > 1) ...[
              const SizedBox(height: 10),
              Text('Otras posibilidades', style: tema.textTheme.labelLarge),
              for (final otra in ayuda.indicadores.skip(1))
                Text('${otra.nombre} · ${(otra.confianza * 100).round()}%'),
            ],
            const SizedBox(height: 16),
            Text('¿Te sirvió esta ayuda?', style: tema.textTheme.labelLarge),
            const SizedBox(height: 6),
            Row(
              children: [
                Expanded(
                  child: ChoiceChip(
                    key: const Key('botonUtil'),
                    label: const Text('Sí, me sirvió'),
                    selected: _util == true,
                    onSelected: _enviando ? null : (_) => _marcar(true),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: ChoiceChip(
                    key: const Key('botonNoUtil'),
                    label: const Text('No me sirvió'),
                    selected: _util == false,
                    onSelected: _enviando ? null : (_) => _marcar(false),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
