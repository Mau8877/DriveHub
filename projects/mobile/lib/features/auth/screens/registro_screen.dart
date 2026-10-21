import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../models/registro_estudiante_request.dart';
import '../providers/registro_provider.dart';
import '../validators/registro_validators.dart';

/// Registro del estudiante (CU01). Pantalla pública: sin sesión y sin
/// auto-login, al terminar vuelve al login.
///
/// * La escuela se escribe como "código de escuela" (su slug): la app no
/// * tiene enlaces por escuela. El backend responde ERR_IDE_03 si no existe.
class RegistroScreen extends ConsumerStatefulWidget {
  const RegistroScreen({super.key});

  @override
  ConsumerState<RegistroScreen> createState() => _RegistroScreenState();
}

class _RegistroScreenState extends ConsumerState<RegistroScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nombres = TextEditingController();
  final _apellidos = TextEditingController();
  final _fecha = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  final _telefono = TextEditingController();
  final _escuela = TextEditingController();

  DateTime? _fechaNacimiento;
  bool _verPassword = false;

  // * Errores que devuelve el backend, mostrados bajo su campo hasta el próximo envío
  String? _errorEmail;
  String? _errorFecha;
  String? _errorEscuela;

  @override
  void dispose() {
    for (final c in [_nombres, _apellidos, _fecha, _email, _password, _telefono, _escuela]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _elegirFecha() async {
    final hoy = DateTime.now();
    final elegida = await showDatePicker(
      context: context,
      initialDate: _fechaNacimiento ?? DateTime(hoy.year - edadMinimaRegistro, hoy.month, hoy.day),
      firstDate: DateTime(1900),
      lastDate: hoy,
      helpText: 'Fecha de nacimiento',
    );
    if (elegida == null) return;
    setState(() {
      _fechaNacimiento = elegida;
      _fecha.text =
          '${elegida.day.toString().padLeft(2, '0')}/${elegida.month.toString().padLeft(2, '0')}/${elegida.year}';
    });
  }

  Future<void> _enviar() async {
    setState(() {
      _errorEmail = null;
      _errorFecha = null;
      _errorEscuela = null;
    });
    if (!_formKey.currentState!.validate()) return;

    final telefono = _telefono.text.trim();
    await ref.read(registroProvider.notifier).registrar(
          RegistroEstudianteRequest(
            nombres: _nombres.text.trim(),
            apellidos: _apellidos.text.trim(),
            fechaNacimiento: _fechaNacimiento!,
            email: _email.text.trim(),
            password: _password.text,
            telefono: telefono.isEmpty ? null : telefono,
            escuelaSlug: _escuela.text.trim().toLowerCase(),
          ),
        );
    if (!mounted) return;

    final estado = ref.read(registroProvider);
    if (!estado.hasError) {
      context.go('/login?registrado=1');
      return;
    }

    setState(() {
      switch (errorDeRegistro(estado.error!)) {
        case ErrorRegistro.correoEnUso:
          _errorEmail = 'Este correo ya está registrado';
        case ErrorRegistro.menorDeEdad:
          _errorFecha = 'Debes tener al menos $edadMinimaRegistro años';
        case ErrorRegistro.escuelaNoDisponible:
          _errorEscuela = 'La escuela no existe o no está disponible';
        case ErrorRegistro.validacion:
          _avisar('Revisa los datos ingresados e inténtalo de nuevo.');
        case ErrorRegistro.general:
          _avisar('No pudimos completar el registro. Inténtalo de nuevo más tarde.');
      }
    });
    _formKey.currentState!.validate();
  }

  void _avisar(String mensaje) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(mensaje)));
  }

  @override
  Widget build(BuildContext context) {
    final enviando = ref.watch(registroProvider).isLoading;

    return Scaffold(
      appBar: AppBar(title: const Text('Crear cuenta')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextFormField(
                  key: const Key('campoEscuela'),
                  controller: _escuela,
                  decoration: const InputDecoration(labelText: 'Código de escuela'),
                  autocorrect: false,
                  textInputAction: TextInputAction.next,
                  validator: (v) => validarCodigoEscuela(v) ?? _errorEscuela,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  key: const Key('campoNombres'),
                  controller: _nombres,
                  decoration: const InputDecoration(labelText: 'Nombres'),
                  textCapitalization: TextCapitalization.words,
                  textInputAction: TextInputAction.next,
                  validator: validarNombres,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  key: const Key('campoApellidos'),
                  controller: _apellidos,
                  decoration: const InputDecoration(labelText: 'Apellidos'),
                  textCapitalization: TextCapitalization.words,
                  textInputAction: TextInputAction.next,
                  validator: validarApellidos,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  key: const Key('campoFecha'),
                  controller: _fecha,
                  readOnly: true,
                  decoration: const InputDecoration(
                    labelText: 'Fecha de nacimiento',
                    suffixIcon: Icon(Icons.calendar_today_outlined),
                  ),
                  onTap: _elegirFecha,
                  validator: (_) => validarFechaNacimiento(_fechaNacimiento) ?? _errorFecha,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  key: const Key('campoEmail'),
                  controller: _email,
                  decoration: const InputDecoration(labelText: 'Correo'),
                  keyboardType: TextInputType.emailAddress,
                  autocorrect: false,
                  textInputAction: TextInputAction.next,
                  validator: (v) => validarEmail(v) ?? _errorEmail,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  key: const Key('campoPassword'),
                  controller: _password,
                  decoration: InputDecoration(
                    labelText: 'Contraseña',
                    suffixIcon: IconButton(
                      icon: Icon(_verPassword ? Icons.visibility_off_outlined : Icons.visibility_outlined),
                      tooltip: _verPassword ? 'Ocultar contraseña' : 'Mostrar contraseña',
                      onPressed: () => setState(() => _verPassword = !_verPassword),
                    ),
                  ),
                  obscureText: !_verPassword,
                  textInputAction: TextInputAction.next,
                  validator: validarPassword,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  key: const Key('campoTelefono'),
                  controller: _telefono,
                  decoration: const InputDecoration(labelText: 'Teléfono (opcional)'),
                  keyboardType: TextInputType.phone,
                  validator: validarTelefono,
                ),
                const SizedBox(height: 24),
                FilledButton(
                  key: const Key('botonRegistrar'),
                  onPressed: enviando ? null : _enviar,
                  child: enviando
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Text('Registrarme'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
