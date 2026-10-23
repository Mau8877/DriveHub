import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../../../shared/widgets/medidor_password.dart';
import '../models/cambiar_contrasena_request.dart';
import '../providers/perfil_provider.dart';
import '../validators/perfil_validators.dart';

/// Cambiar la contraseña con sesión (CU06). La actual es obligatoria y la nueva sigue la regla
/// del registro. Al terminar se cierran las sesiones de los demás dispositivos (la web), pero la
/// de este teléfono no se cierra ni se renueva.
///
/// ! Las contraseñas nunca se guardan ni se escriben en la consola.
class CambiarContrasenaScreen extends ConsumerStatefulWidget {
  const CambiarContrasenaScreen({super.key});

  @override
  ConsumerState<CambiarContrasenaScreen> createState() =>
      _CambiarContrasenaScreenState();
}

class _CambiarContrasenaScreenState
    extends ConsumerState<CambiarContrasenaScreen> {
  final _formKey = GlobalKey<FormState>();
  final _actual = TextEditingController();
  final _nueva = TextEditingController();
  final _confirmacion = TextEditingController();
  bool _ver = false;
  bool _enviando = false;
  String? _errorActual;
  String? _errorNueva;

  @override
  void dispose() {
    _actual.dispose();
    _nueva.dispose();
    _confirmacion.dispose();
    super.dispose();
  }

  Future<void> _enviar() async {
    setState(() {
      _errorActual = null;
      _errorNueva = null;
    });
    if (!_formKey.currentState!.validate()) return;

    final mensajes = ScaffoldMessenger.of(context);
    final router = GoRouter.of(context);
    setState(() => _enviando = true);
    try {
      await ref
          .read(perfilProvider.notifier)
          .cambiarContrasena(
            CambiarContrasenaRequest(
              passwordActual: _actual.text,
              passwordNueva: _nueva.text,
            ),
          );
      mensajes.showSnackBar(
        const SnackBar(
          content: Text('Contraseña actualizada. Cerramos tus otras sesiones.'),
        ),
      );
      if (router.canPop()) router.pop();
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.code == 'ERR_IDE_14') {
        setState(() => _errorActual = mensajeDeCodigo(e.code));
      } else if (e.code == 'ERR_IDE_15') {
        setState(() => _errorNueva = mensajeDeCodigo(e.code));
      } else {
        mensajes.showSnackBar(
          SnackBar(
            content: Text(e.code == 'ERR_SYS_01' ? e.message : mensajeGenerico),
          ),
        );
      }
    } catch (_) {
      mensajes.showSnackBar(const SnackBar(content: Text(mensajeGenerico)));
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final ojo = IconButton(
      icon: Icon(
        _ver ? Icons.visibility_off_outlined : Icons.visibility_outlined,
      ),
      tooltip: _ver ? 'Ocultar contraseñas' : 'Mostrar contraseñas',
      onPressed: () => setState(() => _ver = !_ver),
    );

    return CockpitScaffold(
      appBar: AppBar(title: const Text('Cambiar contraseña')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 8, 24, 32),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextFormField(
                  controller: _actual,
                  obscureText: !_ver,
                  autofillHints: const [AutofillHints.password],
                  decoration: InputDecoration(
                    labelText: 'Contraseña actual',
                    prefixIcon: const Icon(Icons.lock_outline),
                    suffixIcon: ojo,
                    errorText: _errorActual,
                  ),
                  validator: validarContrasenaActual,
                  onChanged: (_) => setState(() => _errorActual = null),
                ),
                const SizedBox(height: 14),
                TextFormField(
                  controller: _nueva,
                  obscureText: !_ver,
                  autofillHints: const [AutofillHints.newPassword],
                  decoration: InputDecoration(
                    labelText: 'Contraseña nueva',
                    prefixIcon: const Icon(Icons.lock_reset_outlined),
                    errorText: _errorNueva,
                  ),
                  validator: (valor) =>
                      contrasenaNueva(valor) ??
                      distintaDeLaActual(valor, _actual.text),
                  onChanged: (_) => setState(() => _errorNueva = null),
                ),
                const SizedBox(height: 8),
                MedidorPassword(fuerza: fuerzaDePassword(_nueva.text)),
                const SizedBox(height: 14),
                TextFormField(
                  controller: _confirmacion,
                  obscureText: !_ver,
                  autofillHints: const [AutofillHints.newPassword],
                  decoration: const InputDecoration(
                    labelText: 'Confirma la contraseña nueva',
                    prefixIcon: Icon(Icons.lock_reset_outlined),
                  ),
                  validator: (valor) => confirmacion(valor, _nueva.text),
                ),
                const SizedBox(height: 24),
                FilledButton(
                  onPressed: _enviando ? null : _enviar,
                  child: _enviando
                      ? const IndicadorBoton()
                      : const Text('Cambiar contraseña'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
