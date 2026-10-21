import 'package:flutter/material.dart';

/// Pantalla mientras se recupera la sesión guardada al abrir la app.
class CargandoScreen extends StatelessWidget {
  const CargandoScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const Scaffold(body: Center(child: CircularProgressIndicator()));
  }
}
