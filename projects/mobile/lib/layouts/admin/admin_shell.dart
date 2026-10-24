import 'package:flutter/material.dart';

import '../../shared/widgets/cockpit_scaffold.dart';

/// Shell del rol Admin -- AppBar + `NavigationDrawer` (Material 3, tres
/// secciones). Equivalente mobile de `AdminLayout` en el frontend web (ver
/// ARQUITECTURA_FRONTEND.md#layouts-por-rol): un Drawer calza mejor que un
/// sidebar fijo en una pantalla angosta, pero cumple el mismo rol -- la
/// navegación de nivel superior del rol admin.
class AdminShell extends StatefulWidget {
  const AdminShell({super.key});

  @override
  State<AdminShell> createState() => _AdminShellState();
}

class _AdminSeccion {
  final String label;
  final IconData icon;

  const _AdminSeccion(this.label, this.icon);
}

const _secciones = [
  _AdminSeccion('Dashboard', Icons.dashboard_outlined),
  _AdminSeccion('Usuarios', Icons.people_outline),
  _AdminSeccion('Pedidos', Icons.list_alt_outlined),
];

class _AdminShellState extends State<AdminShell> {
  int _selected = 0;

  @override
  Widget build(BuildContext context) {
    final seccionActual = _secciones[_selected];

    return CockpitScaffold(
      appBar: AppBar(title: const Text('App Admin')),
      drawer: NavigationDrawer(
        selectedIndex: _selected,
        onDestinationSelected: (index) {
          setState(() => _selected = index);
          Navigator.pop(context);
        },
        children: [
          const Padding(
            padding: EdgeInsets.fromLTRB(16, 16, 16, 8),
            child: Text('App Admin', style: TextStyle(fontWeight: FontWeight.w600)),
          ),
          for (final seccion in _secciones)
            NavigationDrawerDestination(
              icon: Icon(seccion.icon),
              label: Text(seccion.label),
            ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(seccionActual.label, style: Theme.of(context).textTheme.headlineSmall),
            const SizedBox(height: 8),
            Text(
              'Pantalla de prueba del layout -- acá va el contenido real de "${seccionActual.label}" cuando exista esa feature.',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ],
        ),
      ),
    );
  }
}
