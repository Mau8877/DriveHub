import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/providers/sesion_provider.dart';
import '../../features/clases/screens/clases_screen.dart';
import '../../features/inicio/screens/inicio_screen.dart';
import '../../features/perfil/screens/perfil_screen.dart';
import '../../shared/widgets/aviso_cockpit.dart';
import '../../shared/widgets/aviso_snack.dart';
import '../../shared/widgets/avatar_usuario.dart';
import '../../shared/widgets/cockpit_scaffold.dart';
import '../../shared/widgets/marca_drivehub.dart';

/// Shell del rol Usuario (diseño Cockpit): barra superior transparente y `NavigationBar` inferior
/// con tres pestañas (Inicio, Clases o Agenda y Perfil). Equivalente mobile de `UsuarioLayout` en el
/// frontend web (ver ARQUITECTURA_FRONTEND.md#layouts-por-rol): una barra inferior es el patrón
/// mobile estándar para navegación de nivel superior con pocas secciones, a diferencia del
/// sidebar que usa el rol Admin.
///
/// * El inicio lleva la marca, los avisos y el avatar; las otras pestañas, su título. Con el
/// * correo sin verificar, un aviso con el botón "Verificar correo" queda fijo sobre Inicio y Perfil.
class UsuarioShell extends ConsumerStatefulWidget {
  const UsuarioShell({super.key});

  @override
  ConsumerState<UsuarioShell> createState() => _UsuarioShellState();
}

const _indiceInicio = 0;
const _indiceClases = 1;
const _indicePerfil = 2;

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

  PreferredSizeWidget _barra(BuildContext context, bool esInstructor) {
    final usuario = ref.watch(sesionProvider).usuario;

    if (_selected == _indiceInicio) {
      return AppBar(
        titleSpacing: 20,
        title: const LogoDriveHub(tamano: 30),
        actions: [
          IconButton(
            tooltip: 'Avisos',
            icon: const Icon(Icons.notifications_none),
            onPressed: () => avisar(context, 'Todavía no tienes avisos nuevos'),
          ),
          if (usuario != null)
            Padding(
              padding: const EdgeInsets.only(right: 12, left: 4),
              child: IconButton(
                tooltip: 'Perfil',
                padding: EdgeInsets.zero,
                icon: AvatarUsuario(
                  nombres: usuario.nombres,
                  apellidos: usuario.apellidos,
                  fotoUrl: usuario.fotoUrl,
                  radio: 18,
                ),
                onPressed: () => setState(() => _selected = _indicePerfil),
              ),
            ),
        ],
      );
    }
    return AppBar(
      titleSpacing: 20,
      title: Text(
        _selected == _indiceClases
            ? (esInstructor ? 'Agenda' : 'Mis clases')
            : 'Mi perfil',
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final usuario = ref.watch(sesionProvider).usuario;
    final correoSinVerificar = usuario?.correoVerificado == false;
    final esInstructor = usuario?.roles.contains('INSTRUCTOR') ?? false;

    final contenido = switch (_selected) {
      _indiceInicio => const InicioScreen(),
      _indiceClases => const ClasesScreen(),
      _ => const PerfilScreen(),
    };
    final mostrarAviso = correoSinVerificar && _selected != _indiceClases;

    return CockpitScaffold(
      appBar: _barra(context, esInstructor),
      body: Column(
        children: [
          if (mostrarAviso)
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 0, 20, 8),
              child: AvisoCockpit(
                texto: 'Tu correo no está verificado.',
                tono: TonoAviso.advertencia,
                accion: TextButton(
                  onPressed: _verificando ? null : _verificarCorreo,
                  child: const Text('Verificar correo'),
                ),
              ),
            ),
          Expanded(child: contenido),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selected,
        onDestinationSelected: (index) => setState(() => _selected = index),
        destinations: [
          const NavigationDestination(
            icon: Icon(Icons.home_outlined),
            label: 'Inicio',
          ),
          NavigationDestination(
            icon: const Icon(Icons.calendar_today_outlined),
            label: esInstructor ? 'Agenda' : 'Clases',
          ),
          const NavigationDestination(
            icon: Icon(Icons.person_outline),
            label: 'Perfil',
          ),
        ],
      ),
    );
  }
}
