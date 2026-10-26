import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_theme.dart';
import '../../horarios/models/fechas.dart';
import '../models/momento_dia.dart';
import '../providers/franjas_disponibles_provider.dart';

/// Cuántos días hacia adelante se pueden elegir en «Reservar» (contando hoy). El backend genera franjas para 28.
const int diasElegibles = 14;

/// Los filtros de «Reservar» (CU14): el día, el momento del día y el instructor.
///
/// * Solo cambian el estado de los filtros (`reservar*UiProvider`); la lista de franjas se vuelve a pedir sola.
class FiltrosReservar extends ConsumerWidget {
  const FiltrosReservar({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: const [
        _TiraDeDias(),
        SizedBox(height: 12),
        _MomentosDelDia(),
        _Instructores(),
      ],
    );
  }
}

class _TiraDeDias extends ConsumerWidget {
  const _TiraDeDias();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final elegido = ref.watch(reservarFechaUiProvider);
    final hoy = soloFecha(DateTime.now());

    return SizedBox(
      height: 72,
      child: ListView.separated(
        key: const Key('tiraDeDias'),
        scrollDirection: Axis.horizontal,
        itemCount: diasElegibles,
        separatorBuilder: (_, _) => const SizedBox(width: 8),
        itemBuilder: (context, i) {
          final dia = sumarDias(hoy, i);
          return _Dia(
            key: Key('dia-${fechaParaApi(dia)}'),
            fecha: dia,
            esHoy: i == 0,
            elegido: dia == elegido,
            alTocar: () => ref.read(reservarFechaUiProvider.notifier).elegir(dia),
          );
        },
      ),
    );
  }
}

class _Dia extends StatelessWidget {
  final DateTime fecha;
  final bool esHoy;
  final bool elegido;
  final VoidCallback alTocar;

  const _Dia({super.key, required this.fecha, required this.esHoy, required this.elegido, required this.alTocar});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final colores = tema.colorScheme;
    final fondo = elegido ? colores.primary : tema.cardColor;
    final texto = elegido ? colores.onPrimary : colores.onSurface;
    final apagado = elegido ? colores.onPrimary : colores.onSurfaceVariant;

    return Semantics(
      button: true,
      selected: elegido,
      label: '${esHoy ? 'Hoy, ' : ''}${nombreDeDiaCapitalizado(fecha.weekday)} ${fecha.day} de ${mesCorto(fecha.month)}',
      child: ExcludeSemantics(
        child: Material(
          color: fondo,
          borderRadius: BorderRadius.circular(AppRadios.control),
          child: InkWell(
            borderRadius: BorderRadius.circular(AppRadios.control),
            onTap: alTocar,
            child: Container(
              width: 56,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(AppRadios.control),
                border: Border.all(color: elegido ? colores.primary : context.cockpit.borde),
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    nombreDeDia(fecha.weekday).substring(0, 3),
                    style: tema.textTheme.labelMedium?.copyWith(color: apagado),
                  ),
                  Text('${fecha.day}', style: tema.textTheme.titleLarge?.copyWith(color: texto)),
                  if (esHoy)
                    Container(
                      width: 5,
                      height: 5,
                      margin: const EdgeInsets.only(top: 2),
                      decoration: BoxDecoration(color: elegido ? colores.onPrimary : context.cockpit.acento, shape: BoxShape.circle),
                    ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _MomentosDelDia extends ConsumerWidget {
  const _MomentosDelDia();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final elegido = ref.watch(reservarMomentoUiProvider);

    return Wrap(
      spacing: 8,
      runSpacing: 4,
      children: [
        for (final momento in MomentoDia.values)
          ChoiceChip(
            key: Key('momento-${momento.name}'),
            label: Text(momento.etiqueta),
            selected: elegido == momento,
            onSelected: (_) => ref.read(reservarMomentoUiProvider.notifier).elegir(momento),
          ),
      ],
    );
  }
}

/// Los instructores del día como chips en una fila que se desliza: «Todos» y uno por cada instructor con franjas.
///
/// * Si la lista no carga, el filtro simplemente no se ofrece: las franjas se pueden ver igual.
class _Instructores extends ConsumerWidget {
  const _Instructores();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final instructores = ref.watch(instructoresDisponiblesListProvider).maybeWhen(data: (l) => l, orElse: () => const []);
    if (instructores.isEmpty) return const SizedBox.shrink();
    final elegido = ref.watch(reservarInstructorEfectivoProvider);
    final notifier = ref.read(reservarInstructorUiProvider.notifier);

    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: SingleChildScrollView(
        key: const Key('instructores'),
        scrollDirection: Axis.horizontal,
        child: Row(
          children: [
            ChoiceChip(
              key: const Key('instructor-todos'),
              label: const Text('Todos los instructores'),
              selected: elegido == null,
              onSelected: (_) => notifier.elegir(null),
            ),
            for (final instructor in instructores) ...[
              const SizedBox(width: 8),
              ChoiceChip(
                key: Key('instructor-${instructor.codigo}'),
                label: Text(instructor.nombre),
                selected: elegido == instructor.codigo,
                onSelected: (_) => notifier.elegir(instructor.codigo),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
