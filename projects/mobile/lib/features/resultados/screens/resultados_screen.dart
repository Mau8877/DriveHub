import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../../../shared/widgets/insignia.dart';
import '../../pagos/widgets/formato_fecha.dart';
import '../models/resultado.dart';
import '../providers/resultados_providers.dart';

/// «Mis notas» (CU23, dentro de la sección «Estudio»): el historial de notas del estudiante, de la más reciente a la
/// más antigua, con un filtro por tipo de evaluación.
///
/// * La app solo muestra lo que informa el backend: no calcula puntajes ni si aprobó.
/// * Deslizar hacia abajo lo vuelve a pedir.
/// * Con [deAlumnos] es la vista del instructor: las notas de sus alumnos asignados, con el nombre de cada uno.
class ResultadosScreen extends ConsumerWidget {
  /// ¿Son las notas de los alumnos del instructor (y no las propias)?
  final bool deAlumnos;

  const ResultadosScreen({super.key, this.deAlumnos = false});

  /// Los chips del filtro: el texto y el tipo que piden (`null` = todas).
  static const _filtros = <(String, OrigenResultado?)>[
    ('Todas', null),
    ('Simulacros', OrigenResultado.simulacro),
    ('Pruebas', OrigenResultado.pruebaClase),
    ('Exámenes', OrigenResultado.examenConduccion),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final lista = deAlumnos ? resultadosAlumnosListProvider : resultadosListProvider;
    final notas = ref.watch(lista);
    final filtro = ref.watch(resultadosFiltroUiProvider);

    return RefreshIndicator(
      onRefresh: () async {
        ref.invalidate(lista);
        await ref.read(lista.future);
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
        children: [
          Wrap(
            spacing: 8,
            runSpacing: 4,
            children: [
              for (final (texto, origen) in _filtros)
                ChoiceChip(
                  key: Key('filtro-${origen?.valorApi ?? 'TODAS'}'),
                  label: Text(texto),
                  selected: filtro == origen,
                  onSelected: (_) => ref
                      .read(resultadosFiltroUiProvider.notifier)
                      .elegir(origen),
                ),
            ],
          ),
          const SizedBox(height: 12),
          ...notas.when(
            loading: () => const [
              Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: CircularProgressIndicator()),
              ),
            ],
            error: (_, _) => [
              AvisoCockpit(
                texto: deAlumnos
                    ? 'No pudimos cargar las notas de tus alumnos. Revisa tu conexión e inténtalo de nuevo.'
                    : 'No pudimos cargar tus notas. Revisa tu conexión e inténtalo de nuevo.',
                tono: TonoAviso.error,
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                key: const Key('botonReintentarNotas'),
                onPressed: () => ref.invalidate(lista),
                child: const Text('Reintentar'),
              ),
            ],
            data: (datos) => datos.isEmpty
                ? [
                    EstadoVacio(
                      icono: Icons.grading_outlined,
                      titulo: filtro == null
                          ? (deAlumnos
                                ? 'Tus alumnos todavía no tienen notas'
                                : 'Todavía no tienes notas')
                          : 'No hay notas de este tipo',
                      texto: filtro == null
                          ? (deAlumnos
                                ? 'Cuando tus alumnos terminen un simulacro o un examen, sus notas aparecerán aquí.'
                                : 'Cuando termines un simulacro o un examen, tu nota aparecerá aquí.')
                          : 'Prueba con otro filtro.',
                    ),
                  ]
                : [for (final nota in datos) _TarjetaNota(nota: nota)],
          ),
        ],
      ),
    );
  }
}

class _TarjetaNota extends StatelessWidget {
  final Resultado nota;

  const _TarjetaNota({required this.nota});

  @override
  Widget build(BuildContext context) {
    final puntaje = nota.puntaje == nota.puntaje.roundToDouble()
        ? nota.puntaje.toStringAsFixed(0)
        : nota.puntaje.toStringAsFixed(1);
    return Card(
      key: Key('nota-${nota.codigo}'),
      margin: const EdgeInsets.only(bottom: 10),
      child: ListTile(
        title: Text(nota.titulo),
        subtitle: Text(
          [
            ?nota.estudianteNombre,
            nota.origen.etiqueta,
            formatearFechaCorta(nota.dia),
            '$puntaje%',
          ].join(' · '),
        ),
        trailing: Insignia(
          nota.aprobado ? 'Aprobado' : 'No aprobado',
          tono: nota.aprobado ? TonoInsignia.exito : TonoInsignia.neutro,
        ),
      ),
    );
  }
}
