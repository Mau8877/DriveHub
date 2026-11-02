import 'package:flutter/material.dart';

import '../models/clase_agenda.dart';
import 'clase_agenda_card.dart';
import 'dialogo_cancelar_clase_agenda.dart';

/// Las clases del día del instructor (CU16), una tarjeta por clase.
///
/// * El orden lo pone el backend (por hora de inicio): la app no reordena.
class ListaDeAgenda extends StatelessWidget {
  final List<ClaseAgenda> clases;

  const ListaDeAgenda({super.key, required this.clases});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        for (final clase in clases) ...[
          ClaseAgendaCard(
            key: Key('clase-${clase.codigo}'),
            clase: clase,
            alCancelar: () => mostrarDialogoCancelarClase(context, clase),
          ),
          const SizedBox(height: 10),
        ],
      ],
    );
  }
}
