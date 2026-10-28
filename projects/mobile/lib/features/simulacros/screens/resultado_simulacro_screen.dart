import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/insignia.dart';
import '../models/intento_resultado.dart';
import '../providers/simulacros_list_provider.dart';
import '../widgets/mensajes_de_simulacro.dart';

/// El resultado y la corrección de un intento (CU21, `/study/attempts/:codigo`): el puntaje, si aprobó y cada
/// pregunta con lo que marcó, la respuesta correcta y la explicación.
class ResultadoSimulacroScreen extends ConsumerWidget {
  final String codigo;

  const ResultadoSimulacroScreen({super.key, required this.codigo});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final intento = ref.watch(intentoCorregidoProvider(codigo));
    return CockpitScaffold(
      appBar: AppBar(
        title: const Text('Resultado'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () =>
              context.canPop() ? context.pop() : context.go('/inicio'),
        ),
      ),
      body: intento.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            children: [
              AvisoCockpit(
                texto: mensajeDeErrorDeSimulacro(error),
                tono: TonoAviso.error,
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                onPressed: () =>
                    ref.invalidate(intentoCorregidoProvider(codigo)),
                child: const Text('Reintentar'),
              ),
            ],
          ),
        ),
        data: (resultado) => _Contenido(resultado: resultado),
      ),
    );
  }
}

class _Contenido extends StatelessWidget {
  final IntentoResultado resultado;

  const _Contenido({required this.resultado});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              children: [
                Text(
                  resultado.simulacroNombre,
                  style: tema.textTheme.titleMedium,
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 8),
                Text(
                  '${formatearPuntaje(resultado.puntaje)}%',
                  key: const Key('puntajeFinal'),
                  style: tema.textTheme.displaySmall,
                ),
                const SizedBox(height: 4),
                Text('${resultado.aciertos} de ${resultado.total} correctas'),
                const SizedBox(height: 8),
                if (resultado.expirado)
                  const Insignia(
                    'Se acabó el tiempo',
                    tono: TonoInsignia.advertencia,
                  )
                else if (resultado.aprobado)
                  const Insignia('Aprobado', tono: TonoInsignia.exito)
                else
                  Insignia(
                    'No aprobado · necesitas ${formatearPuntaje(resultado.puntajeAprobacion)}%',
                    tono: TonoInsignia.neutro,
                  ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        Text('Corrección', style: tema.textTheme.titleLarge),
        const SizedBox(height: 8),
        for (var i = 0; i < resultado.preguntas.length; i++)
          _TarjetaCorreccion(numero: i + 1, correccion: resultado.preguntas[i]),
      ],
    );
  }
}

class _TarjetaCorreccion extends StatelessWidget {
  final int numero;
  final Correccion correccion;

  const _TarjetaCorreccion({required this.numero, required this.correccion});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final sinResponder = correccion.elegidas.isEmpty;
    return Card(
      key: Key('correccion-$numero'),
      margin: const EdgeInsets.only(bottom: 10),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  correccion.correcta ? Icons.check_circle : Icons.cancel,
                  color: correccion.correcta
                      ? tema.colorScheme.primary
                      : tema.colorScheme.error,
                  size: 20,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    '$numero. ${correccion.enunciado}',
                    style: tema.textTheme.titleSmall,
                  ),
                ),
              ],
            ),
            if (sinResponder) ...[
              const SizedBox(height: 4),
              Text('Sin responder', style: tema.textTheme.bodySmall),
            ],
            const SizedBox(height: 8),
            for (final opcion in correccion.opciones)
              _FilaOpcion(
                opcion: opcion,
                elegida: correccion.elegidas.contains(opcion.id),
              ),
            if (correccion.explicacion != null) ...[
              const SizedBox(height: 8),
              Text(correccion.explicacion!, style: tema.textTheme.bodySmall),
            ],
          ],
        ),
      ),
    );
  }
}

class _FilaOpcion extends StatelessWidget {
  final OpcionCorregida opcion;
  final bool elegida;

  const _FilaOpcion({required this.opcion, required this.elegida});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    // * Verde (la correcta), rojo (la que marcó y está mal) o neutra. El texto aclara el estado: el color no es lo único
    final (icono, color, nota) = opcion.correcta
        ? (
            Icons.check,
            tema.colorScheme.primary,
            elegida ? ' (tu respuesta)' : ' (correcta)',
          )
        : elegida
        ? (Icons.close, tema.colorScheme.error, ' (tu respuesta)')
        : (null, null, '');
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 22,
            child: icono == null ? null : Icon(icono, size: 18, color: color),
          ),
          const SizedBox(width: 4),
          Expanded(
            child: Text(
              '${opcion.id}. ${opcion.texto}$nota',
              style: TextStyle(color: color),
            ),
          ),
        ],
      ),
    );
  }
}
