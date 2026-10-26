import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shared/widgets/estado_vacio.dart';
import '../../auth/providers/sesion_provider.dart';
import 'clases_estudiante_screen.dart';

/// La pestaña «Clases» (estudiante) o «Agenda» (instructor) del `UsuarioShell`.
///
/// * El estudiante ve sus tres pestañas (CU14): [ClasesEstudianteScreen].
/// * La agenda del instructor es de los casos de uso de Académico que le siguen (CU16): hasta que existan, es un
/// * estado vacío con el diseño final.
class ClasesScreen extends ConsumerWidget {
  const ClasesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final esInstructor = ref.watch(sesionProvider).usuario?.roles.contains('INSTRUCTOR') ?? false;
    if (!esInstructor) return const ClasesEstudianteScreen();

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
      children: const [
        Card(
          child: EstadoVacio(
            icono: Icons.event_available_outlined,
            titulo: 'No tienes clases hoy',
            texto: 'Cuando te asignen clases, aparecerán acá con su hora y su alumno.',
          ),
        ),
      ],
    );
  }
}
