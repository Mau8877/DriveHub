import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../models/editar_perfil_request.dart';
import '../models/perfil.dart';
import '../providers/perfil_provider.dart';
import '../validators/perfil_validators.dart';
import '../widgets/foto_perfil_seccion.dart';

const _roles = {
  'ESTUDIANTE': 'Estudiante',
  'INSTRUCTOR': 'Instructor',
  'ADMIN_ESCUELA': 'Administrador de escuela',
  'SUPER_ADMIN': 'Super admin',
};

/// Perfil de la cuenta propia (CU06): datos de solo lectura, foto, edición de los datos
/// personales y accesos a cambiar la contraseña y el correo.
///
/// * Es el cuerpo de la pestaña "Perfil" del `UsuarioShell`.
class PerfilScreen extends ConsumerWidget {
  const PerfilScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final perfil = ref.watch(perfilProvider);
    return perfil.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (_, _) => Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('No pudimos cargar tu perfil'),
            const SizedBox(height: 8),
            OutlinedButton(
              onPressed: () => ref.invalidate(perfilProvider),
              child: const Text('Reintentar'),
            ),
          ],
        ),
      ),
      data: (datos) => _PerfilContenido(perfil: datos),
    );
  }
}

class _PerfilContenido extends ConsumerStatefulWidget {
  final Perfil perfil;

  const _PerfilContenido({required this.perfil});

  @override
  ConsumerState<_PerfilContenido> createState() => _PerfilContenidoState();
}

class _PerfilContenidoState extends ConsumerState<_PerfilContenido> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nombres;
  late final TextEditingController _apellidos;
  late final TextEditingController _telefono;
  late DateTime _fecha;
  bool _guardando = false;
  String? _errorFecha;

  @override
  void initState() {
    super.initState();
    _nombres = TextEditingController(text: widget.perfil.nombres)
      ..addListener(_cambio);
    _apellidos = TextEditingController(text: widget.perfil.apellidos)
      ..addListener(_cambio);
    _telefono = TextEditingController(text: widget.perfil.telefono ?? '')
      ..addListener(_cambio);
    _fecha = widget.perfil.fechaNacimiento;
  }

  @override
  void dispose() {
    _nombres.dispose();
    _apellidos.dispose();
    _telefono.dispose();
    super.dispose();
  }

  void _cambio() => setState(() => _errorFecha = null);

  /// Solo los campos que difieren del perfil cargado; un teléfono vaciado se manda como `null`.
  EditarPerfilRequest _cambios() {
    final p = widget.perfil;
    final telefono = _telefono.text.trim();
    return EditarPerfilRequest(
      nombres: _nombres.text.trim() != p.nombres ? _nombres.text.trim() : null,
      apellidos: _apellidos.text.trim() != p.apellidos
          ? _apellidos.text.trim()
          : null,
      telefono: telefono.isNotEmpty && telefono != (p.telefono ?? '')
          ? telefono
          : null,
      borrarTelefono: telefono.isEmpty && (p.telefono ?? '').isNotEmpty,
      fechaNacimiento: _fecha != p.fechaNacimiento ? _fecha : null,
    );
  }

  Future<void> _elegirFecha() async {
    final hoy = DateTime.now();
    final elegida = await showDatePicker(
      context: context,
      initialDate: _fecha,
      firstDate: DateTime(1900),
      lastDate: DateTime(hoy.year, hoy.month, hoy.day),
    );
    if (elegida != null) {
      setState(() {
        _fecha = elegida;
        _errorFecha = null;
      });
    }
  }

  Future<void> _guardar() async {
    final mensajes = ScaffoldMessenger.of(context);
    final errorDeFecha = _fecha != widget.perfil.fechaNacimiento
        ? validarFechaNacimiento(_fecha)
        : null;
    if (!_formKey.currentState!.validate() || errorDeFecha != null) {
      setState(() => _errorFecha = errorDeFecha);
      return;
    }
    final cambios = _cambios();
    if (cambios.estaVacio) return;

    setState(() => _guardando = true);
    try {
      await ref.read(perfilProvider.notifier).editar(cambios);
      mensajes.showSnackBar(const SnackBar(content: Text('Datos guardados')));
    } on ApiException catch (e) {
      if (e.code == 'ERR_IDE_02') {
        if (mounted) setState(() => _errorFecha = mensajeDeCodigo(e.code));
      } else {
        mensajes.showSnackBar(SnackBar(content: Text(mensajeDeCodigo(e.code))));
      }
    } catch (_) {
      mensajes.showSnackBar(const SnackBar(content: Text(mensajeGenerico)));
    } finally {
      if (mounted) setState(() => _guardando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final p = widget.perfil;
    final hayCambios = !_cambios().estaVacio;
    final tema = Theme.of(context).textTheme;

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        FotoPerfilSeccion(perfil: p),
        const SizedBox(height: 24),
        Text('Tu cuenta', style: tema.titleMedium),
        const SizedBox(height: 8),
        _FilaSoloLectura(
          etiqueta: 'Correo',
          valor: p.email,
          insignia: p.correoVerificado ? 'Verificado' : 'Sin verificar',
        ),
        _FilaSoloLectura(
          etiqueta: 'Rol',
          valor: p.roles.map((r) => _roles[r] ?? r).join(', '),
        ),
        if (p.escuelaNombre != null)
          _FilaSoloLectura(etiqueta: 'Escuela', valor: p.escuelaNombre!),
        const SizedBox(height: 24),
        Text('Datos personales', style: tema.titleMedium),
        const SizedBox(height: 8),
        Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextFormField(
                controller: _nombres,
                decoration: const InputDecoration(labelText: 'Nombres'),
                textCapitalization: TextCapitalization.words,
                validator: validarNombres,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _apellidos,
                decoration: const InputDecoration(labelText: 'Apellidos'),
                textCapitalization: TextCapitalization.words,
                validator: validarApellidos,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _telefono,
                decoration: const InputDecoration(
                  labelText: 'Teléfono (opcional)',
                ),
                keyboardType: TextInputType.phone,
                validator: validarTelefono,
              ),
              const SizedBox(height: 12),
              InkWell(
                onTap: _guardando ? null : _elegirFecha,
                child: InputDecorator(
                  decoration: InputDecoration(
                    labelText: 'Fecha de nacimiento',
                    errorText: _errorFecha,
                  ),
                  child: Text(_comoFecha(_fecha)),
                ),
              ),
              const SizedBox(height: 16),
              FilledButton(
                onPressed: (_guardando || !hayCambios) ? null : _guardar,
                child: _guardando
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text('Guardar cambios'),
              ),
            ],
          ),
        ),
        const SizedBox(height: 24),
        OutlinedButton(
          onPressed: () => context.push('/perfil/contrasena'),
          child: const Text('Cambiar contraseña'),
        ),
        const SizedBox(height: 8),
        OutlinedButton(
          onPressed: () => context.push('/perfil/correo'),
          child: const Text('Cambiar correo'),
        ),
      ],
    );
  }

  static String _comoFecha(DateTime fecha) {
    final dia = fecha.day.toString().padLeft(2, '0');
    final mes = fecha.month.toString().padLeft(2, '0');
    return '$dia/$mes/${fecha.year}';
  }
}

class _FilaSoloLectura extends StatelessWidget {
  final String etiqueta;
  final String valor;
  final String? insignia;

  const _FilaSoloLectura({
    required this.etiqueta,
    required this.valor,
    this.insignia,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 80,
            child: Text(etiqueta, style: Theme.of(context).textTheme.bodySmall),
          ),
          Expanded(child: Text(valor)),
          if (insignia != null)
            Padding(
              padding: const EdgeInsets.only(left: 8),
              child: Text(
                insignia!,
                style: Theme.of(context).textTheme.labelSmall,
              ),
            ),
        ],
      ),
    );
  }
}
