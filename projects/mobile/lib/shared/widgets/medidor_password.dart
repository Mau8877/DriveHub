import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';

/// Fuerza de una contraseña, de 0 a 4. Es solo una ayuda visual: la regla que decide es la del
/// backend (9 a 64 caracteres).
int fuerzaDePassword(String valor) {
  if (valor.isEmpty) return 0;
  final puntos =
      (valor.length >= 9 ? 1 : 0) +
      (RegExp('[A-Z]').hasMatch(valor) && RegExp('[a-z]').hasMatch(valor)
          ? 1
          : 0) +
      (RegExp(r'\d').hasMatch(valor) ? 1 : 0) +
      (RegExp('[^A-Za-z0-9]').hasMatch(valor) ? 1 : 0);
  return puntos.clamp(1, 4);
}

/// Cuatro barras que se llenan según [fuerza] (rojo, ámbar, azul, verde), como el `meter` del mockup.
class MedidorPassword extends StatelessWidget {
  final int fuerza;

  const MedidorPassword({super.key, required this.fuerza});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final colores = context.cockpit;
    final color = switch (fuerza) {
      1 => tema.colorScheme.error,
      2 => AppColors.ambar,
      3 => colores.enlace,
      _ => colores.exito,
    };

    return Semantics(
      label: 'Fuerza de la contraseña: $fuerza de 4',
      child: Row(
        children: [
          for (var i = 0; i < 4; i++) ...[
            if (i > 0) const SizedBox(width: 5),
            Expanded(
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 250),
                height: 5,
                decoration: BoxDecoration(
                  color: i < fuerza ? color : colores.superficieSuave,
                  borderRadius: BorderRadius.circular(9),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
