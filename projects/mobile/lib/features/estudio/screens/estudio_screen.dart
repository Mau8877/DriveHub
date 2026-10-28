import 'package:flutter/material.dart';

import '../../manuales/screens/manuales_screen.dart';
import '../../resultados/screens/resultados_screen.dart';
import '../../simulacros/screens/simulacros_screen.dart';
import '../../tutoriales/screens/biblioteca_tutoriales_view.dart';

/// La sección «Estudio» del estudiante (pestaña del `EstudianteShell`): sus pestañas agrupan lo que sirve para
/// prepararse y ver cómo le va.
///
/// * «Simulacros» (CU21), «Manuales» (CU34) y «Mis notas» (CU23). Los tutoriales (CU35) suman su pestaña con su
///   caso de uso.
class EstudioScreen extends StatelessWidget {
  const EstudioScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const DefaultTabController(
      length: 4,
      child: Column(
        children: [
          TabBar(
            isScrollable: true,
            tabAlignment: TabAlignment.start,
            tabs: [
              Tab(text: 'Simulacros'),
              Tab(text: 'Manuales'),
              Tab(text: 'Tutoriales'),
              Tab(text: 'Mis notas'),
            ],
          ),
          Expanded(
            child: TabBarView(
              children: [
                SimulacrosScreen(),
                ManualesScreen(),
                BibliotecaTutorialesView(),
                ResultadosScreen(),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
