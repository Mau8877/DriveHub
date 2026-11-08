import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/offline/modelos_offline.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../../offline/mensajes_offline.dart';
import '../models/reserva.dart';
import '../providers/gestion_reserva_provider.dart';
import 'formato_clases.dart';
import 'mensajes_de_cancelacion.dart';

/// Abre la confirmación de cancelar una clase (CU15).
Future<void> mostrarDialogoCancelar(BuildContext context, Reserva reserva) {
  return showDialog<void>(
    context: context,
    // * Mientras la llamada está en curso no se cierra tocando afuera
    barrierDismissible: false,
    builder: (_) => DialogoCancelarReserva(reserva: reserva),
  );
}

/// «¿Cancelar esta clase?»: muestra la clase y le dice al estudiante ANTES de confirmar si recupera la hora o la
/// pierde (decisión del servidor, vía [Reserva.cancelacionATiempo]).
///
/// * Mientras la llamada está en curso los dos botones quedan deshabilitados: un doble toque no cancela dos veces.
/// * Sin red el diálogo se queda abierto con el aviso y el botón pasa a «Reintentar»; nunca se da por cancelada una
///   clase sin una respuesta del servidor.
/// * Si el servidor dice que la clase ya no existe, no está confirmada, ya empezó o se pasó el plazo, se cierra
///   con el mensaje del servidor (las listas ya se refrescaron).
class DialogoCancelarReserva extends ConsumerStatefulWidget {
  final Reserva reserva;

  const DialogoCancelarReserva({super.key, required this.reserva});

  @override
  ConsumerState<DialogoCancelarReserva> createState() => _DialogoCancelarReservaState();
}

class _DialogoCancelarReservaState extends ConsumerState<DialogoCancelarReserva> {
  bool _enviando = false;
  String? _error;
  bool _esDeRed = false;

  Future<void> _cancelar() async {
    if (_enviando) return;
    // * El contexto del navegador sobrevive a este diálogo: con él se avisa después de cerrarlo
    final navegador = Navigator.of(context);
    final contexto = navegador.context;
    setState(() {
      _enviando = true;
      _error = null;
    });
    try {
      final resultado = await ref.read(gestionReservaProvider.notifier).cancelar(
        widget.reserva.codigo,
        descripcion:
            'Cancelación de la clase del ${tituloDeDia(widget.reserva.fecha, DateTime.now()).toLowerCase()} '
            '(${widget.reserva.horaInicio})',
      );
      if (!mounted || !contexto.mounted) return;
      navegador.pop();
      avisar(contexto, avisoDeResultadoDeCancelacion(resultado.horaDevuelta));
    } catch (e) {
      if (!mounted) return;
      if (e is AccionEncolada) {
        navegador.pop();
        if (contexto.mounted) avisar(contexto, avisoDeCancelacionEncolada(e));
        return;
      }
      if (e is ApiException && codigosQueCierranLaAccion.contains(e.code)) {
        navegador.pop();
        if (contexto.mounted) avisar(contexto, e.message);
        return;
      }
      setState(() {
        _enviando = false;
        _esDeRed = e is! ApiException;
        _error = e is Exception ? mensajeDeError(e) : 'No pudimos cancelar. Inténtalo de nuevo.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final reserva = widget.reserva;

    return AlertDialog(
      title: const Text('¿Cancelar esta clase?'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '${tituloDeDia(reserva.fecha, DateTime.now())} · ${rangoDeHoras(reserva.horaInicio, reserva.horaFin)}',
              key: const Key('claseACancelar'),
              style: tema.textTheme.titleMedium,
            ),
            const SizedBox(height: 2),
            Text(
              reserva.instructorNombre,
              style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: 14),
            AvisoCockpit(
              key: const Key('avisoPrevio'),
              tono: reserva.cancelacionATiempo ? TonoAviso.info : TonoAviso.advertencia,
              texto: avisoPrevioDeCancelacion(aTiempo: reserva.cancelacionATiempo),
            ),
            if (_error != null) ...[
              const SizedBox(height: 12),
              AvisoCockpit(key: const Key('errorCancelar'), tono: TonoAviso.error, texto: _error!),
            ],
          ],
        ),
      ),
      actions: [
        TextButton(
          key: const Key('volverDeCancelar'),
          onPressed: _enviando ? null : () => Navigator.of(context).pop(),
          child: const Text('Volver'),
        ),
        FilledButton(
          key: const Key('confirmarCancelacion'),
          onPressed: _enviando ? null : _cancelar,
          style: FilledButton.styleFrom(minimumSize: const Size(0, 44)),
          child: _enviando ? const IndicadorBoton() : Text(_esDeRed ? 'Reintentar' : 'Cancelar clase'),
        ),
      ],
    );
  }
}
