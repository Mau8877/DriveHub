import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/providers/sesion_provider.dart';
import '../../shared/widgets/aviso_cockpit.dart';
import '../../shared/widgets/aviso_snack.dart';
import '../../shared/widgets/avatar_usuario.dart';
import '../../shared/widgets/cockpit_scaffold.dart';
import '../../shared/widgets/estado_vacio.dart';
import '../../shared/widgets/marca_drivehub.dart';

/// Una sección de la barra inferior de un shell.
class SeccionShell {
  final String label;
  final IconData icon;

  /// Casos de uso que va a mostrar la sección (ej. `CU13 a CU16`). Solo se ve en el
  /// cuerpo de prueba, hasta que la sección tenga su pantalla real.
  final String casosDeUso;

  /// Pantalla real de la sección. Sin ella se muestra el cuerpo de prueba.
  final WidgetBuilder? builder;

  const SeccionShell(this.label, this.icon, this.casosDeUso, {this.builder});
}

/// Estructura común de los shells de la app móvil (diseño Cockpit): barra superior transparente
/// y `NavigationBar` inferior con las secciones del rol.
///
/// * Cada rol (`EstudianteShell`, `InstructorShell`) solo declara sus secciones.
/// * La sección "Inicio" lleva la marca, los avisos y el avatar; las demás, su título.
/// * Con el correo sin verificar, un aviso con el botón "Verificar correo" queda fijo sobre
/// * "Inicio" y "Perfil". Cerrar sesión vive en Perfil.
/// ! No conoce lógica de negocio: el cuerpo de cada sección lo pone su feature.
class ShellConBarra extends ConsumerStatefulWidget {
  final List<SeccionShell> secciones;

  const ShellConBarra({super.key, required this.secciones});

  @override
  ConsumerState<ShellConBarra> createState() => _ShellConBarraState();
}

const _etiquetaInicio = 'Inicio';
const _etiquetaPerfil = 'Perfil';

class _ShellConBarraState extends ConsumerState<ShellConBarra>
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

  int get _indicePerfil =>
      widget.secciones.indexWhere((s) => s.label == _etiquetaPerfil);

  PreferredSizeWidget _barra(SeccionShell seccion) {
    if (seccion.label != _etiquetaInicio) {
      return AppBar(titleSpacing: 20, title: Text(seccion.label));
    }

    final usuario = ref.watch(sesionProvider).usuario;
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
              tooltip: 'Mi perfil',
              padding: EdgeInsets.zero,
              icon: AvatarUsuario(
                nombres: usuario.nombres,
                apellidos: usuario.apellidos,
                fotoUrl: usuario.fotoUrl,
                radio: 18,
              ),
              onPressed: _indicePerfil < 0
                  ? null
                  : () => setState(() => _selected = _indicePerfil),
            ),
          ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final correoSinVerificar =
        ref.watch(sesionProvider).usuario?.correoVerificado == false;
    final seccionActual = widget.secciones[_selected];
    final mostrarAviso =
        correoSinVerificar &&
        (seccionActual.label == _etiquetaInicio ||
            seccionActual.label == _etiquetaPerfil);

    return CockpitScaffold(
      appBar: _barra(seccionActual),
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
          Expanded(
            child:
                seccionActual.builder?.call(context) ??
                _CuerpoDePrueba(seccion: seccionActual),
          ),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selected,
        onDestinationSelected: (index) => setState(() => _selected = index),
        destinations: [
          for (final seccion in widget.secciones)
            NavigationDestination(
              icon: Icon(seccion.icon),
              label: seccion.label,
            ),
        ],
      ),
    );
  }
}

/// Lo que muestra una sección que todavía no tiene su pantalla: un estado vacío con el caso de
/// uso que la va a llenar.
class _CuerpoDePrueba extends StatelessWidget {
  final SeccionShell seccion;

  const _CuerpoDePrueba({required this.seccion});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
      children: [
        Card(
          child: EstadoVacio(
            icono: seccion.icon,
            titulo: seccion.label,
            texto:
                'Pantalla de prueba del layout -- acá va el contenido real de "${seccion.label}" (${seccion.casosDeUso}) cuando exista esa feature.',
          ),
        ),
      ],
    );
  }
}
