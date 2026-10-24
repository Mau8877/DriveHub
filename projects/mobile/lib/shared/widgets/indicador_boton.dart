import 'package:flutter/material.dart';

/// Spinner para dentro de un botón de acción: toma el color del texto del botón (asfalto sobre el
/// ámbar), no el del tema, que sobre el fondo ámbar no se vería.
class IndicadorBoton extends StatelessWidget {
  const IndicadorBoton({super.key});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 20,
      width: 20,
      child: CircularProgressIndicator(
        strokeWidth: 2.5,
        color: Theme.of(context).colorScheme.onPrimary,
      ),
    );
  }
}
