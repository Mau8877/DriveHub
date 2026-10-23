import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../../../shared/widgets/marca_drivehub.dart';
import '../providers/sesion_provider.dart';
import '../validators/registro_validators.dart';

/// Por qué se llegó al login sin que la persona lo pidiera.
const _mensajesDeMotivo = {
  'ERR_IDE_05':
      'Tu cuenta está bloqueada temporalmente. Intenta de nuevo más tarde.',
  'ERR_IDE_06': 'Tu cuenta está inactiva. Contacta a tu administrador.',
  'ERR_IDE_07': 'La escuela de tu cuenta está suspendida.',
  'ERR_IDE_08': 'Tu cuenta no puede usar la app. Ingresa desde la web.',
  'ERR_IDE_09': 'Tu sesión terminó. Inicia sesión de nuevo.',
};

/// Inicio de sesión de la app (CU02): solo Estudiante e Instructor.
///
/// * Al ingresar bien no navega a mano: el redirect global del router lleva al
/// * inicio cuando la sesión cambia de estado.
class LoginScreen extends ConsumerStatefulWidget {
  final bool cuentaCreada;

  const LoginScreen({super.key, this.cuentaCreada = false});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _password = TextEditingController();

  bool _verPassword = false;
  bool _enviando = false;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _enviar() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _enviando = true);
    try {
      await ref
          .read(sesionProvider.notifier)
          .iniciarSesion(_email.text.trim(), _password.text);
    } catch (error) {
      if (mounted) _avisar(_mensajeDe(error));
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  String _mensajeDe(Object error) {
    if (error is! ApiException) {
      return 'No pudimos iniciar sesión. Inténtalo de nuevo más tarde.';
    }
    switch (error.code) {
      case 'ERR_IDE_04':
        return 'Correo o contraseña incorrectos';
      case 'ERR_IDE_06':
        return 'Tu cuenta está inactiva. Contacta a tu administrador';
      case 'ERR_IDE_07':
        return 'La escuela de tu cuenta está suspendida';
      case 'ERR_IDE_05':
      case 'ERR_IDE_08':
        // * El servidor dice cuándo reintentar (423) o qué plataforma usar (403)
        return error.message;
      default:
        return 'No pudimos iniciar sesión. Inténtalo de nuevo más tarde.';
    }
  }

  void _avisar(String mensaje) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(mensaje)));
  }

  @override
  Widget build(BuildContext context) {
    final motivo = _mensajesDeMotivo[ref.watch(sesionProvider).motivo];

    final tema = Theme.of(context);

    return CockpitScaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(24, 16, 24, 32),
            child: Form(
              key: _formKey,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Align(
                    alignment: Alignment.centerLeft,
                    child: LogoDriveHub(tamano: 34),
                  ),
                  const SizedBox(height: 28),
                  Text(
                    'Iniciar sesión',
                    style: tema.textTheme.headlineMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                      letterSpacing: -1,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Tu autoescuela, en tu bolsillo.',
                    style: tema.textTheme.bodyMedium?.copyWith(
                      color: tema.colorScheme.onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(height: 24),
                  if (widget.cuentaCreada) ...[
                    const AvisoCockpit(
                      texto: 'Tu cuenta fue creada. Inicia sesión para continuar.',
                      tono: TonoAviso.exito,
                    ),
                    const SizedBox(height: 16),
                  ],
                  if (motivo != null) ...[
                    AvisoCockpit(texto: motivo, tono: TonoAviso.advertencia),
                    const SizedBox(height: 16),
                  ],
                  TextFormField(
                    key: const Key('campoEmail'),
                    controller: _email,
                    decoration: const InputDecoration(
                      labelText: 'Correo',
                      prefixIcon: Icon(Icons.mail_outline),
                    ),
                    keyboardType: TextInputType.emailAddress,
                    autocorrect: false,
                    textInputAction: TextInputAction.next,
                    validator: validarEmail,
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    key: const Key('campoPassword'),
                    controller: _password,
                    decoration: InputDecoration(
                      labelText: 'Contraseña',
                      prefixIcon: const Icon(Icons.lock_outline),
                      suffixIcon: IconButton(
                        icon: Icon(
                          _verPassword
                              ? Icons.visibility_off_outlined
                              : Icons.visibility_outlined,
                        ),
                        tooltip: _verPassword
                            ? 'Ocultar contraseña'
                            : 'Mostrar contraseña',
                        onPressed: () =>
                            setState(() => _verPassword = !_verPassword),
                      ),
                    ),
                    obscureText: !_verPassword,
                    onFieldSubmitted: (_) => _enviar(),
                    validator: (v) => (v == null || v.isEmpty)
                        ? 'La contraseña es obligatoria'
                        : null,
                  ),
                  Align(
                    alignment: Alignment.centerRight,
                    child: TextButton(
                      key: const Key('botonOlvideContrasena'),
                      onPressed: () => context.go('/forgot-password'),
                      child: const Text('¿Olvidaste tu contraseña?'),
                    ),
                  ),
                  const SizedBox(height: 8),
                  FilledButton(
                    key: const Key('botonIngresar'),
                    onPressed: _enviando ? null : _enviar,
                    child: _enviando
                        ? const IndicadorBoton()
                        : const Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Text('Ingresar'),
                              SizedBox(width: 8),
                              Icon(Icons.arrow_forward, size: 18),
                            ],
                          ),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        '¿Eres estudiante nuevo?',
                        style: tema.textTheme.bodyMedium?.copyWith(
                          color: tema.colorScheme.onSurfaceVariant,
                        ),
                      ),
                      TextButton(
                        onPressed: () => context.go('/register'),
                        child: const Text('Crear cuenta'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
