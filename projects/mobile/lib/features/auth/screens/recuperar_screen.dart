import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/indicador_boton.dart';
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

    final tema = Theme.of(context);

    return CockpitScaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.chevron_left),
          tooltip: 'Volver',
          onPressed: () => context.go('/login'),
        ),
        title: const Text('Recuperar contraseña'),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 8, 24, 32),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  'Ingresa tu correo y te enviaremos un enlace para elegir una contraseña nueva.',
                  style: tema.textTheme.bodyMedium?.copyWith(
                    color: tema.colorScheme.onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 20),
                if (enviado)
                  const AvisoCockpit(
                    texto: _mensajeEnviado,
                    tono: TonoAviso.exito,
                  )
                else ...[
                  TextFormField(
                    key: const Key('campoEmail'),
                    controller: _email,
                    decoration: const InputDecoration(
                      labelText: 'Correo',
                      prefixIcon: Icon(Icons.mail_outline),
                    ),
                    keyboardType: TextInputType.emailAddress,
                    autocorrect: false,
                    onFieldSubmitted: (_) => _enviar(),
                    validator: validarEmail,
                  ),
                  if (estado.hasError) ...[
                    const SizedBox(height: 12),
                    Text(
                      'No pudimos procesar la solicitud. Intenta más tarde',
                      style: TextStyle(color: tema.colorScheme.error),
                    ),
                  ],
                  const SizedBox(height: 16),
                  FilledButton(
                    key: const Key('botonEnviarEnlace'),
                    onPressed: cargando ? null : _enviar,
                    child: cargando
                        ? const IndicadorBoton()
                        : const Text('Enviar enlace'),
                  ),
                ],
                const SizedBox(height: 16),
                Text(
                  'La recuperación solo funciona si verificaste tu correo desde la app.',
                  style: tema.textTheme.bodySmall,
                ),
                const SizedBox(height: 12),
                OutlinedButton(
                  onPressed: () => context.go('/login'),
                  child: const Text('Volver a iniciar sesión'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
