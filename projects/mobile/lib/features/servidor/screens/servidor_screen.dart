import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../../../shared/widgets/marca_drivehub.dart';
import '../providers/servidor_provider.dart';
import '../services/probar_servidor.dart';
import '../validators/servidor_validators.dart';

/// Con qué servidor habla la app (`/servidor`).
///
/// * **La primera vez** que se abre la app es lo único que se ve: el router no deja pasar a nada más hasta que haya un
///   servidor. Después la app arranca directo en el login y esta pantalla solo se vuelve a ver si la persona la abre
///   desde «Cambiar servidor».
/// * «Probar conexión» es informativa: avisa si encontró un servidor de DriveHub, pero no obliga. Guardar solo pide que
///   la dirección esté bien escrita, así se puede dejar configurada la app sin conexión.
/// * Se escribe solo el servidor (`http://192.168.1.10:8080`); la ruta `/api/v1` la agrega la app.
class ServidorScreen extends ConsumerStatefulWidget {
  const ServidorScreen({super.key});

  @override
  ConsumerState<ServidorScreen> createState() => _ServidorScreenState();
}

class _ServidorScreenState extends ConsumerState<ServidorScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _direccion;

  bool _probando = false;
  bool _guardando = false;
  ResultadoPrueba? _resultado;

  @override
  void initState() {
    super.initState();
    // * Se sugiere lo que ya hay: lo que eligió antes o, la primera vez, lo que trae el `.env`
    final url = ref.read(servidorProvider).url ?? ApiClient.baseUrlActual;
    _direccion = TextEditingController(text: direccionParaMostrar(url));
  }

  @override
  void dispose() {
    _direccion.dispose();
    super.dispose();
  }

  Future<void> _probar() async {
    if (!_formKey.currentState!.validate()) return;
    final url = normalizarDireccionServidor(_direccion.text)!;
    setState(() {
      _probando = true;
      _resultado = null;
    });
    final resultado = await ref.read(probadorServidorProvider)(url);
    if (!mounted) return;
    setState(() {
      _probando = false;
      _resultado = resultado;
    });
  }

  Future<void> _guardar() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _guardando = true);
    try {
      await ref.read(servidorProvider.notifier).guardar(_direccion.text);
      // * Con el servidor elegido sigue lo de siempre: el login (el redirect global decide si ya hay sesión)
      if (mounted) context.go('/login');
    } catch (_) {
      if (!mounted) return;
      setState(() => _guardando = false);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('No pudimos guardar la dirección. Inténtalo de nuevo.')));
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final primeraVez = !ref.watch(servidorProvider).configurado;

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
                  const Align(alignment: Alignment.centerLeft, child: LogoDriveHub(tamano: 34)),
                  const SizedBox(height: 28),
                  Text(
                    primeraVez ? 'Conecta tu app' : 'Servidor',
                    style: tema.textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w800, letterSpacing: -1),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    primeraVez
                        ? 'Escribe la dirección del servidor de tu autoescuela. Solo te la pedimos esta vez.'
                        : 'Cambia el servidor con el que habla la app.',
                    style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant),
                  ),
                  const SizedBox(height: 24),
                  TextFormField(
                    key: const Key('campoServidor'),
                    controller: _direccion,
                    decoration: const InputDecoration(
                      labelText: 'Dirección del servidor',
                      hintText: 'http://192.168.1.10:8080',
                      helperText: 'En el emulador de Android, 10.0.2.2 es tu computadora.',
                      helperMaxLines: 2,
                      prefixIcon: Icon(Icons.dns_outlined),
                    ),
                    keyboardType: TextInputType.url,
                    autocorrect: false,
                    enableSuggestions: false,
                    textInputAction: TextInputAction.done,
                    onChanged: (_) {
                      if (_resultado != null) setState(() => _resultado = null);
                    },
                    onFieldSubmitted: (_) => _guardar(),
                    validator: validarDireccionServidor,
                  ),
                  const SizedBox(height: 14),
                  if (_resultado != null) ...[_avisoDelResultado(_resultado!), const SizedBox(height: 14)],
                  OutlinedButton.icon(
                    key: const Key('botonProbarServidor'),
                    onPressed: (_probando || _guardando) ? null : _probar,
                    icon: _probando
                        ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.wifi_find_outlined, size: 18),
                    label: const Text('Probar conexión'),
                  ),
                  const SizedBox(height: 10),
                  FilledButton(
                    key: const Key('botonGuardarServidor'),
                    onPressed: (_probando || _guardando) ? null : _guardar,
                    child: _guardando
                        ? const IndicadorBoton()
                        : const Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [Text('Guardar y continuar'), SizedBox(width: 8), Icon(Icons.arrow_forward, size: 18)],
                          ),
                  ),
                  if (!primeraVez) ...[
                    const SizedBox(height: 4),
                    TextButton(
                      key: const Key('botonCancelarServidor'),
                      onPressed: _guardando ? null : () => context.go('/login'),
                      child: const Text('Cancelar'),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _avisoDelResultado(ResultadoPrueba resultado) => switch (resultado) {
    ResultadoPrueba.servidorDriveHub => const AvisoCockpit(
      key: Key('resultadoServidor'),
      texto: 'Servidor de DriveHub encontrado.',
      tono: TonoAviso.exito,
    ),
    ResultadoPrueba.respondeOtraCosa => const AvisoCockpit(
      key: Key('resultadoServidor'),
      texto: 'Respondió, pero no parece un servidor de DriveHub. Revisa la dirección y el puerto.',
      tono: TonoAviso.advertencia,
    ),
    ResultadoPrueba.sinRespuesta => const AvisoCockpit(
      key: Key('resultadoServidor'),
      texto:
          'No hay respuesta. Revisa la dirección y que el teléfono esté en la misma red que el servidor. '
          'Puedes guardarla igual y conectarte después.',
      tono: TonoAviso.error,
    ),
  };
}
