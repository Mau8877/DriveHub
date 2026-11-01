import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../../../shared/widgets/insignia.dart';
import '../models/prueba.dart';
import '../providers/prueba_providers.dart';
import '../services/detector_rostro.dart';

/// El texto de aviso de un error al abrir la cámara, en español y sin términos técnicos.
String mensajeDeErrorDePrueba(Object error) {
  if (error is PermisoCamaraDenegado) {
    return 'Necesitamos permiso para usar la cámara. Actívalo en los ajustes del teléfono e inténtalo de nuevo.';
  }
  if (error is CamaraNoDisponible) {
    return 'No pudimos abrir la cámara frontal de tu teléfono.';
  }
  return 'No pudimos empezar la prueba. Inténtalo de nuevo.';
}

String _segundos(int ms) => (ms / 1000).toStringAsFixed(1);

/// «Prueba» (CU39, dentro de la sección «Estudio»): una prueba de conducción asistida. La app guía una revisión antes
/// de maniobrar (al frente, espejo izquierdo, espejo derecho y retrovisor) y, con la cámara frontal, comprueba que la
/// persona gira la cabeza hacia cada punto.
///
/// * El reconocimiento corre en el teléfono: la cámara se usa solo para saber hacia dónde mira la persona, no se
///   graba ni se envía video. Al terminar se guarda el resultado (puntaje y pasos) en el historial.
/// * Valida la atención visual, no las maniobras ni el entorno.
class PruebaScreen extends ConsumerStatefulWidget {
  const PruebaScreen({super.key});

  @override
  ConsumerState<PruebaScreen> createState() => _PruebaScreenState();
}

class _PruebaScreenState extends ConsumerState<PruebaScreen> {
  @override
  void dispose() {
    // * Se devuelve el giro libre de pantalla si la persona sale a mitad de la prueba
    SystemChrome.setPreferredOrientations(const []);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // * El detector de rostros espera el teléfono en vertical: se fija mientras dura la prueba
    ref.listen(pruebaProvider.select((e) => e.fase == FasePrueba.enCurso), (
      _,
      enCurso,
    ) {
      SystemChrome.setPreferredOrientations(
        enCurso ? const [DeviceOrientation.portraitUp] : const [],
      );
    });
    final estado = ref.watch(pruebaProvider);

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
      children: switch (estado.fase) {
        FasePrueba.inicio => _inicio(context, estado),
        FasePrueba.preparando => const [
          Padding(
            padding: EdgeInsets.all(32),
            child: Center(
              key: Key('abriendoCamara'),
              child: Column(
                children: [
                  CircularProgressIndicator(),
                  SizedBox(height: 12),
                  Text('Abriendo la cámara…'),
                ],
              ),
            ),
          ),
        ],
        FasePrueba.enCurso => _enCurso(context, estado),
        FasePrueba.terminada => _terminada(context, estado),
      },
    );
  }

  List<Widget> _inicio(BuildContext context, EstadoPrueba estado) {
    final tema = Theme.of(context);
    return [
      Text(
        'Practica la revisión que debes hacer antes de maniobrar. La app te pide mirar cada punto y comprueba con la cámara que lo haces.',
        style: tema.textTheme.bodyMedium,
      ),
      const SizedBox(height: 12),
      for (final (i, paso) in pasosDePrueba.indexed)
        ListTile(
          dense: true,
          contentPadding: EdgeInsets.zero,
          leading: CircleAvatar(radius: 14, child: Text('${i + 1}')),
          title: Text(paso.instruccion),
        ),
      const SizedBox(height: 8),
      const AvisoCockpit(
        key: Key('avisoPrivacidad'),
        texto:
            'La cámara frontal se usa solo en tu teléfono para ver hacia dónde miras. No se graba ni se envía video: solo se guarda tu resultado.',
        tono: TonoAviso.info,
      ),
      const SizedBox(height: 8),
      Text(
        'Apoya el teléfono de frente, a la altura de tu cara y con buena luz. Es una práctica orientativa: valida que mires los espejos, no tus maniobras.',
        key: const Key('consejosPrueba'),
        style: tema.textTheme.bodySmall,
      ),
      if (estado.error != null) ...[
        const SizedBox(height: 12),
        AvisoCockpit(
          key: const Key('errorPrueba'),
          texto: mensajeDeErrorDePrueba(estado.error!),
          tono: TonoAviso.error,
        ),
      ],
      const SizedBox(height: 16),
      FilledButton.icon(
        key: const Key('botonEmpezarPrueba'),
        onPressed: () => ref.read(pruebaProvider.notifier).iniciar(),
        icon: const Icon(Icons.play_arrow),
        label: const Text('Empezar la prueba'),
      ),
    ];
  }

  List<Widget> _enCurso(BuildContext context, EstadoPrueba estado) {
    final tema = Theme.of(context);
    final detector = ref.read(detectorRostroProvider);
    final paso =
        pasosDePrueba[estado.indice.clamp(0, pasosDePrueba.length - 1)];

    return [
      ClipRRect(
        borderRadius: BorderRadius.circular(12),
        child: SizedBox(
          height: 260,
          child: FittedBox(
            fit: BoxFit.cover,
            clipBehavior: Clip.hardEdge,
            child: SizedBox(
              width: 200,
              height: 260,
              child: detector.vistaPrevia(context),
            ),
          ),
        ),
      ),
      const SizedBox(height: 12),
      Text(
        'Paso ${estado.indice + 1} de ${pasosDePrueba.length}',
        key: const Key('numeroPaso'),
        style: tema.textTheme.labelLarge,
      ),
      const SizedBox(height: 4),
      Text(
        paso.instruccion,
        key: const Key('instruccionPaso'),
        style: tema.textTheme.headlineSmall,
      ),
      const SizedBox(height: 12),
      LinearProgressIndicator(
        key: const Key('progresoPaso'),
        value: estado.progreso,
        minHeight: 8,
      ),
      const SizedBox(height: 6),
      Text(
        'Tiempo: ${_segundos(estado.restanteMs)} s',
        style: tema.textTheme.bodySmall,
      ),
      if (estado.sinRostro) ...[
        const SizedBox(height: 8),
        const AvisoCockpit(
          key: Key('sinRostro'),
          texto: 'No te veo. Centra tu rostro frente a la cámara.',
          tono: TonoAviso.advertencia,
        ),
      ],
      const SizedBox(height: 16),
      OutlinedButton(
        key: const Key('botonCancelarPrueba'),
        onPressed: () => ref.read(pruebaProvider.notifier).cancelar(),
        child: const Text('Cancelar'),
      ),
    ];
  }

  List<Widget> _terminada(BuildContext context, EstadoPrueba estado) {
    final tema = Theme.of(context);
    final resultado = estado.resultado!;

    return [
      Card(
        key: const Key('resultadoPrueba'),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      '${resultado.puntaje.round()}%',
                      key: const Key('puntajePrueba'),
                      style: tema.textTheme.displaySmall,
                    ),
                  ),
                  Insignia(
                    '${resultado.aprobados} de ${resultado.pasos.length} pasos',
                    tono: resultado.aprobados == resultado.pasos.length
                        ? TonoInsignia.exito
                        : TonoInsignia.advertencia,
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                resultado.retroalimentacion,
                key: const Key('retroalimentacion'),
              ),
            ],
          ),
        ),
      ),
      const SizedBox(height: 8),
      for (final p in resultado.pasos)
        ListTile(
          key: Key('resultadoPaso-${p.orden}'),
          dense: true,
          contentPadding: EdgeInsets.zero,
          leading: Icon(
            p.aprobado ? Icons.check_circle_outline : Icons.cancel_outlined,
            color: p.aprobado ? null : tema.colorScheme.error,
          ),
          title: Text(p.paso.instruccion),
          trailing: Text('${_segundos(p.tiempoMs)} s'),
        ),
      const SizedBox(height: 8),
      _EstadoGuardado(guardado: estado.guardado),
      const SizedBox(height: 16),
      FilledButton(
        key: const Key('botonRepetirPrueba'),
        onPressed: () => ref.read(pruebaProvider.notifier).reiniciar(),
        child: const Text('Repetir la prueba'),
      ),
    ];
  }
}

class _EstadoGuardado extends ConsumerWidget {
  final GuardadoPrueba? guardado;

  const _EstadoGuardado({required this.guardado});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return switch (guardado) {
      null => const SizedBox.shrink(),
      GuardadoPrueba.guardando => const Row(
        key: Key('guardando'),
        children: [
          SizedBox(width: 16, height: 16, child: IndicadorBoton()),
          SizedBox(width: 8),
          Text('Guardando en tu historial…'),
        ],
      ),
      GuardadoPrueba.guardado => const Row(
        key: Key('guardado'),
        children: [
          Icon(Icons.check_circle_outline, size: 18),
          SizedBox(width: 8),
          Text('Guardado en tu historial'),
        ],
      ),
      GuardadoPrueba.noGuardado => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const AvisoCockpit(
            key: Key('noGuardado'),
            texto:
                'No se guardó en tu historial. El resultado de arriba sigue siendo válido.',
            tono: TonoAviso.info,
          ),
          const SizedBox(height: 8),
          OutlinedButton(
            key: const Key('botonReintentarGuardado'),
            onPressed: () => ref.read(pruebaProvider.notifier).guardar(),
            child: const Text('Reintentar guardar'),
          ),
        ],
      ),
    };
  }
}
