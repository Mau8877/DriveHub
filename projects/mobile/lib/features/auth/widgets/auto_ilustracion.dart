import 'package:flutter/material.dart';

import '../../../core/theme/app_theme.dart';

/// Auto de perfil sobre una línea de camino, con formas planas de la paleta
/// (design.md 1.1, principio 5). Cuerpo blanco, ventanas azul técnico, ruedas asfalto.
class AutoIlustracion extends StatelessWidget {
  final double ancho;

  const AutoIlustracion({super.key, this.ancho = 140});

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Ilustración de un auto',
      child: ExcludeSemantics(
        child: CustomPaint(
          size: Size(ancho, ancho * .48),
          painter: _AutoPainter(),
        ),
      ),
    );
  }
}

class _AutoPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final h = size.height;
    final pintura = Paint()..style = PaintingStyle.fill;

    // Camino
    pintura.color = AppColors.asfalto;
    canvas.drawRRect(
      RRect.fromLTRBR(0, h * .92, w, h, Radius.circular(h * .04)),
      pintura,
    );

    // Cabina
    pintura.color = AppColors.blanco;
    final cabina = Path()
      ..moveTo(w * .25, h * .52)
      ..lineTo(w * .35, h * .16)
      ..quadraticBezierTo(w * .37, h * .10, w * .44, h * .10)
      ..lineTo(w * .62, h * .10)
      ..quadraticBezierTo(w * .69, h * .10, w * .73, h * .20)
      ..lineTo(w * .82, h * .52)
      ..close();
    canvas.drawPath(cabina, pintura);

    // Carrocería
    canvas.drawRRect(
      RRect.fromLTRBR(
        w * .04,
        h * .46,
        w * .96,
        h * .84,
        Radius.circular(h * .16),
      ),
      pintura,
    );

    // Ventanas
    pintura.color = AppColors.azul;
    final ventanaIzquierda = Path()
      ..moveTo(w * .31, h * .46)
      ..lineTo(w * .385, h * .20)
      ..lineTo(w * .50, h * .20)
      ..lineTo(w * .50, h * .46)
      ..close();
    final ventanaDerecha = Path()
      ..moveTo(w * .54, h * .20)
      ..lineTo(w * .62, h * .20)
      ..quadraticBezierTo(w * .66, h * .20, w * .685, h * .27)
      ..lineTo(w * .74, h * .46)
      ..lineTo(w * .54, h * .46)
      ..close();
    canvas.drawPath(ventanaIzquierda, pintura);
    canvas.drawPath(ventanaDerecha, pintura);

    // Faro
    pintura.color = AppColors.ambar;
    canvas.drawRRect(
      RRect.fromLTRBR(
        w * .90,
        h * .55,
        w * .96,
        h * .63,
        Radius.circular(h * .03),
      ),
      pintura,
    );

    // Ruedas
    for (final x in [w * .26, w * .74]) {
      pintura.color = AppColors.asfalto;
      canvas.drawCircle(Offset(x, h * .84), h * .15, pintura);
      pintura.color = AppColors.grisClaro;
      canvas.drawCircle(Offset(x, h * .84), h * .06, pintura);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
