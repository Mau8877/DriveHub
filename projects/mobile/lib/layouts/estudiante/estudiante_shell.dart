import 'package:flutter/material.dart';

import '../../features/clases/screens/clases_screen.dart';
import '../../features/inicio/screens/inicio_screen.dart';
import '../../features/pagos/screens/pagos_screen.dart';
import '../../features/perfil/screens/perfil_screen.dart';
import '../shared/shell_con_barra.dart';

/// Menú del Estudiante, armado con los casos de uso móviles que le corresponden.
///
/// * Las secciones sin pantalla muestran el cuerpo de prueba hasta que se implemente
/// * su caso de uso. Una sección nueva se agrega con el primer CU que la necesite.
/// * El asistente IA (CU39 a CU42) y las notificaciones (CU37, CU38) no tienen sección
/// * propia: la barra admite 5 destinos y van como acceso aparte cuando existan.
final seccionesEstudiante = [
  SeccionShell(
    'Inicio',
    Icons.home_outlined,
    'resumen',
    builder: (_) => const InicioScreen(),
  ),
  SeccionShell(
    'Clases',
    Icons.calendar_month_outlined,
    'CU13 a CU16',
    builder: (_) => const ClasesScreen(),
  ),
  const SeccionShell('Estudio', Icons.menu_book_outlined, 'CU21, CU34 y CU35'),
  SeccionShell(
    'Pagos',
    Icons.account_balance_wallet_outlined,
    'CU26 y CU28',
    builder: (_) => const PagosScreen(),
  ),
  SeccionShell(
    'Perfil',
    Icons.person_outline,
    'CU06',
    builder: (_) => const PerfilScreen(),
  ),
];

/// Shell del rol Estudiante -- AppBar + `NavigationBar` inferior.
class EstudianteShell extends StatelessWidget {
  const EstudianteShell({super.key});

  @override
  Widget build(BuildContext context) =>
      ShellConBarra(secciones: seccionesEstudiante);
}
