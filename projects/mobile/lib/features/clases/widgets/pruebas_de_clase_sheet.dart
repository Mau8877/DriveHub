import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../../../shared/widgets/insignia.dart';
import '../models/clase_agenda.dart';
import '../models/prueba_clase.dart';
import '../providers/pruebas_de_clase_provider.dart';
import 'formato_clases.dart';
import 'validadores_prueba.dart';

/// Abre la hoja con las pruebas de una clase ya evaluada (CU18). La clase tiene que traer su `claseCodigo`.
Future<void> mostrarPruebasDeClase(BuildContext context, ClaseAgenda clase) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    builder: (_) => PruebasDeClaseSheet(clase: clase),
  );
}

/// Las pruebas de una clase, de solo lectura (CU18): una prueba no se corrige ni se borra.
///
/// * Una tarjeta por prueba, en el orden en que se hicieron: tipo, resultado, total y el puntaje de cada criterio. Las
///   observaciones solo se dibujan si las hay.
/// * Esta hoja sí se cierra tocando afuera: no hay nada que perder.
class PruebasDeClaseSheet extends ConsumerWidget {
  final ClaseAgenda clase;

  const PruebasDeClaseSheet({super.key, required this.clase});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tema = Theme.of(context);
    final pruebas = ref.watch(pruebasDeClaseDetailProvider(clase.claseCodigo!));

    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(20, 4, 20, 12),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Pruebas de la clase', style: tema.textTheme.titleLarge),
          const SizedBox(height: 2),
          Text(
            '${tituloDeDia(clase.fecha, DateTime.now())} · ${rangoDeHoras(clase.horaInicio, clase.horaFin)}',
            key: const Key('claseConPruebas'),
            style: tema.textTheme.titleMedium,
          ),
          Text(
            clase.estudianteNombre,
            style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant),
          ),
          const SizedBox(height: 16),
          pruebas.when(
            loading: () => const Padding(
              padding: EdgeInsets.all(32),
              child: Center(child: CircularProgressIndicator()),
            ),
            error: (error, _) => AvisoCockpit(
              key: const Key('errorCargarPruebas'),
              tono: TonoAviso.error,
              texto: mensajeDeError(error),
              accion: TextButton(
                onPressed: () => ref.invalidate(pruebasDeClaseDetailProvider(clase.claseCodigo!)),
                child: const Text('Reintentar'),
              ),
            ),
            data: (lista) => lista.isEmpty
                ? const EstadoVacio(
                    icono: Icons.fact_check_outlined,
                    titulo: 'Sin pruebas',
                    texto: 'Esta clase todavía no tiene pruebas.',
                  )
                : Column(
                    children: [
                      for (final prueba in lista) ...[_TarjetaDePrueba(prueba: prueba), const SizedBox(height: 10)],
                    ],
                  ),
          ),
          const SizedBox(height: 6),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton(
              key: const Key('cerrarPruebas'),
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Cerrar'),
            ),
          ),
        ],
      ),
    );
  }
}

class _TarjetaDePrueba extends StatelessWidget {
  final PruebaClase prueba;

  const _TarjetaDePrueba({required this.prueba});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final colorSecundario = tema.colorScheme.onSurfaceVariant;

    return Card(
      key: Key('prueba-${prueba.tipo.valorApi}'),
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(child: Text(prueba.tipo.etiqueta, style: tema.textTheme.titleMedium)),
                Insignia(
                  prueba.resultado.etiqueta,
                  tono: prueba.aprobada ? TonoInsignia.exito : TonoInsignia.advertencia,
                ),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              'Total: ${formatoPuntaje(prueba.puntajeTotal)} / 100',
              key: Key('total-${prueba.tipo.valorApi}'),
              style: tema.textTheme.titleSmall,
            ),
            const SizedBox(height: 8),
            for (final criterio in prueba.criterios)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 2),
                child: Row(
                  children: [
                    Expanded(child: Text(criterio.descripcion, style: tema.textTheme.bodyMedium)),
                    Text(
                      '${formatoPuntaje(criterio.puntaje)} / ${formatoPuntaje(criterio.maximo)}',
                      style: tema.textTheme.bodyMedium?.copyWith(color: colorSecundario),
                    ),
                  ],
                ),
              ),
            if (prueba.observaciones != null && prueba.observaciones!.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text('Observaciones', style: tema.textTheme.labelMedium?.copyWith(color: colorSecundario)),
              Text(prueba.observaciones!, key: Key('observaciones-${prueba.tipo.valorApi}')),
            ],
          ],
        ),
      ),
    );
  }
}
