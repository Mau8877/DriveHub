import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../../../shared/widgets/insignia.dart';
import '../models/analisis_tablero.dart';
import '../models/luz_tablero.dart';
import '../providers/tablero_providers.dart';
import '../services/clasificador_tablero.dart';
import '../services/procesar_imagen.dart';

/// El texto de aviso de un error al analizar la foto, en español y sin términos técnicos.
String mensajeDeErrorDeTablero(Object error) {
  if (error is ModeloNoDisponible) {
    return 'El análisis del tablero no está disponible en esta versión de la app.';
  }
  if (error is ImagenInvalida) {
    return 'No pudimos leer esa foto. Prueba con otra.';
  }
  return 'No pudimos analizar la foto. Inténtalo de nuevo.';
}

TonoInsignia _tono(SeveridadTablero severidad) => switch (severidad) {
  SeveridadTablero.informativa => TonoInsignia.info,
  SeveridadTablero.advertencia => TonoInsignia.advertencia,
  SeveridadTablero.critica => TonoInsignia.advertencia,
};

/// «Tablero» (CU40, dentro de la sección «Estudio»): se toma o se elige la foto de una luz del tablero y el modelo
/// del teléfono dice qué es, qué significa y qué hacer.
///
/// * El análisis corre en el teléfono, sin internet. Si hay conexión y el modelo está seguro, el resultado se guarda
///   en el historial (CU42); sin conexión se muestra igual y se avisa que no se guardó.
/// * Es orientativo: no reemplaza la revisión de un mecánico.
class TableroScreen extends ConsumerStatefulWidget {
  const TableroScreen({super.key});

  @override
  ConsumerState<TableroScreen> createState() => _TableroScreenState();
}

class _TableroScreenState extends ConsumerState<TableroScreen> {
  bool _guardarFoto = true;

  Future<void> _analizar(ImageSource origen) => ref
      .read(tableroProvider.notifier)
      .analizar(origen, guardarFoto: _guardarFoto);

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(tableroProvider);
    final ocupado = estado.isLoading;

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
      children: [
        Text(
          'Apunta a la luz encendida del tablero y te explicamos qué significa.',
          style: Theme.of(context).textTheme.bodyMedium,
        ),
        const SizedBox(height: 12),
        FilledButton.icon(
          key: const Key('botonTomarFoto'),
          onPressed: ocupado ? null : () => _analizar(ImageSource.camera),
          icon: ocupado
              ? const IndicadorBoton()
              : const Icon(Icons.photo_camera_outlined),
          label: Text(ocupado ? 'Analizando…' : 'Tomar foto'),
        ),
        const SizedBox(height: 8),
        OutlinedButton.icon(
          key: const Key('botonElegirDeGaleria'),
          onPressed: ocupado ? null : () => _analizar(ImageSource.gallery),
          icon: const Icon(Icons.photo_library_outlined),
          label: const Text('Elegir de la galería'),
        ),
        TextButton.icon(
          key: const Key('botonVerHistorial'),
          onPressed: () => context.push('/study/tablero/historial'),
          icon: const Icon(Icons.history),
          label: const Text('Ver mi historial de ayudas'),
        ),
        SwitchListTile(
          key: const Key('interruptorGuardarFoto'),
          contentPadding: EdgeInsets.zero,
          title: const Text('Guardar la foto en mi historial'),
          subtitle: const Text('Una miniatura, para verla después'),
          value: _guardarFoto,
          onChanged: ocupado ? null : (v) => setState(() => _guardarFoto = v),
        ),
        const SizedBox(height: 4),
        ...estado.when(
          loading: () => const [
            Padding(
              padding: EdgeInsets.all(32),
              child: Center(child: CircularProgressIndicator()),
            ),
          ],
          error: (error, _) => [
            AvisoCockpit(
              key: const Key('errorTablero'),
              texto: mensajeDeErrorDeTablero(error),
              tono: TonoAviso.error,
            ),
          ],
          data: (resultado) => resultado == null
              ? const [
                  EstadoVacio(
                    icono: Icons.dashboard_customize_outlined,
                    titulo: 'Analiza una luz del tablero',
                    texto:
                        'Toma una foto con buena luz, de cerca y sin reflejos.',
                  ),
                ]
              : [_Resultado(resultado: resultado)],
        ),
        const SizedBox(height: 16),
        Text(
          'Es una ayuda orientativa y no reemplaza la revisión de un mecánico.',
          key: const Key('avisoOrientativo'),
          style: Theme.of(context).textTheme.bodySmall,
        ),
      ],
    );
  }
}

class _Resultado extends ConsumerWidget {
  final ResultadoTablero resultado;

  const _Resultado({required this.resultado});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tema = Theme.of(context);
    final principal = resultado.analisis.principal;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(12),
          child: Image.memory(
            resultado.foto,
            key: const Key('fotoAnalizada'),
            height: 180,
            width: double.infinity,
            fit: BoxFit.cover,
          ),
        ),
        const SizedBox(height: 12),
        if (principal == null || !resultado.seguro)
          _NoEstoySeguro(analisis: resultado.analisis)
        else
          Card(
            key: const Key('resultadoTablero'),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          principal.luz.nombre,
                          style: tema.textTheme.titleMedium,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Insignia(
                        principal.luz.severidad.etiqueta,
                        tono: _tono(principal.luz.severidad),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Confianza ${(principal.confianza * 100).round()}%',
                    style: tema.textTheme.bodySmall,
                  ),
                  const SizedBox(height: 10),
                  Text('Qué significa', style: tema.textTheme.labelLarge),
                  Text(principal.luz.significado),
                  const SizedBox(height: 10),
                  Text('Qué hacer', style: tema.textTheme.labelLarge),
                  Text(principal.luz.queHacer),
                ],
              ),
            ),
          ),
        if (resultado.analisis.luces.length > 1) ...[
          const SizedBox(height: 12),
          Text('Otras posibilidades', style: tema.textTheme.labelLarge),
          for (final otra in resultado.analisis.luces.skip(1))
            Text('${otra.luz.nombre} · ${(otra.confianza * 100).round()}%'),
        ],
        if (resultado.seguro) ...[
          const SizedBox(height: 12),
          _EstadoGuardado(resultado: resultado),
        ],
      ],
    );
  }
}

class _NoEstoySeguro extends StatelessWidget {
  final AnalisisTablero analisis;

  const _NoEstoySeguro({required this.analisis});

  @override
  Widget build(BuildContext context) {
    return const AvisoCockpit(
      key: Key('noEstoySeguro'),
      texto:
          'No estoy seguro de qué luz es. Prueba con otra foto: más cerca, con buena luz y sin reflejos.',
      tono: TonoAviso.advertencia,
    );
  }
}

class _EstadoGuardado extends ConsumerWidget {
  final ResultadoTablero resultado;

  const _EstadoGuardado({required this.resultado});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return switch (resultado.guardado) {
      GuardadoAyuda.noAplica => const SizedBox.shrink(),
      GuardadoAyuda.guardando => const Row(
        key: Key('guardando'),
        children: [
          SizedBox(
            width: 16,
            height: 16,
            child: CircularProgressIndicator(strokeWidth: 2),
          ),
          SizedBox(width: 8),
          Text('Guardando en tu historial…'),
        ],
      ),
      GuardadoAyuda.guardado => const Row(
        key: Key('guardado'),
        children: [
          Icon(Icons.check_circle_outline, size: 18),
          SizedBox(width: 8),
          Text('Guardado en tu historial'),
        ],
      ),
      GuardadoAyuda.noGuardado => Column(
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
            onPressed: () =>
                ref.read(tableroProvider.notifier).reintentarGuardado(),
            child: const Text('Reintentar guardar'),
          ),
        ],
      ),
    };
  }
}
