import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/providers/sesion_provider.dart';

/// Shell del rol Usuario -- AppBar + `NavigationBar` (Material 3) inferior,
/// tres secciones. Equivalente mobile de `UsuarioLayout` en el frontend
/// web (ver ARQUITECTURA_FRONTEND.md#layouts-por-rol): una barra inferior es
/// el patrón mobile estándar para navegación de nivel superior con pocas
/// secciones, a diferencia del sidebar que usa el rol Admin.
class UsuarioShell extends ConsumerStatefulWidget {
  const UsuarioShell({super.key});

  @override
  ConsumerState<UsuarioShell> createState() => _UsuarioShellState();
}

class _UsuarioSeccion {
  final String label;
  final IconData icon;

  const _UsuarioSeccion(this.label, this.icon);
}

const _secciones = [
  _UsuarioSeccion('Inicio', Icons.home_outlined),
  _UsuarioSeccion('Pedidos', Icons.list_alt_outlined),
  _UsuarioSeccion('Perfil', Icons.person_outline),
];

class _UsuarioShellState extends ConsumerState<UsuarioShell> {
  int _selected = 0;

  @override
  Widget build(BuildContext context) {
    final seccionActual = _secciones[_selected];

    return Scaffold(
      appBar: AppBar(
        title: const Text('App'),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: 'Cerrar sesión',
            onPressed: () => ref.read(sesionProvider.notifier).cerrarSesion(),
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
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selected,
        onDestinationSelected: (index) => setState(() => _selected = index),
        destinations: [
          for (final seccion in _secciones)
            NavigationDestination(icon: Icon(seccion.icon), label: seccion.label),
        ],
      ),
    );
  }
}
