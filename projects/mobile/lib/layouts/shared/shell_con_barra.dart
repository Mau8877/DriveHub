import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../features/auth/providers/sesion_provider.dart';

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

/// Estructura común de los shells de la app móvil: `AppBar` con "Verificar correo" y
/// "Cerrar sesión", y `NavigationBar` (Material 3) inferior con las secciones del rol.
///
/// * Cada rol (`EstudianteShell`, `InstructorShell`) solo declara sus secciones.
/// * ! No conoce lógica de negocio: el cuerpo de cada sección lo pone su feature.
class ShellConBarra extends ConsumerStatefulWidget {
  final List<SeccionShell> secciones;

  const ShellConBarra({super.key, required this.secciones});

  @override
  ConsumerState<ShellConBarra> createState() => _ShellConBarraState();
}

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

  @override
  Widget build(BuildContext context) {
    final correoSinVerificar =
        ref.watch(sesionProvider).usuario?.correoVerificado == false;
    final seccionActual = widget.secciones[_selected];

    return Scaffold(
      appBar: AppBar(
        title: Text(seccionActual.label),
        titleSpacing: AppSpace.lg,
        actions: [
          _BotonIconoCircular(
            icon: Icons.logout,
            tooltip: 'Cerrar sesión',
            onPressed: () => ref.read(sesionProvider.notifier).cerrarSesion(),
          ),
          const SizedBox(width: AppSpace.lg),
        ],
        // * Solo mientras el correo no está verificado; es secundario, nunca ámbar
        bottom: correoSinVerificar
            ? PreferredSize(
                preferredSize: const Size.fromHeight(60),
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(
                    AppSpace.lg,
                    0,
                    AppSpace.lg,
                    AppSpace.sm,
                  ),
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: OutlinedButton.icon(
                      onPressed: _verificando ? null : _verificarCorreo,
                      icon: const Icon(Icons.mail_outline),
                      label: const Text('Verificar correo'),
                      style: OutlinedButton.styleFrom(
                        minimumSize: const Size(0, 48),
                        padding: const EdgeInsets.symmetric(
                          horizontal: AppSpace.lg,
                        ),
                      ),
                    ),
                  ),
                ),
              )
            : null,
      ),
      body:
          seccionActual.builder?.call(context) ??
          _CuerpoDePrueba(seccion: seccionActual),
      bottomNavigationBar: _BarraInferior(
        secciones: widget.secciones,
        seleccionada: _selected,
        alElegir: (index) => setState(() => _selected = index),
      ),
    );
  }
}

/// Botón de ícono circular de 44 dp con la sombra suave de las tarjetas (design.md 3.8).
class _BotonIconoCircular extends StatelessWidget {
  final IconData icon;
  final String tooltip;
  final VoidCallback onPressed;

  const _BotonIconoCircular({
    required this.icon,
    required this.tooltip,
    required this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surface,
          shape: BoxShape.circle,
          boxShadow: context.semantica.cardShadow,
        ),
        child: IconButton(
          icon: Icon(icon),
          tooltip: tooltip,
          onPressed: onPressed,
          constraints: const BoxConstraints.tightFor(width: 44, height: 44),
        ),
      ),
    );
  }
}

/// `NavigationBar` sobre una superficie con esquinas superiores de 28 dp y sombra.
class _BarraInferior extends StatelessWidget {
  final List<SeccionShell> secciones;
  final int seleccionada;
  final ValueChanged<int> alElegir;

  const _BarraInferior({
    required this.secciones,
    required this.seleccionada,
    required this.alElegir,
  });

  @override
  Widget build(BuildContext context) {
    final esquema = Theme.of(context).colorScheme;
    return DecoratedBox(
      decoration: BoxDecoration(
        color: esquema.surface,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(AppRadius.card),
        ),
        boxShadow: [
          BoxShadow(
            color: AppColors.asfalto.withValues(
              alpha: esquema.brightness == Brightness.light ? .07 : 0,
            ),
            blurRadius: 14,
            offset: const Offset(0, -4),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(AppRadius.card),
        ),
        child: NavigationBar(
          selectedIndex: seleccionada,
          onDestinationSelected: alElegir,
          destinations: [
            for (final seccion in secciones)
              NavigationDestination(
                icon: Icon(seccion.icon),
                label: seccion.label,
              ),
          ],
        ),
      ),
    );
  }
}

class _CuerpoDePrueba extends StatelessWidget {
  final SeccionShell seccion;

  const _CuerpoDePrueba({required this.seccion});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(seccion.label, style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 8),
          Text(
            'Pantalla de prueba del layout -- acá va el contenido real de "${seccion.label}" (${seccion.casosDeUso}) cuando exista esa feature.',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ],
      ),
    );
  }
}
