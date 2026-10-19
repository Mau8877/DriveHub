import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// Pantalla provisional de inicio de sesión.
///
/// * El login real es CU02. Por ahora solo muestra el aviso de "cuenta
/// * creada" cuando llega desde el registro (CU01) y deja ir a /registro.
class LoginPlaceholderScreen extends StatelessWidget {
  final bool cuentaCreada;

  const LoginPlaceholderScreen({super.key, this.cuentaCreada = false});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (cuentaCreada) ...[
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.secondaryContainer,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Text('Tu cuenta fue creada. Inicia sesión para continuar.'),
                  ),
                  const SizedBox(height: 16),
                ],
                Text('Iniciar sesión', style: Theme.of(context).textTheme.headlineSmall),
                const SizedBox(height: 4),
                const Text('Pendiente de implementar (CU02).'),
                const SizedBox(height: 24),
                OutlinedButton(
                  onPressed: () => context.go('/registro'),
                  child: const Text('Crear cuenta'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
