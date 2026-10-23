import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../../../core/theme/app_theme.dart';
import '../providers/sesion_provider.dart';
import '../validators/registro_validators.dart';
import '../widgets/auto_ilustracion.dart';

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

    return Scaffold(
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _Cabecera(tema: tema),
            Padding(
              padding: const EdgeInsets.all(AppSpace.xl),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    if (widget.cuentaCreada) ...[
                      _aviso(
                        context,
                        'Tu cuenta fue creada. Inicia sesión para continuar.',
                      ),
                      const SizedBox(height: AppSpace.lg),
                    ],
                    if (motivo != null) ...[
                      _aviso(context, motivo),
                      const SizedBox(height: AppSpace.lg),
                    ],
                    Text('Iniciar sesión', style: tema.textTheme.headlineSmall),
                    const SizedBox(height: AppSpace.lg),
                    _Etiqueta('Correo', tema),
                    TextFormField(
                      key: const Key('campoEmail'),
                      controller: _email,
                      decoration: const InputDecoration(
                        prefixIcon: Icon(Icons.mail_outline),
                      ),
                      keyboardType: TextInputType.emailAddress,
                      autocorrect: false,
                      autofillHints: const [AutofillHints.email],
                      textInputAction: TextInputAction.next,
                      validator: validarEmail,
                    ),
                    const SizedBox(height: AppSpace.md),
                    _Etiqueta('Contraseña', tema),
                    TextFormField(
                      key: const Key('campoPassword'),
                      controller: _password,
                      decoration: InputDecoration(
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
                      autofillHints: const [AutofillHints.password],
                      onFieldSubmitted: (_) => _enviar(),
                      validator: (v) => (v == null || v.isEmpty)
                          ? 'La contraseña es obligatoria'
                          : null,
                    ),
                    Align(
                      alignment: Alignment.centerLeft,
                      child: TextButton(
                        key: const Key('botonOlvideContrasena'),
                        onPressed: () => context.go('/forgot-password'),
                        child: const Text('¿Olvidaste tu contraseña?'),
                      ),
                    ),
                    const SizedBox(height: AppSpace.sm),
                    FilledButton(
                      key: const Key('botonIngresar'),
                      onPressed: _enviando ? null : _enviar,
                      child: _enviando
                          ? const Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                SizedBox(
                                  height: 20,
                                  width: 20,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                  ),
                                ),
                                SizedBox(width: AppSpace.sm),
                                Text('Ingresando…'),
                              ],
                            )
                          : const Text('Ingresar'),
                    ),
                    const SizedBox(height: AppSpace.md),
                    OutlinedButton(
                      onPressed: () => context.go('/register'),
                      child: const Text('Crear cuenta'),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _aviso(BuildContext context, String texto) {
    return Container(
      padding: const EdgeInsets.all(AppSpace.lg),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.secondaryContainer,
        borderRadius: BorderRadius.circular(AppRadius.textarea),
      ),
      child: Text(texto),
    );
  }
}

/// Bloque ámbar de arriba: logo, saludo e ilustración (design.md 6.1).
class _Cabecera extends StatelessWidget {
  final ThemeData tema;

  const _Cabecera({required this.tema});

  @override
  Widget build(BuildContext context) {
    final esquema = tema.colorScheme;
    return Container(
      height: 330,
      padding: const EdgeInsets.fromLTRB(
        AppSpace.xxl,
        AppSpace.xxl,
        AppSpace.xxl,
        AppSpace.xl,
      ),
      decoration: BoxDecoration(
        color: esquema.secondary,
        borderRadius: const BorderRadius.vertical(bottom: Radius.circular(56)),
      ),
      child: SafeArea(
        bottom: false,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  Icons.directions_car_filled_outlined,
                  color: esquema.onSecondary,
                  size: 32,
                ),
                const SizedBox(width: AppSpace.sm),
                Text(
                  'DriveHub',
                  style: tema.textTheme.headlineMedium?.copyWith(
                    color: esquema.onSecondary,
                    fontSize: 30,
                  ),
                ),
              ],
            ),
            const SizedBox(height: AppSpace.xxl),
            ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 230),
              child: Text(
                '¡Qué bueno verte de nuevo!',
                style: AppTheme.conPeso(
                  tema.textTheme.headlineSmall!.copyWith(
                    color: esquema.onSecondary,
                    fontSize: 24,
                  ),
                  FontWeight.w800,
                ),
              ),
            ),
            const Spacer(),
            const Align(
              alignment: Alignment.bottomRight,
              child: AutoIlustracion(),
            ),
          ],
        ),
      ),
    );
  }
}

class _Etiqueta extends StatelessWidget {
  final String texto;
  final ThemeData tema;

  const _Etiqueta(this.texto, this.tema);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(6, 0, 0, 6),
      child: Text(texto, style: tema.textTheme.labelMedium),
    );
  }
}
