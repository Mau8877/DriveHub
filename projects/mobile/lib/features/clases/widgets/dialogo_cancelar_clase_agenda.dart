import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../models/clase_agenda.dart';
import '../providers/gestion_agenda_provider.dart';
import 'formato_clases.dart';
import 'mensajes_de_cancelacion_instructor.dart';

/// Abre la confirmación de cancelar una clase como instructor (CU16).
Future<void> mostrarDialogoCancelarClase(BuildContext context, ClaseAgenda clase) {
  return showDialog<void>(
    context: context,
    // * Mientras la llamada está en curso no se cierra tocando afuera
    barrierDismissible: false,
    builder: (_) => DialogoCancelarClaseAgenda(clase: clase),
  );
}

/// «¿Cancelar esta clase?»: muestra la clase y le dice al instructor ANTES de confirmar que el estudiante recupera
/// su hora y que la franja queda bloqueada.
///
/// * Mientras la llamada está en curso los dos botones quedan deshabilitados: un doble toque no cancela dos veces.
/// * Sin red el diálogo se queda abierto con el aviso y el botón pasa a «Reintentar»; nunca se da por cancelada una
///   clase sin una respuesta del servidor.
/// * Si el servidor dice que la clase ya no existe o ya no se puede cancelar, se cierra con su mensaje (la agenda
///   ya se refrescó). Cualquier otro error de negocio se muestra en el diálogo.
class DialogoCancelarClaseAgenda extends ConsumerStatefulWidget {
  final ClaseAgenda clase;

  const DialogoCancelarClaseAgenda({super.key, required this.clase});

  @override
  ConsumerState<DialogoCancelarClaseAgenda> createState() => _DialogoCancelarClaseAgendaState();
}

class _DialogoCancelarClaseAgendaState extends ConsumerState<DialogoCancelarClaseAgenda> {
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
      final resultado = await ref.read(gestionAgendaProvider.notifier).cancelar(widget.clase.codigo);
      if (!mounted || !contexto.mounted) return;
      navegador.pop();
      avisar(contexto, avisoDeResultadoDeCancelacionInstructor(resultado.horaDevuelta));
    } catch (e) {
      if (!mounted) return;
      if (e is ApiException && codigosQueCierranLaCancelacionDelInstructor.contains(e.code)) {
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
    final clase = widget.clase;

    return AlertDialog(
      title: const Text('¿Cancelar esta clase?'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '${tituloDeDia(clase.fecha, DateTime.now())} · ${rangoDeHoras(clase.horaInicio, clase.horaFin)}',
              key: const Key('claseACancelar'),
              style: tema.textTheme.titleMedium,
            ),
            const SizedBox(height: 2),
            Text(
              clase.estudianteNombre,
              style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: 14),
            const AvisoCockpit(
              key: Key('avisoPrevio'),
              tono: TonoAviso.info,
              texto: avisoPrevioDeCancelacionInstructor,
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
