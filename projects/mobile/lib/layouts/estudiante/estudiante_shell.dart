import 'package:flutter/material.dart';

import '../shared/shell_con_barra.dart';

/// Menú del Estudiante, armado con los casos de uso móviles que le corresponden.
///
/// * Las secciones sin pantalla muestran el cuerpo de prueba hasta que se implemente
/// * su caso de uso. Una sección nueva se agrega con el primer CU que la necesite.
/// * El asistente IA (CU39 a CU42) y las notificaciones (CU37, CU38) no tienen sección
/// * propia: la barra admite 5 destinos y van como acceso aparte cuando existan.
const seccionesEstudiante = [
  SeccionShell('Inicio', Icons.home_outlined, 'resumen'),
  SeccionShell('Clases', Icons.calendar_month_outlined, 'CU13 a CU16'),
  SeccionShell('Estudio', Icons.menu_book_outlined, 'CU21, CU34 y CU35'),
  SeccionShell('Pagos', Icons.account_balance_wallet_outlined, 'CU26 y CU28'),
  SeccionShell('Perfil', Icons.person_outline, 'CU06'),
];

/// Shell del rol Estudiante -- AppBar + `NavigationBar` inferior.
class EstudianteShell extends StatelessWidget {
  const EstudianteShell({super.key});

  @override
  Widget build(BuildContext context) =>
      const ShellConBarra(secciones: seccionesEstudiante);
}
