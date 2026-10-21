import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../providers/recuperar_provider.dart';
import '../validators/registro_validators.dart';

/// Mensaje fijo: no revela si el correo existe ni si está verificado.
const _mensajeEnviado =
    'Si el correo está registrado y verificado, te enviamos un enlace. '
    'Revisa tu bandeja y ábrelo en tu navegador.';

/// Pedir el enlace para recuperar la contraseña (CU04). Pantalla pública.
///
/// * Elegir la contraseña nueva ocurre en la página web a la que lleva el enlace
/// * del correo (se abre en el navegador del teléfono); la app no la reemplaza.
/// * La advertencia de que hace falta el correo verificado se muestra siempre.
class RecuperarScreen extends ConsumerStatefulWidget {
  const RecuperarScreen({super.key});

  @override
  ConsumerState<RecuperarScreen> createState() => _RecuperarScreenState();
}

class _RecuperarScreenState extends ConsumerState<RecuperarScreen> {
  final _formKey = GlobalKey<FormState>();
  final _email = TextEditingController();

  @override
  void dispose() {
    _email.dispose();
    super.dispose();
  }

  Future<void> _enviar() async {
    if (!_formKey.currentState!.validate()) return;
    await ref.read(recuperarProvider.notifier).solicitar(_email.text.trim());
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(recuperarProvider);
    final cargando = estado.isLoading;
    final enviado = estado.valueOrNull == true;

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Form(
              key: _formKey,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    'Recuperar contraseña',
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Ingresa tu correo y te enviaremos un enlace para elegir una contraseña nueva.',
                  ),
                  const SizedBox(height: 16),
                  if (enviado)
                    _aviso(context, _mensajeEnviado)
                  else ...[
                    TextFormField(
                      key: const Key('campoEmail'),
                      controller: _email,
                      decoration: const InputDecoration(labelText: 'Correo'),
                      keyboardType: TextInputType.emailAddress,
                      autocorrect: false,
                      onFieldSubmitted: (_) => _enviar(),
                      validator: validarEmail,
                    ),
                    if (estado.hasError) ...[
                      const SizedBox(height: 12),
                      Text(
                        'No pudimos procesar la solicitud. Intenta más tarde',
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.error,
                        ),
                      ),
                    ],
                    const SizedBox(height: 16),
                    FilledButton(
                      key: const Key('botonEnviarEnlace'),
                      onPressed: cargando ? null : _enviar,
                      child: cargando
                          ? const SizedBox(
                              height: 20,
                              width: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Text('Enviar enlace'),
                    ),
                  ],
                  const SizedBox(height: 16),
                  Text(
                    'La recuperación solo funciona si verificaste tu correo desde la app.',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  const SizedBox(height: 12),
                  TextButton(
                    onPressed: () => context.go('/login'),
                    child: const Text('Volver a iniciar sesión'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _aviso(BuildContext context, String texto) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.secondaryContainer,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(texto),
    );
  }
}
