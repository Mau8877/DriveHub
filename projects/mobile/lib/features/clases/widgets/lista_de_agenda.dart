import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/offline/modelos_offline.dart';
import '../../offline/providers/offline_provider.dart';
import '../models/clase_agenda.dart';
import 'clase_agenda_card.dart';
import 'dialogo_cancelar_clase_agenda.dart';
import 'evaluar_clase_sheet.dart';
import 'formulario_clase_sheet.dart';
import 'pendientes_offline.dart';
import 'pruebas_de_clase_sheet.dart';

/// Las clases del día del instructor (CU16), una tarjeta por clase.
///
/// * El orden lo pone el backend (por hora de inicio): la app no reordena.
/// * Cada tarjeta abre el diálogo de cancelar (CU16), la hoja de cerrar y de editar (CU17) o la de evaluar y de ver la
///   prueba (CU18) según lo que el servidor dijo de esa clase.
class ListaDeAgenda extends ConsumerWidget {
  final List<ClaseAgenda> clases;

  const ListaDeAgenda({super.key, required this.clases});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final offline = ref.watch(offlineProvider);
    return Column(
      children: [
        for (final clase in clases) ...[
          ClaseAgendaCard(
            key: Key('clase-${clase.codigo}'),
            clase: clase,
            etiquetaPendiente: _etiqueta(offline, clase),
            alCancelar: () => mostrarDialogoCancelarClase(context, clase),
            alCerrar: () => mostrarCerrarClase(context, clase),
            alEditar: () => mostrarEditarClase(context, clase),
            alEvaluar: () => mostrarEvaluarClase(context, clase),
            alVerPrueba: () => mostrarPruebasDeClase(context, clase),
          ),
          const SizedBox(height: 10),
        ],
      ],
    );
  }

  static String? _etiqueta(EstadoOffline offline, ClaseAgenda clase) {
    final pendiente = pendienteDeClase(offline, clase);
    return pendiente == null ? null : etiquetaDePendiente(pendiente);
  }
}
