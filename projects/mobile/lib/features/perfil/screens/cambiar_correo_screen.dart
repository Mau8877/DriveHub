import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../models/cambiar_correo_request.dart';
import '../providers/perfil_provider.dart';
import '../validators/perfil_validators.dart';

/// Cambiar el correo (CU06). Pide la contraseña actual y el correo nuevo; el enlace llega al
/// correo NUEVO y el de la cuenta no cambia hasta abrirlo (se abre en el navegador del teléfono).
///
/// * Con una solicitud pendiente, al volver a la app (`resumed`) se vuelve a pedir el perfil: si
/// * el correo cambió, se muestra y se quita el estado pendiente (CA18).
/// ! La contraseña nunca se guarda ni se escribe en la consola.
class CambiarCorreoScreen extends ConsumerStatefulWidget {
  const CambiarCorreoScreen({super.key});

  @override
  ConsumerState<CambiarCorreoScreen> createState() =>
      _CambiarCorreoScreenState();
}

class _CambiarCorreoScreenState extends ConsumerState<CambiarCorreoScreen>
    with WidgetsBindingObserver {
  final _formKey = GlobalKey<FormState>();
  final _password = TextEditingController();
  final _correo = TextEditingController();
  bool _ver = false;
  bool _enviando = false;
  String? _errorPassword;
  String? _errorCorreo;

  /// El correo nuevo al que se envió el enlace, mientras no se haya abierto.
  String? _pendiente;

  /// El correo de la cuenta cuando se pidió el cambio, para saber si cambió al volver.
  String? _correoAlPedir;
  String? _aviso;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _password.dispose();
    _correo.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState estado) {
    if (estado == AppLifecycleState.resumed && _pendiente != null) {
      unawaited(_revisarCorreo());
    }
  }

  Future<void> _revisarCorreo() async {
    await ref.read(perfilProvider.notifier).recargar();
    final actual = ref.read(perfilProvider).valueOrNull?.email;
    final anterior = _correoAlPedir;
    if (!mounted || actual == null || anterior == null) return;
    if (correoCambio(anterior, actual)) {
      setState(() {
        _pendiente = null;
        _correoAlPedir = null;
        _aviso = 'Tu correo ahora es $actual';
      });
    }
  }

  Future<void> _enviar() async {
    setState(() {
      _errorPassword = null;
      _errorCorreo = null;
      _aviso = null;
    });
    if (!_formKey.currentState!.validate()) return;

    final mensajes = ScaffoldMessenger.of(context);
    final correoNuevo = _correo.text.trim();
    final correoActual = ref.read(perfilProvider).valueOrNull?.email;
    setState(() => _enviando = true);
    try {
      await ref
          .read(perfilProvider.notifier)
          .solicitarCambioCorreo(
            CambiarCorreoRequest(
              passwordActual: _password.text,
              correoNuevo: correoNuevo,
            ),
          );
      if (!mounted) return;
      setState(() {
        _pendiente = correoNuevo.toLowerCase();
        _correoAlPedir = correoActual;
        _password.clear();
        _correo.clear();
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.code == 'ERR_IDE_14') {
        setState(() => _errorPassword = mensajeDeCodigo(e.code));
      } else if (e.code == 'ERR_IDE_01' || e.code == 'ERR_IDE_18') {
        setState(() => _errorCorreo = mensajeDeCodigo(e.code));
      } else {
        mensajes.showSnackBar(
          SnackBar(
            content: Text(
              e.code == 'ERR_SYS_01'
                  ? e.message
                  : 'No pudimos enviar el correo. Intenta más tarde',
            ),
          ),
        );
      }
    } catch (_) {
      mensajes.showSnackBar(
        const SnackBar(
          content: Text('No pudimos enviar el correo. Intenta más tarde'),
        ),
      );
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final correoActual = ref.watch(perfilProvider).valueOrNull?.email ?? '';

    return CockpitScaffold(
      appBar: AppBar(title: const Text('Cambiar correo')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 8, 24, 32),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'Tu correo actual es $correoActual',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
              if (_aviso != null) ...[
                const SizedBox(height: 12),
                AvisoCockpit(texto: _aviso!, tono: TonoAviso.exito),
              ],
              const SizedBox(height: 16),
              if (_pendiente != null) ...[
                AvisoCockpit(
                  texto:
                      'Te enviamos un correo a $_pendiente. Tu correo no cambia hasta que abras el '
                      'enlace (vale 24 horas).',
                ),
                const SizedBox(height: 12),
                OutlinedButton(
                  onPressed: () => setState(() => _pendiente = null),
                  child: const Text('Pedir otro enlace'),
                ),
              ] else
                Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      TextFormField(
                        controller: _password,
                        obscureText: !_ver,
                        decoration: InputDecoration(
                          labelText: 'Contraseña actual',
                          prefixIcon: const Icon(Icons.lock_outline),
                          errorText: _errorPassword,
                          suffixIcon: IconButton(
                            icon: Icon(
                              _ver
                                  ? Icons.visibility_off_outlined
                                  : Icons.visibility_outlined,
                            ),
                            tooltip: _ver
                                ? 'Ocultar contraseña'
                                : 'Mostrar contraseña',
                            onPressed: () => setState(() => _ver = !_ver),
                          ),
                        ),
                        validator: validarContrasenaActual,
                        onChanged: (_) => setState(() => _errorPassword = null),
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _correo,
                        keyboardType: TextInputType.emailAddress,
                        decoration: InputDecoration(
                          labelText: 'Correo nuevo',
                          prefixIcon: const Icon(Icons.mail_outline),
                          errorText: _errorCorreo,
                        ),
                        validator: validarCorreoNuevo,
                        onChanged: (_) => setState(() => _errorCorreo = null),
                      ),
                      const SizedBox(height: 24),
                      FilledButton(
                        onPressed: _enviando ? null : _enviar,
                        child: _enviando
                            ? const IndicadorBoton()
                            : const Text('Enviar enlace'),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
