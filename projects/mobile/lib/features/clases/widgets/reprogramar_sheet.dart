import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/offline/modelos_offline.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../../offline/mensajes_offline.dart';
import '../models/franja_disponible.dart';
import '../models/reserva.dart';
import '../providers/franja_para_reprogramar_provider.dart';
import '../providers/franjas_disponibles_provider.dart';
import '../providers/gestion_reserva_provider.dart';
import 'filtros_reservar.dart';
import 'formato_clases.dart';
import 'franja_card.dart';
import 'mensajes_de_cancelacion.dart';

/// Abre la hoja para mover una clase a otra franja libre (CU15).
///
/// * Empieza y termina sin ninguna franja elegida, también si se descarta tocando afuera o deslizando.
Future<void> mostrarReprogramar(BuildContext context, Reserva reserva) {
  final container = ProviderScope.containerOf(context);
  container.read(franjaParaReprogramarUiProvider.notifier).vaciar();
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    builder: (_) => ReprogramarSheet(reserva: reserva),
  ).whenComplete(() => container.read(franjaParaReprogramarUiProvider.notifier).vaciar());
}

/// «Reprogramar»: las franjas libres con los MISMOS filtros que la pestaña «Reservar» (día, momento, instructor),
/// para elegir UNA y mover la clase a ella. Las horas no se tocan.
///
/// * Mientras la llamada está en curso todo queda deshabilitado: un doble toque no manda dos peticiones.
/// * `ERR_ACA_18` (otro estudiante tomó la franja): la hoja sigue abierta, se vacía la elección y las franjas se
///   refrescan para elegir otra. Un error de red conserva la elección para reintentar.
/// * Un error que deja a la clase sin sentido (no existe, no está confirmada, fuera de plazo) cierra la hoja con el
///   mensaje del servidor.
class ReprogramarSheet extends ConsumerStatefulWidget {
  final Reserva reserva;

  const ReprogramarSheet({super.key, required this.reserva});

  @override
  ConsumerState<ReprogramarSheet> createState() => _ReprogramarSheetState();
}

class _ReprogramarSheetState extends ConsumerState<ReprogramarSheet> {
  bool _enviando = false;
  String? _error;

  Future<void> _reprogramar(FranjaDisponible franja) async {
    if (_enviando) return;
    // * El contexto del navegador sobrevive a esta hoja: con él se avisa después de cerrarla
    final navegador = Navigator.of(context);
    final contexto = navegador.context;
    final seleccion = ref.read(franjaParaReprogramarUiProvider.notifier);
    setState(() {
      _enviando = true;
      _error = null;
    });
    try {
      final nueva = await ref.read(gestionReservaProvider.notifier).reprogramar(
        widget.reserva.codigo,
        franja.codigo,
        descripcion:
            'Reprogramación de la clase del ${tituloDeDia(widget.reserva.fecha, DateTime.now()).toLowerCase()} '
            'al ${tituloDeDia(franja.fecha, DateTime.now()).toLowerCase()} (${franja.horaInicio})',
      );
      seleccion.vaciar();
      if (!mounted || !contexto.mounted) return;
      navegador.pop();
      avisar(
        contexto,
        'Clase reprogramada para ${tituloDeDia(nueva.fecha, DateTime.now()).toLowerCase()} a las ${nueva.horaInicio}.',
      );
    } catch (e) {
      if (!mounted) return;
      if (e is AccionEncolada) {
        seleccion.vaciar();
        navegador.pop();
        if (contexto.mounted) avisar(contexto, avisoDeAccionEncolada(e));
        return;
      }
      if (e is ApiException && codigosQueCierranLaAccion.contains(e.code)) {
        navegador.pop();
        if (contexto.mounted) avisar(contexto, e.message);
        return;
      }
      if (e is ApiException && e.code == 'ERR_ACA_18') seleccion.vaciar();
      setState(() {
        _enviando = false;
        _error = e is ApiException && e.code == 'ERR_ACA_18'
            ? 'Otro estudiante ya reservó esa franja. Elige otra.'
            : mensajeDeError(e);
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final reserva = widget.reserva;
    final franjas = ref.watch(franjasDisponiblesListProvider);
    final elegida = ref.watch(franjaParaReprogramarUiProvider);

    return FractionallySizedBox(
      heightFactor: 0.92,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Reprogramar clase', style: tema.textTheme.titleLarge),
            const SizedBox(height: 2),
            Text(
              'Ahora: ${tituloDeDia(reserva.fecha, DateTime.now())}, ${rangoDeHoras(reserva.horaInicio, reserva.horaFin)} '
              'con ${reserva.instructorNombre}',
              key: const Key('claseActual'),
              style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: 12),
            Expanded(
              child: ListView(
                children: [
                  const FiltrosReservar(),
                  const SizedBox(height: 16),
                  franjas.when(
                    loading: () => const Padding(
                      padding: EdgeInsets.all(32),
                      child: Center(child: CircularProgressIndicator()),
                    ),
                    error: (error, _) => AvisoCockpit(
                      key: const Key('errorFranjasReprogramar'),
                      tono: TonoAviso.error,
                      texto: mensajeDeError(error),
                      accion: TextButton(
                        onPressed: () => ref.invalidate(franjasDisponiblesListProvider),
                        child: const Text('Reintentar'),
                      ),
                    ),
                    data: (lista) => lista.isEmpty ? const _SinFranjas() : _Franjas(lista, elegida),
                  ),
                  const SizedBox(height: 12),
                ],
              ),
            ),
            if (_error != null) ...[
              AvisoCockpit(key: const Key('errorReprogramar'), tono: TonoAviso.error, texto: _error!),
              const SizedBox(height: 8),
            ],
            SafeArea(
              top: false,
              child: Padding(
                padding: const EdgeInsets.only(bottom: 12, top: 4),
                child: Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        key: const Key('volverDeReprogramar'),
                        onPressed: _enviando ? null : () => Navigator.of(context).pop(),
                        child: const Text('Volver'),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: FilledButton(
                        key: const Key('confirmarReprogramacion'),
                        onPressed: _enviando || elegida == null ? null : () => _reprogramar(elegida),
                        child: _enviando ? const IndicadorBoton() : const Text('Reprogramar'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SinFranjas extends StatelessWidget {
  const _SinFranjas();

  @override
  Widget build(BuildContext context) {
    return const Card(
      child: EstadoVacio(
        icono: Icons.event_busy_outlined,
        titulo: 'No hay horarios libres con estos filtros',
        texto: 'Prueba con otro día, otro momento del día u otro instructor.',
      ),
    );
  }
}

class _Franjas extends ConsumerWidget {
  final List<FranjaDisponible> franjas;
  final FranjaDisponible? elegida;

  const _Franjas(this.franjas, this.elegida);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tema = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: Text(
            tituloDeDia(franjas.first.fecha, DateTime.now()),
            style: tema.textTheme.titleSmall?.copyWith(color: tema.colorScheme.onSurfaceVariant),
          ),
        ),
        for (final franja in franjas) ...[
          FranjaCard(
            key: Key('franjaDestino-${franja.codigo}'),
            franja: franja,
            elegida: elegida?.codigo == franja.codigo,
            deshabilitada: false,
            alTocar: () => ref.read(franjaParaReprogramarUiProvider.notifier).elegir(franja),
          ),
          const SizedBox(height: 10),
        ],
      ],
    );
  }
}
