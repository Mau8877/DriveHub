import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../providers/offline_provider.dart';

/// Aviso fijo sobre el contenido de los shells con el estado del modo offline. Un solo aviso a la vez, el más
/// importante: resultado de lo hecho sin conexión > enviando > pendientes > sin conexión. Sin nada que decir no
/// ocupa espacio.
///
/// * Sin conexión: advierte que se ven datos guardados y que lo que se haga se enviará al volver internet, con un
/// * aviso de cuáles operaciones se cumplieron y cuáles no.
/// * Al volver internet: cuenta cuántas operaciones se cumplieron y cuántas no ([_HojaResultados] las detalla).
class AvisoOffline extends ConsumerWidget {
  const AvisoOffline({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(offlineProvider);
    if (!estado.hayAviso) return const SizedBox.shrink();

    final AvisoCockpit aviso;
    if (estado.hayResultados && !estado.sincronizando && estado.pendientes.isEmpty) {
      final ok = estado.completadas.length;
      final no = estado.rechazadas.length;
      aviso = AvisoCockpit(
        texto: _resumen(ok, no),
        tono: no > 0 ? TonoAviso.error : TonoAviso.exito,
        accion: TextButton(
          onPressed: () => _verResultados(context),
          child: const Text('Ver'),
        ),
      );
    } else if (estado.pendientes.isNotEmpty) {
      final n = estado.pendientes.length;
      final sujeto = n == 1 ? '1 operación pendiente' : '$n operaciones pendientes';
      aviso = AvisoCockpit(
        texto: estado.sincronizando
            ? 'Volvió la conexión: enviando $sujeto…'
            : estado.sinConexion
            ? 'Sin conexión: $sujeto. Se enviará al volver internet y te avisaremos cuáles se cumplieron y cuáles no.'
            : '$sujeto de enviar.',
        tono: estado.sinConexion ? TonoAviso.advertencia : TonoAviso.info,
      );
    } else {
      aviso = const AvisoCockpit(
        texto:
            'Sin conexión. Se muestran los datos guardados en tu teléfono. Lo que hagas se guardará y, al volver '
            'internet, te avisaremos cuáles operaciones se cumplieron y cuáles no.',
        tono: TonoAviso.advertencia,
      );
    }

    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 0, 20, 8),
      child: aviso,
    );
  }

  static String _resumen(int ok, int no) {
    final cumplidas = ok == 1 ? '1 operación se cumplió' : '$ok operaciones se cumplieron';
    final rechazadas = no == 1 ? '1 no fue aceptada' : '$no no fueron aceptadas';
    if (no == 0) return 'Volvió la conexión: $cumplidas.';
    if (ok == 0) return 'Volvió la conexión: $rechazadas por el servidor.';
    return 'Volvió la conexión: $cumplidas y $rechazadas.';
  }

  void _verResultados(BuildContext context) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (_) => const _HojaResultados(),
    );
  }
}

class _HojaResultados extends ConsumerWidget {
  const _HojaResultados();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(offlineProvider);
    final tema = Theme.of(context);

    // * Al cerrar los resultados (o si ya no hay), la hoja se va sola
    if (!estado.hayResultados) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (context.mounted) Navigator.of(context).maybePop();
      });
    }

    return SafeArea(
      child: ListView(
        shrinkWrap: true,
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
        children: [
          Text('Lo que hiciste sin conexión', style: tema.textTheme.titleMedium),
          const SizedBox(height: 4),
          Text(
            'Al volver internet, el servidor revisó cada operación. Estas son las que se cumplieron y las que no.',
            style: tema.textTheme.bodyMedium,
          ),
          if (estado.completadas.isNotEmpty) ...[
            const SizedBox(height: 16),
            Text('Se cumplieron', style: tema.textTheme.titleSmall),
            const SizedBox(height: 4),
            for (final c in estado.completadas) _Fila(descripcion: c.descripcion, ok: true),
          ],
          if (estado.rechazadas.isNotEmpty) ...[
            const SizedBox(height: 16),
            Text('No se cumplieron', style: tema.textTheme.titleSmall),
            const SizedBox(height: 4),
            Text(
              'Ya no están en la cola: si todavía las necesitas, hazlas de nuevo.',
              style: tema.textTheme.bodySmall,
            ),
            const SizedBox(height: 4),
            for (final r in estado.rechazadas) _Fila(descripcion: r.descripcion, ok: false, motivo: r.motivo),
          ],
          const SizedBox(height: 16),
          FilledButton(
            key: const Key('entendidoOffline'),
            onPressed: () => ref.read(offlineProvider.notifier).descartarResultados(),
            child: const Text('Entendido'),
          ),
        ],
      ),
    );
  }
}

class _Fila extends StatelessWidget {
  final String descripcion;
  final bool ok;
  final String? motivo;

  const _Fila({required this.descripcion, required this.ok, this.motivo});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Card(
      child: ListTile(
        leading: Icon(
          ok ? Icons.check_circle_outline : Icons.error_outline,
          color: ok ? tema.colorScheme.primary : tema.colorScheme.error,
        ),
        title: Text(descripcion),
        subtitle: motivo == null ? null : Text(motivo!),
      ),
    );
  }
}
