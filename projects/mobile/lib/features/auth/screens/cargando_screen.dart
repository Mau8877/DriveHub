import 'package:flutter/material.dart';

import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/marca_drivehub.dart';

/// Pantalla mientras se recupera la sesión guardada al abrir la app.
class CargandoScreen extends StatelessWidget {
  const CargandoScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const CockpitScaffold(
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            MarcaDriveHub(tamano: 64),
            SizedBox(height: 28),
            CircularProgressIndicator(),
          ],
        ),
      ),
    );
  }
}
