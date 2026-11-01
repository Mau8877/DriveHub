import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/cockpit_scaffold.dart';
import 'biblioteca_tutoriales_view.dart';
import 'mis_tutoriales_view.dart';

/// Los tutoriales del instructor: «Mis tutoriales» (CU33, los crea, edita, publica y archiva) y «Biblioteca» (CU35, los
/// publicados dirigidos a instructores). Se abre desde el Inicio del instructor.
class TutorialesInstructorScreen extends StatelessWidget {
  const TutorialesInstructorScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: CockpitScaffold(
        appBar: AppBar(
          title: const Text('Tutoriales'),
          bottom: const TabBar(
            tabs: [
              Tab(text: 'Mis tutoriales'),
              Tab(text: 'Biblioteca'),
            ],
          ),
        ),
        body: const TabBarView(
          children: [MisTutorialesView(), BibliotecaTutorialesView()],
        ),
        bottomNavigationBar: SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 12),
            child: FilledButton.icon(
              key: const Key('botonNuevoTutorial'),
              onPressed: () => context.push('/tutorials/new'),
              icon: const Icon(Icons.add),
              label: const Text('Nuevo tutorial'),
            ),
          ),
        ),
      ),
    );
  }
}
