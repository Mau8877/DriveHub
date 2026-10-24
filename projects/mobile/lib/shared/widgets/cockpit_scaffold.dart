import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';

/// Fondo del diseño Cockpit: el color de fondo con un resplandor azul arriba a la izquierda y otro
/// ámbar abajo a la derecha.
///
/// * Los resplandores se desvanecen antes de llegar al centro y son muy tenues en el modo claro:
/// * el contenido (tarjetas opacas, texto) nunca queda sobre un degradado fuerte, que era lo que
/// * dificultaba la lectura en el primer mockup.
class FondoCockpit extends StatelessWidget {
  final Widget child;

  const FondoCockpit({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    final colores = context.cockpit;

    return ColoredBox(
      color: Theme.of(context).scaffoldBackgroundColor,
      child: DecoratedBox(
        decoration: BoxDecoration(
          gradient: RadialGradient(
            center: Alignment.topLeft,
            radius: 1.0,
            colors: [colores.brilloAzul, colores.brilloAzul.withValues(alpha: 0)],
            stops: const [0, 0.7],
          ),
        ),
        child: DecoratedBox(
          decoration: BoxDecoration(
            gradient: RadialGradient(
              center: Alignment.bottomRight,
              radius: 0.9,
              colors: [colores.brilloAmbar, colores.brilloAmbar.withValues(alpha: 0)],
              stops: const [0, 0.7],
            ),
          ),
          child: child,
        ),
      ),
    );
  }
}

/// `Scaffold` de la app: el mismo contenido sobre el [FondoCockpit].
///
/// * Cada pantalla lo usa en lugar de `Scaffold`. El `Scaffold` interno es transparente y el
/// * fondo se pinta una vez por pantalla (no en `MaterialApp.builder`), así durante las
/// * transiciones de ruta no se ven dos pantallas superpuestas.
class CockpitScaffold extends StatelessWidget {
  final PreferredSizeWidget? appBar;
  final Widget body;
  final Widget? bottomNavigationBar;
  final Widget? drawer;

  const CockpitScaffold({
    super.key,
    this.appBar,
    required this.body,
    this.bottomNavigationBar,
    this.drawer,
  });

  @override
  Widget build(BuildContext context) {
    return FondoCockpit(
      child: Scaffold(
        backgroundColor: Colors.transparent,
        appBar: appBar,
        body: body,
        bottomNavigationBar: bottomNavigationBar,
        drawer: drawer,
      ),
    );
  }
}
