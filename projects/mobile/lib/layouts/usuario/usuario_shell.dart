import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/providers/sesion_provider.dart';
import '../../features/perfil/screens/perfil_screen.dart';
import '../../shared/widgets/avatar_usuario.dart';

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

class _UsuarioShellState extends ConsumerState<UsuarioShell>
    with WidgetsBindingObserver {
  int _selected = 0;
  bool _verificando = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  /// Si la persona abrió el enlace de verificación en el navegador, al volver a la
  /// app se renueva la sesión para que el botón "Verificar correo" desaparezca.
  @override
  void didChangeAppLifecycleState(AppLifecycleState estado) {
    if (estado == AppLifecycleState.resumed &&
        ref.read(sesionProvider).usuario?.correoVerificado == false) {
      unawaited(ref.read(sesionProvider.notifier).renovar());
    }
  }

  Future<void> _verificarCorreo() async {
    final mensajes = ScaffoldMessenger.of(context);
    setState(() => _verificando = true);
    try {
      await ref.read(sesionProvider.notifier).verificarCorreo();
      // * Con ERR_IDE_10 la sesión se renueva y ya figura verificada: no se mandó nada
      final yaVerificado =
          ref.read(sesionProvider).usuario?.correoVerificado == true;
      mensajes.showSnackBar(
        SnackBar(
          content: Text(
            yaVerificado
                ? 'Tu correo ya estaba verificado'
                : 'Te enviamos un correo para verificar tu dirección',
          ),
        ),
      );
    } catch (_) {
      mensajes.showSnackBar(
        const SnackBar(
          content: Text('No pudimos enviar el correo. Intenta más tarde'),
        ),
      );
    } finally {
      if (mounted) setState(() => _verificando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final seccionActual = _secciones[_selected];
    final usuario = ref.watch(sesionProvider).usuario;
    final correoSinVerificar = usuario?.correoVerificado == false;
    const indicePerfil = 2;

    return Scaffold(
      appBar: AppBar(
        title: const Text('App'),
        actions: [
          if (correoSinVerificar)
            TextButton(
              onPressed: _verificando ? null : _verificarCorreo,
              child: const Text('Verificar correo'),
            ),
          if (usuario != null)
            IconButton(
              tooltip: 'Perfil',
              icon: AvatarUsuario(
                nombres: usuario.nombres,
                apellidos: usuario.apellidos,
                fotoUrl: usuario.fotoUrl,
                radio: 16,
              ),
              onPressed: () => setState(() => _selected = indicePerfil),
            ),
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: 'Cerrar sesión',
            onPressed: () => ref.read(sesionProvider.notifier).cerrarSesion(),
          ),
        ],
      ),
      body: _selected == indicePerfil
          ? const PerfilScreen()
          : Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    seccionActual.label,
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
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
            NavigationDestination(
              icon: Icon(seccion.icon),
              label: seccion.label,
            ),
        ],
      ),
    );
  }
}
