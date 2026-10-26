import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../../pagos/providers/saldo_horas_provider.dart';
import '../models/franja_disponible.dart';
import '../providers/franjas_seleccionadas_provider.dart';
import '../providers/reservas_provider.dart';
import 'formato_clases.dart';
import 'resultado_reserva_sheet.dart';

/// Abre el resumen de lo que el estudiante eligió, antes de gastar las horas (CU14).
Future<void> mostrarResumenReserva(BuildContext context) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    builder: (_) => const ResumenReservaSheet(),
  );
}

/// El resumen previo a confirmar: las clases elegidas con su día, hora e instructor, y cuántas horas se gastarán.
///
/// * «Confirmar reserva» reserva de verdad. Mientras la llamada está en curso el botón queda deshabilitado, así un
/// * doble toque no manda dos veces la misma petición.
/// * Si la llamada falla (sin red, un 400) el resumen se mantiene con el aviso y la selección intacta, para
/// * reintentar. Nunca se anuncian clases reservadas sin una respuesta del servidor.
class ResumenReservaSheet extends ConsumerStatefulWidget {
  const ResumenReservaSheet({super.key});

  @override
  ConsumerState<ResumenReservaSheet> createState() => _ResumenReservaSheetState();
}

class _ResumenReservaSheetState extends ConsumerState<ResumenReservaSheet> {
  bool _enviando = false;
  String? _error;

  Future<void> _confirmar() async {
    if (_enviando) return;
    // * Las franjas se guardan ANTES de confirmar: al terminar la selección se vacía y el resultado las necesita
    // * para ofrecer "Reintentar" las que fallaron por un corte de Pagos
    final elegidas = Map<String, FranjaDisponible>.of(ref.read(franjasSeleccionadasUiProvider));
    // * El contexto del navegador sobrevive a este resumen: con él se abre el resultado después de cerrarlo
    final navegador = Navigator.of(context);
    final contexto = navegador.context;
    setState(() {
      _enviando = true;
      _error = null;
    });
    try {
      final resultado = await ref.read(reservasProvider.notifier).confirmar();
      if (!mounted || !contexto.mounted) return;
      navegador.pop();
      await mostrarResultadoReserva(contexto, resultado, elegidas);
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _enviando = false;
        _error = e is Exception ? mensajeDeError(e) : 'No pudimos confirmar. Inténtalo de nuevo.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final elegidas = ref.watch(franjasSeleccionadasUiProvider).values.toList()
      ..sort((a, b) {
        final porFecha = a.fecha.compareTo(b.fecha);
        return porFecha != 0 ? porFecha : a.horaInicio.compareTo(b.horaInicio);
      });
    final horas = ref.watch(saldoHorasProvider).maybeWhen(data: (h) => h, orElse: () => null);
    final gastar = elegidas.length;

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Confirma tus clases', style: tema.textTheme.titleLarge),
            const SizedBox(height: 12),
            Flexible(
              child: ListView(
                shrinkWrap: true,
                children: [
                  for (final franja in elegidas)
                    ListTile(
                      key: Key('resumen-${franja.codigo}'),
                      contentPadding: EdgeInsets.zero,
                      leading: const Icon(Icons.event_available_outlined),
                      title: Text('${tituloDeDia(franja.fecha, DateTime.now())} · ${rangoDeHoras(franja.horaInicio, franja.horaFin)}'),
                      subtitle: Text(franja.instructorNombre),
                    ),
                ],
              ),
            ),
            const Divider(),
            Text(
              horas == null
                  ? 'Gastarás $gastar h'
                  : 'Gastarás $gastar h de tus $horas h. Te quedarán ${(horas - gastar).clamp(0, horas)} h.',
              key: const Key('resumenHoras'),
              style: tema.textTheme.titleSmall,
            ),
            if (_error != null) ...[
              const SizedBox(height: 12),
              AvisoCockpit(key: const Key('errorConfirmar'), tono: TonoAviso.error, texto: _error!),
            ],
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    key: const Key('cancelarResumen'),
                    onPressed: _enviando ? null : () => Navigator.of(context).pop(),
                    child: const Text('Cancelar'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton(
                    key: const Key('confirmarReserva'),
                    onPressed: _enviando ? null : _confirmar,
                    child: _enviando ? const IndicadorBoton() : const Text('Confirmar reserva'),
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
