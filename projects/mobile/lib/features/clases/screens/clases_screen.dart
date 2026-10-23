import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shared/widgets/estado_vacio.dart';
import '../../auth/providers/sesion_provider.dart';

enum _Vista { proximas, historial }

/// Clases del estudiante ("Próximas" e "Historial") o agenda del día del instructor (pestaña
/// "Clases" / "Agenda" del `UsuarioShell`).
///
/// * Las clases son de los casos de uso de Académico: hasta que existan, la lista es un estado
/// * vacío con el diseño final (selector, tarjeta).
class ClasesScreen extends ConsumerStatefulWidget {
  const ClasesScreen({super.key});

  @override
  ConsumerState<ClasesScreen> createState() => _ClasesScreenState();
}

class _ClasesScreenState extends ConsumerState<ClasesScreen> {
  _Vista _vista = _Vista.proximas;

  @override
  Widget build(BuildContext context) {
    final esInstructor =
        ref.watch(sesionProvider).usuario?.roles.contains('INSTRUCTOR') ?? false;

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
      children: [
        if (!esInstructor) ...[
          SegmentedButton<_Vista>(
            showSelectedIcon: false,
            segments: const [
              ButtonSegment(value: _Vista.proximas, label: Text('Próximas')),
              ButtonSegment(value: _Vista.historial, label: Text('Historial')),
            ],
            selected: {_vista},
            onSelectionChanged: (elegida) =>
                setState(() => _vista = elegida.first),
          ),
          const SizedBox(height: 16),
        ],
        Card(
          child: EstadoVacio(
            icono: esInstructor
                ? Icons.event_available_outlined
                : Icons.calendar_today_outlined,
            titulo: esInstructor
                ? 'No tienes clases hoy'
                : _vista == _Vista.proximas
                ? 'No tienes clases próximas'
                : 'Todavía no tienes historial',
            texto: esInstructor
                ? 'Cuando te asignen clases, aparecerán acá con su hora y su alumno.'
                : _vista == _Vista.proximas
                ? 'Las clases que reserves aparecerán acá.'
                : 'Tus clases pasadas aparecerán acá.',
          ),
        ),
      ],
    );
  }
}
