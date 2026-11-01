import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../models/intento_iniciado.dart';
import '../providers/rendir_provider.dart';
import '../widgets/mensajes_de_simulacro.dart';

/// Rendir un simulacro (CU21, `/study/simulacros/rendir`): una pregunta por pantalla, cronómetro y envío.
///
/// * El tiempo se cuenta en el teléfono desde que llegaron las preguntas; al llegar a cero se envía solo lo que haya
///   marcado. El backend vuelve a controlar el tiempo.
/// * Salir con «atrás» pide confirmar: el intento NO se cancela, el tiempo sigue corriendo en el servidor.
/// * Las respuestas correctas no están en la app hasta que el intento termina.
class RendirSimulacroScreen extends ConsumerStatefulWidget {
  const RendirSimulacroScreen({super.key});

  @override
  ConsumerState<RendirSimulacroScreen> createState() =>
      _RendirSimulacroScreenState();
}

class _RendirSimulacroScreenState extends ConsumerState<RendirSimulacroScreen> {
  Timer? _reloj;
  Duration _restante = Duration.zero;
  bool _enviando = false;

  @override
  void initState() {
    super.initState();
    _tic();
    _reloj = Timer.periodic(const Duration(seconds: 1), (_) => _tic());
  }

  @override
  void dispose() {
    _reloj?.cancel();
    super.dispose();
  }

  void _tic() {
    final estado = ref.read(rendirProvider);
    if (estado == null) return;
    final restante = estado.venceEn.difference(DateTime.now());
    if (mounted) setState(() => _restante = restante);
    if (restante <= Duration.zero && !_enviando) _finalizar(porTiempo: true);
  }

  Future<void> _finalizar({bool porTiempo = false}) async {
    final estado = ref.read(rendirProvider);
    if (estado == null || _enviando) return;
    if (!porTiempo) {
      final sinResponder = estado.intento.preguntas.length - estado.respondidas;
      final confirmado = await showDialog<bool>(
        context: context,
        builder: (contexto) => AlertDialog(
          title: const Text('¿Enviar tus respuestas?'),
          content: Text(
            sinResponder == 0
                ? 'Ya no podrás cambiarlas.'
                : 'Te quedan $sinResponder preguntas sin responder. Ya no podrás cambiar nada.',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(contexto, false),
              child: const Text('Seguir'),
            ),
            FilledButton(
              key: const Key('botonConfirmarEnvio'),
              onPressed: () => Navigator.pop(contexto, true),
              child: const Text('Enviar'),
            ),
          ],
        ),
      );
      if (confirmado != true || !mounted) return;
    }
    setState(() => _enviando = true);
    try {
      final resultado = await ref.read(rendirProvider.notifier).finalizar();
      _reloj?.cancel();
      if (mounted) {
        context.pushReplacement('/study/attempts/${resultado.codigo}');
      }
    } catch (error) {
      if (mounted) {
        setState(() => _enviando = false);
        avisar(context, mensajeDeErrorDeSimulacro(error));
      }
    }
  }

  Future<void> _salir() async {
    final salir = await showDialog<bool>(
      context: context,
      builder: (contexto) => AlertDialog(
        title: const Text('¿Salir del simulacro?'),
        content: const Text(
          'Tu intento no se cancela: el tiempo sigue corriendo y no podrás empezar otro hasta que termine.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(contexto, false),
            child: const Text('Seguir'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(contexto, true),
            child: const Text('Salir'),
          ),
        ],
      ),
    );
    if (salir == true && mounted) {
      ref.read(rendirProvider.notifier).descartar();
      context.pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(rendirProvider);
    if (estado == null) {
      // * Sin intento en la app (se envió o se descartó): no hay nada que rendir
      return const CockpitScaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }
    final pregunta = estado.actual;
    final marcadas = estado.elegidas[pregunta.codigo] ?? const <String>{};
    final poco = _restante < const Duration(minutes: 1);

    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (salio, _) {
        if (!salio) _salir();
      },
      child: CockpitScaffold(
        appBar: AppBar(
          automaticallyImplyLeading: false,
          title: Text(estado.intento.simulacroNombre),
          actions: [
            Padding(
              padding: const EdgeInsets.only(right: 16),
              child: Center(
                child: Chip(
                  key: const Key('cronometro'),
                  avatar: Icon(
                    Icons.timer_outlined,
                    size: 18,
                    color: poco ? Theme.of(context).colorScheme.error : null,
                  ),
                  label: Text(formatearTiempo(_restante)),
                ),
              ),
            ),
          ],
        ),
        body: ListView(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
          children: [
            LinearProgressIndicator(
              value: (estado.indice + 1) / estado.intento.preguntas.length,
            ),
            const SizedBox(height: 8),
            Text(
              'Pregunta ${estado.indice + 1} de ${estado.intento.preguntas.length}',
              style: Theme.of(context).textTheme.bodySmall,
            ),
            const SizedBox(height: 12),
            Text(
              pregunta.enunciado,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            if (pregunta.tipo.admiteVarias) ...[
              const SizedBox(height: 4),
              Text(
                'Marca todas las correctas.',
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
            const SizedBox(height: 12),
            for (final opcion in pregunta.opciones)
              _OpcionTile(
                opcion: opcion,
                marcada: marcadas.contains(opcion.id),
                variasRespuestas: pregunta.tipo.admiteVarias,
                onTap: () => ref
                    .read(rendirProvider.notifier)
                    .elegir(pregunta.codigo, opcion.id),
              ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    key: const Key('botonAnterior'),
                    onPressed: estado.indice == 0 || _enviando
                        ? null
                        : () => ref
                              .read(rendirProvider.notifier)
                              .irA(estado.indice - 1),
                    child: const Text('Anterior'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: estado.esUltima
                      ? FilledButton(
                          key: const Key('botonFinalizar'),
                          onPressed: _enviando ? null : () => _finalizar(),
                          child: _enviando
                              ? const IndicadorBoton()
                              : const Text('Finalizar'),
                        )
                      : FilledButton(
                          key: const Key('botonSiguiente'),
                          onPressed: () => ref
                              .read(rendirProvider.notifier)
                              .irA(estado.indice + 1),
                          child: const Text('Siguiente'),
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

class _OpcionTile extends StatelessWidget {
  final OpcionParaRendir opcion;
  final bool marcada;
  final bool variasRespuestas;
  final VoidCallback onTap;

  const _OpcionTile({
    required this.opcion,
    required this.marcada,
    required this.variasRespuestas,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final icono = variasRespuestas
        ? (marcada ? Icons.check_box : Icons.check_box_outline_blank)
        : (marcada ? Icons.radio_button_checked : Icons.radio_button_unchecked);
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        key: Key('opcion-${opcion.id}'),
        onTap: onTap,
        selected: marcada,
        leading: Icon(icono),
        title: Text('${opcion.id}. ${opcion.texto}'),
      ),
    );
  }
}
