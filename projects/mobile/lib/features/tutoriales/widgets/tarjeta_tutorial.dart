import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/insignia.dart';
import '../../../shared/widgets/visor_pdf_screen.dart';
import '../models/tutorial.dart';
import '../screens/reproductor_video_screen.dart';

/// Abre el recurso de un tutorial dentro de la app: el PDF en el visor y el video en el reproductor.
void abrirTutorial(BuildContext context, Tutorial tutorial) {
  if (tutorial.tipo == TipoTutorial.pdf) {
    context.push(
      '/pdf',
      extra: VisorPdfArgs(url: tutorial.recursoUrl, titulo: tutorial.titulo),
    );
  } else {
    context.push(
      '/video',
      extra: ReproductorVideoArgs(
        url: tutorial.recursoUrl,
        titulo: tutorial.titulo,
      ),
    );
  }
}

/// La tarjeta de un tutorial (CU33 y CU35): título, tipo, descripción, etiquetas y el botón para abrirlo.
///
/// * [acciones] son los botones extra de «Mis tutoriales» (editar, publicar, archivar); la biblioteca no los lleva.
/// * [mostrarEstado] pone la insignia del estado (solo para quien gestiona sus tutoriales).
class TarjetaTutorial extends StatelessWidget {
  final Tutorial tutorial;
  final List<Widget> acciones;
  final bool mostrarEstado;

  const TarjetaTutorial({
    super.key,
    required this.tutorial,
    this.acciones = const [],
    this.mostrarEstado = false,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final esPdf = tutorial.tipo == TipoTutorial.pdf;

    return Card(
      key: Key('tutorial-${tutorial.codigo}'),
      margin: const EdgeInsets.only(bottom: 10),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  esPdf
                      ? Icons.picture_as_pdf_outlined
                      : Icons.play_circle_outline,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    tutorial.titulo,
                    style: tema.textTheme.titleMedium,
                  ),
                ),
                const SizedBox(width: 8),
                if (mostrarEstado)
                  Insignia(
                    tutorial.estado.etiqueta,
                    tono: switch (tutorial.estado) {
                      EstadoTutorial.publicado => TonoInsignia.exito,
                      EstadoTutorial.borrador => TonoInsignia.advertencia,
                      EstadoTutorial.archivado => TonoInsignia.neutro,
                    },
                  )
                else
                  Insignia(
                    tutorial.categoria == 'GENERAL'
                        ? 'General'
                        : tutorial.categoria,
                    tono: TonoInsignia.info,
                  ),
              ],
            ),
            if (tutorial.descripcion != null) ...[
              const SizedBox(height: 6),
              Text(tutorial.descripcion!),
            ],
            const SizedBox(height: 6),
            Text(
              [
                tutorial.tipo.etiqueta,
                if (mostrarEstado)
                  tutorial.categoria == 'GENERAL'
                      ? 'General'
                      : 'Categoría ${tutorial.categoria}',
                'Para ${tutorial.audiencia.map((a) => a.etiqueta.toLowerCase()).join(' y ')}',
              ].join(' · '),
              style: tema.textTheme.bodySmall,
            ),
            if (tutorial.etiquetas.isNotEmpty) ...[
              const SizedBox(height: 8),
              Wrap(
                spacing: 6,
                runSpacing: 4,
                children: [for (final e in tutorial.etiquetas) Insignia(e)],
              ),
            ],
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                OutlinedButton.icon(
                  key: Key('botonAbrirTutorial-${tutorial.codigo}'),
                  // * El tema estira los botones a todo el ancho; estos van en una fila
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size(0, 40),
                  ),
                  onPressed: () => abrirTutorial(context, tutorial),
                  icon: Icon(
                    esPdf ? Icons.picture_as_pdf_outlined : Icons.play_arrow,
                  ),
                  label: Text(esPdf ? 'Abrir PDF' : 'Ver video'),
                ),
                ...acciones,
              ],
            ),
          ],
        ),
      ),
    );
  }
}
