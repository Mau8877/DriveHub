import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../clases/models/reserva.dart';
import '../../clases/widgets/dialogo_cancelar_reserva.dart';
import '../../clases/widgets/formato_clases.dart';
import '../../clases/widgets/reprogramar_sheet.dart';
import '../models/estado_asistente.dart';
import '../models/preparacion_de_cambio.dart';
import '../providers/asistente_provider.dart';
import '../services/modelos_de_ia.dart';

/// Lo que dice y pide el asistente de voz, en la sección «Clases» (CU50). No se dibuja mientras está inactivo.
///
/// * Cancelar y reprogramar NO se confirman acá: el botón «Continuar» abre el diálogo de cancelar o la hoja de
///   reprogramar que ya existen en `clases` (CU15), y ahí la persona confirma. Este panel no cambia ninguna clase.
/// * Todo el texto sale del estado: el panel no decide nada.
class PanelAsistente extends ConsumerWidget {
  const PanelAsistente({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(asistenteProvider);
    if (estado is Inactivo) return const SizedBox.shrink();

    return Padding(
      key: const Key('panelAsistente'),
      padding: const EdgeInsets.fromLTRB(20, 0, 20, 12),
      child: Card(
        margin: EdgeInsets.zero,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [_Contenido(estado: estado)],
          ),
        ),
      ),
    );
  }
}

class _Contenido extends ConsumerWidget {
  final EstadoAsistente estado;

  const _Contenido({required this.estado});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tema = Theme.of(context);
    final cerrar = ref.read(asistenteProvider.notifier).cerrar;

    Widget titulo(String texto) => Text(texto, style: tema.textTheme.titleMedium);
    Widget oido(String? texto) => texto == null || texto.isEmpty
        ? const SizedBox.shrink()
        : Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Text('«$texto»', style: tema.textTheme.bodyMedium?.copyWith(fontStyle: FontStyle.italic)),
          );
    Widget cerrarBoton([String texto = 'Cerrar']) => Align(
      alignment: Alignment.centerRight,
      child: TextButton(key: const Key('cerrarAsistente'), onPressed: cerrar, child: Text(texto)),
    );

    return switch (estado) {
      Inactivo() => const SizedBox.shrink(),
      Escuchando() => Row(
        children: [
          Icon(Icons.mic, color: tema.colorScheme.error),
          const SizedBox(width: 12),
          Expanded(child: titulo('Escuchando… suelta cuando termines')),
        ],
      ),
      Entendiendo(:final texto) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2.5)),
              const SizedBox(width: 12),
              Expanded(child: titulo(texto == null ? 'Escuchando lo que dijiste…' : 'Entendiendo…')),
            ],
          ),
          if (texto != null) ...[const SizedBox(height: 8), oido(texto)],
        ],
      ),
      Resultado(:final texto, :final mensaje, :final exito) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          oido(texto),
          AvisoCockpit(texto: mensaje, tono: exito ? TonoAviso.exito : TonoAviso.advertencia),
          cerrarBoton(),
        ],
      ),
      NoEntendi(:final texto, :final mensaje) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [oido(texto), AvisoCockpit(texto: mensaje, tono: TonoAviso.advertencia), cerrarBoton()],
      ),
      SinPermiso() => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const AvisoCockpit(
            texto: 'Para hablarle al asistente necesito permiso para usar el micrófono. Puedes dárselo en los ajustes de la app.',
            tono: TonoAviso.advertencia,
          ),
          cerrarBoton(),
        ],
      ),
      FaltanModelos() => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AvisoCockpit(
            texto: 'Para hablarle al asistente hay que descargarlo una vez en tu teléfono (unos $megasDeModelosIa MB). '
                'Conviene hacerlo con wifi. Después funciona sin internet.',
            tono: TonoAviso.info,
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              TextButton(key: const Key('cerrarAsistente'), onPressed: cerrar, child: const Text('Ahora no')),
              const SizedBox(width: 8),
              FilledButton(
                key: const Key('descargarModelos'),
                // * El tema pide botones de ancho completo: en una fila hay que acotarlo (como en los diálogos)
                style: FilledButton.styleFrom(minimumSize: const Size(0, 44)),
                onPressed: ref.read(asistenteProvider.notifier).descargarModelos,
                child: const Text('Descargar'),
              ),
            ],
          ),
        ],
      ),
      DescargandoModelos(:final progreso) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          titulo('Descargando el asistente… ${(progreso * 100).floor()} %'),
          const SizedBox(height: 12),
          LinearProgressIndicator(key: const Key('progresoModelos'), value: progreso),
          const SizedBox(height: 4),
          cerrarBoton('Seguir en segundo plano'),
        ],
      ),
      Fallo(:final mensaje) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [AvisoCockpit(texto: mensaje, tono: TonoAviso.error), cerrarBoton()],
      ),
      PideCambio(:final texto, :final propuesta) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          oido(texto),
          titulo(propuesta.resumen),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              TextButton(key: const Key('descartarCambio'), onPressed: cerrar, child: const Text('No')),
              const SizedBox(width: 8),
              FilledButton(
                key: const Key('continuarCambio'),
                // * El tema pide botones de ancho completo: en una fila hay que acotarlo (como en los diálogos)
                style: FilledButton.styleFrom(minimumSize: const Size(0, 44)),
                onPressed: () => _continuar(context, ref, propuesta),
                child: Text(propuesta.tipo == TipoCambio.cancelar ? 'Continuar' : 'Elegir nuevo horario'),
              ),
            ],
          ),
        ],
      ),
      Elegir(:final texto, :final tipo, :final candidatas) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          oido(texto),
          titulo(
            tipo == TipoCambio.cancelar ? '¿Cuál clase quieres cancelar?' : '¿Cuál clase quieres reprogramar?',
          ),
          const SizedBox(height: 8),
          for (final r in candidatas)
            ListTile(
              key: Key('candidata-${r.codigo}'),
              contentPadding: EdgeInsets.zero,
              title: Text(_describir(r)),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => ref.read(asistenteProvider.notifier).elegir(r),
            ),
          cerrarBoton('Ninguna'),
        ],
      ),
    };
  }

  /// Abre la confirmación que ya existe en `clases` y, cuando se cierra, deja el asistente inactivo.
  Future<void> _continuar(BuildContext context, WidgetRef ref, PropuestaDeCambio propuesta) async {
    final cerrar = ref.read(asistenteProvider.notifier).cerrar;
    switch (propuesta.tipo) {
      case TipoCambio.cancelar:
        await mostrarDialogoCancelar(context, propuesta.reserva);
      case TipoCambio.reprogramar:
        await mostrarReprogramar(context, propuesta.reserva);
    }
    await cerrar();
  }

  String _describir(Reserva r) =>
      '${tituloDeDia(r.fecha, DateTime.now())} · ${rangoDeHoras(r.horaInicio, r.horaFin)} · ${r.instructorNombre}';
}
