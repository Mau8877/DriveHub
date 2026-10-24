import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../../../shared/widgets/medidor_password.dart';
import '../../../shared/widgets/selector_ubicacion.dart';
import '../models/registro_estudiante_request.dart';
import '../providers/registro_provider.dart';
import '../validators/registro_validators.dart';

/// Registro del estudiante (CU01). Pantalla pública: sin sesión y sin
/// auto-login, al terminar vuelve al login.
///
/// * La escuela se escribe como "código de escuela" (su slug): la app no
/// * tiene enlaces por escuela. El backend responde ERR_IDE_03 si no existe.
/// * CU08a: pide además sexo, CI y la ubicación en un mapa. El mapa solo elige el punto; la
/// * dirección la calcula el servidor.
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
  final _ci = TextEditingController();

  DateTime? _fechaNacimiento;
  String? _sexo;
  double? _latitud;
  double? _longitud;
  bool _verPassword = false;

  // * Errores que devuelve el backend, mostrados bajo su campo hasta el próximo envío
  String? _errorEmail;
  String? _errorFecha;
  String? _errorEscuela;
  String? _errorCi;

  @override
  void initState() {
    super.initState();
    // * El medidor de fuerza se repinta con cada tecla de la contraseña
    _password.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    for (final c in [_nombres, _apellidos, _fecha, _email, _password, _telefono, _escuela, _ci]) {
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
      _errorCi = null;
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
            ci: _ci.text.trim(),
            sexo: _sexo!,
            latitud: _latitud!,
            longitud: _longitud!,
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
        case ErrorRegistro.ciEnUso:
          _errorCi = 'Este CI ya está registrado';
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

    final tema = Theme.of(context);

    return CockpitScaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.chevron_left),
          tooltip: 'Volver',
          onPressed: () => context.go('/login'),
        ),
        title: const Text('Crear cuenta'),
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
                  'Regístrate como estudiante con el código que te dio tu autoescuela.',
                  style: tema.textTheme.bodyMedium?.copyWith(
                    color: tema.colorScheme.onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 20),
                TextFormField(
                  key: const Key('campoEscuela'),
                  controller: _escuela,
                  decoration: const InputDecoration(
                    labelText: 'Código de escuela',
                    prefixIcon: Icon(Icons.school_outlined),
                    helperText: 'Es el que va en el enlace de registro de tu escuela.',
                  ),
                  autocorrect: false,
                  textInputAction: TextInputAction.next,
                  validator: (v) => validarCodigoEscuela(v) ?? _errorEscuela,
                ),
                const SizedBox(height: 14),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: TextFormField(
                        key: const Key('campoNombres'),
                        controller: _nombres,
                        decoration: const InputDecoration(labelText: 'Nombres'),
                        textCapitalization: TextCapitalization.words,
                        textInputAction: TextInputAction.next,
                        validator: validarNombres,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: TextFormField(
                        key: const Key('campoApellidos'),
                        controller: _apellidos,
                        decoration: const InputDecoration(labelText: 'Apellidos'),
                        textCapitalization: TextCapitalization.words,
                        textInputAction: TextInputAction.next,
                        validator: validarApellidos,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                TextFormField(
                  key: const Key('campoFecha'),
                  controller: _fecha,
                  readOnly: true,
                  decoration: const InputDecoration(
                    labelText: 'Fecha de nacimiento',
                    prefixIcon: Icon(Icons.calendar_today_outlined),
                  ),
                  onTap: _elegirFecha,
                  validator: (_) => validarFechaNacimiento(_fechaNacimiento) ?? _errorFecha,
                ),
                const SizedBox(height: 14),
                DropdownButtonFormField<String>(
                  key: const Key('campoSexo'),
                  initialValue: _sexo,
                  decoration: const InputDecoration(
                    labelText: 'Sexo',
                    prefixIcon: Icon(Icons.person_outline),
                  ),
                  items: const [
                    DropdownMenuItem(value: 'MASCULINO', child: Text('Masculino')),
                    DropdownMenuItem(value: 'FEMENINO', child: Text('Femenino')),
                  ],
                  onChanged: (valor) => setState(() => _sexo = valor),
                  validator: validarSexo,
                ),
                const SizedBox(height: 14),
                TextFormField(
                  key: const Key('campoCi'),
                  controller: _ci,
                  decoration: const InputDecoration(
                    labelText: 'CI',
                    prefixIcon: Icon(Icons.badge_outlined),
                  ),
                  autocorrect: false,
                  textCapitalization: TextCapitalization.characters,
                  textInputAction: TextInputAction.next,
                  validator: (v) => validarCi(v) ?? _errorCi,
                ),
                const SizedBox(height: 14),
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
                  validator: (v) => validarEmail(v) ?? _errorEmail,
                ),
                const SizedBox(height: 14),
                TextFormField(
                  key: const Key('campoPassword'),
                  controller: _password,
                  decoration: InputDecoration(
                    labelText: 'Contraseña',
                    prefixIcon: const Icon(Icons.lock_outline),
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
                const SizedBox(height: 8),
                MedidorPassword(fuerza: fuerzaDePassword(_password.text)),
                const SizedBox(height: 4),
                Text('Entre 9 y 64 caracteres.', style: tema.textTheme.bodySmall),
                const SizedBox(height: 10),
                TextFormField(
                  key: const Key('campoTelefono'),
                  controller: _telefono,
                  decoration: const InputDecoration(
                    labelText: 'Teléfono (opcional)',
                    prefixIcon: Icon(Icons.phone_outlined),
                  ),
                  keyboardType: TextInputType.phone,
                  validator: validarTelefono,
                ),
                const SizedBox(height: 20),
                Text('Tu ubicación', style: tema.textTheme.titleSmall),
                const SizedBox(height: 8),
                FormField<bool>(
                  key: const Key('campoUbicacion'),
                  validator: (_) => validarUbicacion(_latitud, _longitud),
                  builder: (estado) => Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      SelectorUbicacion(
                        latitud: _latitud,
                        longitud: _longitud,
                        tileProvider: ref.watch(proveedorTeselasProvider),
                        onCambio: (latitud, longitud) {
                          setState(() {
                            _latitud = latitud;
                            _longitud = longitud;
                          });
                          estado.didChange(true);
                        },
                      ),
                      if (estado.hasError)
                        Padding(
                          padding: const EdgeInsets.only(top: 6),
                          child: Text(
                            estado.errorText!,
                            style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.error),
                          ),
                        ),
                    ],
                  ),
                ),
                const SizedBox(height: 24),
                FilledButton(
                  key: const Key('botonRegistrar'),
                  onPressed: enviando ? null : _enviar,
                  child: enviando ? const IndicadorBoton() : const Text('Registrarme'),
                ),
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      '¿Ya tienes cuenta?',
                      style: tema.textTheme.bodyMedium?.copyWith(
                        color: tema.colorScheme.onSurfaceVariant,
                      ),
                    ),
                    TextButton(
                      onPressed: () => context.go('/login'),
                      child: const Text('Inicia sesión'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
