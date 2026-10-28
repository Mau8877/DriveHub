import 'package:flutter/material.dart';

import '../../../shared/widgets/cockpit_scaffold.dart';
import 'resultados_screen.dart';

/// «Notas de mis alumnos» (CU23, `/grades`): las notas de los alumnos asignados al instructor. Se abre desde su Inicio.
class NotasAlumnosScreen extends StatelessWidget {
  const NotasAlumnosScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return CockpitScaffold(
      appBar: AppBar(title: const Text('Notas de mis alumnos')),
      body: const ResultadosScreen(deAlumnos: true),
    );
  }
}
