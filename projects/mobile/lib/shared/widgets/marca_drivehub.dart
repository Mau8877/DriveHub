import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';

/// Marca de DriveHub: una "D" con la línea central de una ruta, sobre un cuadrado ámbar.
/// Es el mismo dibujo de `DH.marca` del mockup.
class MarcaDriveHub extends StatelessWidget {
  final double tamano;

  const MarcaDriveHub({super.key, this.tamano = 30});

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'DriveHub',
      child: CustomPaint(
        size: Size.square(tamano),
        painter: const _MarcaPainter(),
      ),
    );
  }
}

class _MarcaPainter extends CustomPainter {
  const _MarcaPainter();

  @override
  void paint(Canvas canvas, Size size) {
    final e = size.width / 32;
    canvas.drawRRect(
      RRect.fromRectAndRadius(Offset.zero & size, Radius.circular(9 * e)),
      Paint()..color = AppColors.ambar,
    );

    final trazo = Paint()
      ..color = AppColors.asfalto
      ..style = PaintingStyle.stroke
      ..strokeJoin = StrokeJoin.round
      ..strokeWidth = 2.6 * e;
    final d = Path()
      ..moveTo(10 * e, 8 * e)
      ..lineTo(16.5 * e, 8 * e)
      ..arcToPoint(Offset(16.5 * e, 24 * e), radius: Radius.circular(8 * e))
      ..lineTo(10 * e, 24 * e)
      ..close();
    canvas.drawPath(d, trazo);

    // * Línea central discontinua de la ruta
    final linea = Paint()
      ..color = AppColors.asfalto
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeWidth = 2.2 * e;
    canvas.drawLine(Offset(13.5 * e, 12.5 * e), Offset(13.5 * e, 14 * e), linea);
    canvas.drawLine(Offset(13.5 * e, 17.5 * e), Offset(13.5 * e, 19.5 * e), linea);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

/// Marca + el nombre "DriveHub" con "Hub" en el acento.
class LogoDriveHub extends StatelessWidget {
  final double tamano;

  const LogoDriveHub({super.key, this.tamano = 30});

  @override
  Widget build(BuildContext context) {
    final estilo = Theme.of(context).textTheme.titleLarge?.copyWith(
      fontSize: tamano * 0.62,
      fontWeight: FontWeight.w800,
      letterSpacing: -0.4,
    );

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        MarcaDriveHub(tamano: tamano),
        SizedBox(width: tamano * 0.3),
        Text.rich(
          TextSpan(
            text: 'Drive',
            children: [
              TextSpan(
                text: 'Hub',
                style: TextStyle(color: context.cockpit.acento),
              ),
            ],
          ),
          style: estilo,
        ),
      ],
    );
  }
}
