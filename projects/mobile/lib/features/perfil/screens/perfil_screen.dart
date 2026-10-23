import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../../../core/theme/tema_provider.dart';
import '../../../shared/widgets/fila_accion.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../../../shared/widgets/insignia.dart';
import '../../../shared/widgets/selector_ubicacion.dart';
import '../../auth/providers/sesion_provider.dart';
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

/// Perfil de la cuenta propia (CU06, CU08a): datos de solo lectura, foto, edición de los datos
/// personales (con CI, sexo y ubicación en el mapa), accesos a cambiar la contraseña y el correo,
/// apariencia y cerrar sesión.
///
/// * La categoría del estudiante se muestra pero no se edita: la asigna la escuela.
/// * El mapa solo elige el punto; la dirección la recalcula el servidor al guardar.
/// * Es el cuerpo de la pestaña "Perfil" del `UsuarioShell`.
class PerfilScreen extends ConsumerWidget {
  const PerfilScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final perfil = ref.watch(perfilProvider);
    return perfil.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (_, _) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('No pudimos cargar tu perfil'),
              const SizedBox(height: 12),
              OutlinedButton(
                onPressed: () => ref.invalidate(perfilProvider),
                child: const Text('Reintentar'),
              ),
            ],
          ),
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
  late final TextEditingController _ci;
  late DateTime _fecha;
  late String _sexo;
  double? _latitud;
  double? _longitud;
  bool _guardando = false;
  String? _errorFecha;
  String? _errorCi;

  @override
  void initState() {
    super.initState();
    _nombres = TextEditingController(text: widget.perfil.nombres)
      ..addListener(_cambio);
    _apellidos = TextEditingController(text: widget.perfil.apellidos)
      ..addListener(_cambio);
    _telefono = TextEditingController(text: widget.perfil.telefono ?? '')
      ..addListener(_cambio);
    _ci = TextEditingController(text: widget.perfil.ci)..addListener(_cambio);
    _fecha = widget.perfil.fechaNacimiento;
    _sexo = widget.perfil.sexo;
    _latitud = widget.perfil.latitud;
    _longitud = widget.perfil.longitud;
  }

  @override
  void dispose() {
    _nombres.dispose();
    _apellidos.dispose();
    _telefono.dispose();
    _ci.dispose();
    super.dispose();
  }

  void _cambio() => setState(() {
    _errorFecha = null;
    _errorCi = null;
  });

  /// El Super admin no tiene ubicación: solo se muestra el mapa a quien la tiene o puede tenerla.
  bool _conUbicacion(Perfil p) => p.latitud != null || !p.roles.contains('SUPER_ADMIN');

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
      ci: _ci.text.trim().toUpperCase() != p.ci ? _ci.text.trim() : null,
      sexo: _sexo != p.sexo ? _sexo : null,
      // * La ubicación va como par y solo si el punto cambió; si no, el servidor no recalcula la dirección
      latitud: _ubicacionCambio(p) ? _latitud : null,
      longitud: _ubicacionCambio(p) ? _longitud : null,
    );
  }

  bool _ubicacionCambio(Perfil p) =>
      _latitud != null && _longitud != null && (_latitud != p.latitud || _longitud != p.longitud);

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
      } else if (e.code == 'ERR_IDE_19') {
        if (mounted) {
          setState(() => _errorCi = mensajeDeCodigo(e.code));
          // * El mensaje del servidor sale por el validator del campo: hay que volver a validar
          _formKey.currentState?.validate();
        }
      } else {
        mensajes.showSnackBar(SnackBar(content: Text(mensajeDeCodigo(e.code))));
      }
    } catch (_) {
      mensajes.showSnackBar(const SnackBar(content: Text(mensajeGenerico)));
    } finally {
      if (mounted) setState(() => _guardando = false);
    }
  }

  Future<void> _cerrarSesion() async {
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('¿Cerrar sesión?'),
        content: const Text('Tendrás que iniciar sesión de nuevo para entrar.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('No'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Sí, cerrar'),
          ),
        ],
      ),
    );
    if (confirmado != true || !mounted) return;
    await ref.read(sesionProvider.notifier).cerrarSesion();
  }

  @override
  Widget build(BuildContext context) {
    final p = widget.perfil;
    final hayCambios = !_cambios().estaVacio;
    final tema = Theme.of(context);
    final apariencia = ref.watch(temaProvider);

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
      children: [
        FotoPerfilSeccion(perfil: p),
        const SizedBox(height: 20),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              children: [
                _FilaSoloLectura(
                  etiqueta: 'Correo',
                  valor: p.email,
                  insignia: Insignia(
                    p.correoVerificado ? 'Verificado' : 'Sin verificar',
                    tono: p.correoVerificado
                        ? TonoInsignia.exito
                        : TonoInsignia.neutro,
                  ),
                ),
                const SizedBox(height: 14),
                _FilaSoloLectura(
                  etiqueta: 'Rol',
                  valor: p.roles.map((r) => _roles[r] ?? r).join(', '),
                ),
                if (p.escuelaNombre != null) ...[
                  const SizedBox(height: 14),
                  _FilaSoloLectura(
                    etiqueta: 'Escuela',
                    valor: p.escuelaNombre!,
                  ),
                ],
                if (p.categoriaSigla != null) ...[
                  const SizedBox(height: 14),
                  _FilaSoloLectura(
                    key: const Key('filaCategoria'),
                    etiqueta: 'Categoría',
                    valor: p.categoriaSigla!,
                    ayuda: 'La asigna tu escuela',
                  ),
                ],
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text('Datos personales', style: tema.textTheme.titleMedium),
                  const SizedBox(height: 14),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: TextFormField(
                          controller: _nombres,
                          decoration: const InputDecoration(
                            labelText: 'Nombres',
                          ),
                          textCapitalization: TextCapitalization.words,
                          validator: validarNombres,
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextFormField(
                          controller: _apellidos,
                          decoration: const InputDecoration(
                            labelText: 'Apellidos',
                          ),
                          textCapitalization: TextCapitalization.words,
                          validator: validarApellidos,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    controller: _telefono,
                    decoration: const InputDecoration(
                      labelText: 'Teléfono (opcional)',
                    ),
                    keyboardType: TextInputType.phone,
                    validator: validarTelefono,
                  ),
                  const SizedBox(height: 14),
                  InkWell(
                    borderRadius: BorderRadius.circular(15),
                    onTap: _guardando ? null : _elegirFecha,
                    child: InputDecorator(
                      decoration: InputDecoration(
                        labelText: 'Fecha de nacimiento',
                        errorText: _errorFecha,
                        suffixIcon: const Icon(Icons.calendar_today_outlined),
                      ),
                      child: Text(_comoFecha(_fecha)),
                    ),
                  ),
                  const SizedBox(height: 14),
                  DropdownButtonFormField<String>(
                    key: const Key('campoSexo'),
                    initialValue: _sexo,
                    decoration: const InputDecoration(labelText: 'Sexo'),
                    items: const [
                      DropdownMenuItem(
                        value: 'MASCULINO',
                        child: Text('Masculino'),
                      ),
                      DropdownMenuItem(
                        value: 'FEMENINO',
                        child: Text('Femenino'),
                      ),
                    ],
                    onChanged: _guardando
                        ? null
                        : (valor) => setState(() => _sexo = valor ?? _sexo),
                    validator: validarSexo,
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    key: const Key('campoCi'),
                    controller: _ci,
                    decoration: const InputDecoration(labelText: 'CI'),
                    autocorrect: false,
                    textCapitalization: TextCapitalization.characters,
                    validator: (v) => validarCi(v) ?? _errorCi,
                  ),
                  if (_conUbicacion(p)) ...[
                    const SizedBox(height: 18),
                    Text('Ubicación', style: tema.textTheme.titleSmall),
                    const SizedBox(height: 8),
                    SelectorUbicacion(
                      latitud: _latitud,
                      longitud: _longitud,
                      direccion: p.direccion,
                      habilitado: !_guardando,
                      tileProvider: ref.watch(proveedorTeselasProvider),
                      onCambio: (latitud, longitud) => setState(() {
                        _latitud = latitud;
                        _longitud = longitud;
                      }),
                    ),
                    if (p.latitud != null &&
                        (p.direccion == null || p.direccion!.isEmpty))
                      Padding(
                        padding: const EdgeInsets.only(top: 6),
                        child: Text(
                          'Dirección pendiente de calcular',
                          key: const Key('direccionPendiente'),
                          style: tema.textTheme.bodySmall?.copyWith(
                            color: tema.colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ),
                  ],
                  const SizedBox(height: 16),
                  FilledButton(
                    onPressed: (_guardando || !hayCambios) ? null : _guardar,
                    child: _guardando
                        ? const IndicadorBoton()
                        : const Text('Guardar cambios'),
                  ),
                ],
              ),
            ),
          ),
        ),
        const SizedBox(height: 16),
        Card(
          child: Column(
            children: [
              FilaAccion(
                icono: Icons.lock_outline,
                titulo: 'Cambiar contraseña',
                detalle: 'Cerraremos tus otras sesiones',
                onTap: () => context.push('/perfil/contrasena'),
              ),
              const Divider(),
              FilaAccion(
                icono: Icons.mail_outline,
                titulo: 'Cambiar correo',
                detalle: 'Lo confirmas con un enlace',
                onTap: () => context.push('/perfil/correo'),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text('Apariencia', style: tema.textTheme.titleMedium),
                const SizedBox(height: 12),
                SegmentedButton<ThemeMode>(
                  showSelectedIcon: false,
                  segments: const [
                    ButtonSegment(
                      value: ThemeMode.system,
                      label: Text('Sistema'),
                    ),
                    ButtonSegment(value: ThemeMode.light, label: Text('Claro')),
                    ButtonSegment(value: ThemeMode.dark, label: Text('Oscuro')),
                  ],
                  selected: {apariencia},
                  onSelectionChanged: (elegida) =>
                      ref.read(temaProvider.notifier).elegir(elegida.first),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        Card(
          child: FilaAccion(
            icono: Icons.logout,
            titulo: 'Cerrar sesión',
            peligro: true,
            onTap: _cerrarSesion,
          ),
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
  final Widget? insignia;
  final String? ayuda;

  const _FilaSoloLectura({
    super.key,
    required this.etiqueta,
    required this.valor,
    this.insignia,
    this.ayuda,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    return Align(
      alignment: Alignment.centerLeft,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            etiqueta.toUpperCase(),
            style: tema.textTheme.labelSmall?.copyWith(
              fontWeight: FontWeight.w800,
              letterSpacing: 1.2,
              color: tema.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 3),
          Wrap(
            spacing: 8,
            runSpacing: 4,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [Text(valor, style: tema.textTheme.bodyLarge), ?insignia],
          ),
          if (ayuda != null)
            Text(
              ayuda!,
              style: tema.textTheme.bodySmall?.copyWith(
                color: tema.colorScheme.onSurfaceVariant,
              ),
            ),
        ],
      ),
    );
  }
}
