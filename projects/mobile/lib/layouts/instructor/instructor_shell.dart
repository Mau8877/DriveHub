import 'package:flutter/material.dart';

import '../../features/clases/screens/clases_screen.dart';
import '../../features/horarios/screens/horarios_screen.dart';
import '../../features/inicio/screens/inicio_screen.dart';
import '../../features/perfil/screens/perfil_screen.dart';
import '../shared/shell_con_barra.dart';

/// Menú del Instructor, armado con los casos de uso móviles que le corresponden.
///
/// * Las secciones sin pantalla muestran el cuerpo de prueba hasta que se implemente
/// * su caso de uso. Los tutoriales (CU33 y CU35) y las notificaciones (CU37, CU38) no
/// * tienen sección propia: la barra admite 5 destinos y van como acceso aparte.
final seccionesInstructor = [
  SeccionShell(
    'Inicio',
    Icons.home_outlined,
    'resumen',
    builder: (_) => const InicioScreen(),
  ),
  SeccionShell(
    'Horarios',
    Icons.schedule_outlined,
    'CU12',
    builder: (_) => const HorariosScreen(),
  ),
  SeccionShell(
    'Clases',
    Icons.assignment_outlined,
    'CU16 a CU18',
    builder: (_) => const ClasesScreen(),
  ),
  const SeccionShell('Pagos', Icons.account_balance_wallet_outlined, 'CU29'),
  SeccionShell(
    'Perfil',
    Icons.person_outline,
    'CU06',
    builder: (_) => const PerfilScreen(),
  ),
];

/// Shell del rol Instructor -- AppBar + `NavigationBar` inferior.
class InstructorShell extends StatelessWidget {
  const InstructorShell({super.key});

  @override
  Widget build(BuildContext context) =>
      ShellConBarra(secciones: seccionesInstructor);
}
