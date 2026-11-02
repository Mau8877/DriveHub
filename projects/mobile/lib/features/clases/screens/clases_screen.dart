import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/providers/sesion_provider.dart';
import 'agenda_instructor_screen.dart';
import 'clases_estudiante_screen.dart';

/// La pestaña «Clases» (estudiante) o «Agenda» (instructor) del `UsuarioShell`.
///
/// * El estudiante ve sus tres pestañas (CU14): [ClasesEstudianteScreen].
/// * El instructor ve su agenda por día (CU16): [AgendaInstructorScreen].
class ClasesScreen extends ConsumerWidget {
  const ClasesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final esInstructor = ref.watch(sesionProvider).usuario?.roles.contains('INSTRUCTOR') ?? false;
    return esInstructor ? const AgendaInstructorScreen() : const ClasesEstudianteScreen();
  }
}
