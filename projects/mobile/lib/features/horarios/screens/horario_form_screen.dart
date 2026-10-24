import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/app_theme.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../models/crear_plantilla_horario_request.dart';
import '../models/fechas.dart';
import '../providers/horarios_provider.dart';
import '../validators/horarios_validators.dart';
import '../widgets/dias_semana.dart';
import '../widgets/errores.dart';

/// Duraciones que ofrece el formulario (el backend acepta de 15 a 240 minutos).
const List<int> _duracionesDeClase = [45, 60, 90];

/// Formulario de un horario nuevo (CU12): nombre, varios días, ventana de horas, duración y vigencia.
///
/// * Las reglas de validación son las del backend (horarios_validators.dart). El cruce con otros
/// * horarios solo lo sabe el backend: su error (`ERR_ACA_04`) se muestra arriba del botón.
/// * La vista previa de franjas se calcula en el teléfono: es solo una ayuda, el backend es quien genera.
class HorarioFormScreen extends ConsumerStatefulWidget {
  const HorarioFormScreen({super.key});

  @override
  ConsumerState<HorarioFormScreen> createState() => _HorarioFormScreenState();
}

class _HorarioFormScreenState extends ConsumerState<HorarioFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nombre = TextEditingController();
  final _horaInicio = TextEditingController();
  final _horaFin = TextEditingController();
  final _desde = TextEditingController();
  final _hasta = TextEditingController();

  final Set<int> _dias = {};
  int _duracion = 60;
  DateTime? _vigenteDesde;
  DateTime? _vigenteHasta;
  bool _enviando = false;
  bool _intentoEnviar = false;
  String? _errorGeneral;

  late final DateTime _hoy = soloFecha(DateTime.now());

  @override
  void initState() {
    super.initState();
    _vigenteDesde = _hoy;
    _desde.text = fechaParaMostrar(_hoy);
  }

  @override
  void dispose() {
    for (final c in [_nombre, _horaInicio, _horaFin, _desde, _hasta]) {
      c.dispose();
    }
    super.dispose();
  }

  /// Hora que propone el selector: la ya elegida, o [porDefecto] si el campo está vacío.
  TimeOfDay _horaInicial(TextEditingController controlador, TimeOfDay porDefecto) {
    final partes = controlador.text.split(':');
    if (partes.length != 2) return porDefecto;
    return TimeOfDay(hour: int.parse(partes[0]), minute: int.parse(partes[1]));
  }

  /// Al elegir la hora de fin se propone la de inicio más una clase (así, con los valores por
  /// defecto, queda un horario de una sola franja); sin hora de inicio, las 12:00.
  TimeOfDay _finSugerido() {
    final inicio = _horaInicial(_horaInicio, const TimeOfDay(hour: 11, minute: 0));
    final minutos = (inicio.hour * 60 + inicio.minute + _duracion).clamp(0, 23 * 60 + 59);
    return TimeOfDay(hour: minutos ~/ 60, minute: minutos % 60);
  }

  Future<void> _elegirHora(TextEditingController controlador, TimeOfDay porDefecto) async {
    final elegida = await showTimePicker(
      context: context,
      initialTime: _horaInicial(controlador, porDefecto),
      builder: (contexto, hijo) =>
          MediaQuery(data: MediaQuery.of(contexto).copyWith(alwaysUse24HourFormat: true), child: hijo!),
    );
    if (elegida == null) return;
    setState(() => controlador.text = horaParaApi(elegida.hour, elegida.minute));
  }

  Future<void> _elegirDesde() async {
    final elegida = await showDatePicker(
      context: context,
      initialDate: _vigenteDesde ?? _hoy,
      firstDate: _hoy,
      lastDate: _hoy.add(const Duration(days: 365 * 2)),
      helpText: 'Vigente desde',
    );
    if (elegida == null) return;
    setState(() {
      _vigenteDesde = soloFecha(elegida);
      _desde.text = fechaParaMostrar(elegida);
    });
  }

  Future<void> _elegirHasta() async {
    final base = _vigenteDesde ?? _hoy;
    final elegida = await showDatePicker(
      context: context,
      initialDate: _vigenteHasta ?? base,
      firstDate: base,
      lastDate: base.add(const Duration(days: 365 * 3)),
      helpText: 'Vigente hasta',
    );
    if (elegida == null) return;
    setState(() {
      _vigenteHasta = soloFecha(elegida);
      _hasta.text = fechaParaMostrar(elegida);
    });
  }

  void _limpiarHasta() => setState(() {
    _vigenteHasta = null;
    _hasta.clear();
  });

  void _alternarDia(int dia) => setState(() {
    if (!_dias.remove(dia)) _dias.add(dia);
  });

  Future<void> _guardar() async {
    setState(() {
      _errorGeneral = null;
      _intentoEnviar = true;
    });
    final formularioValido = _formKey.currentState!.validate();
    if (!formularioValido || validarDias(_dias) != null) return;

    final mensajes = ScaffoldMessenger.of(context);
    final router = GoRouter.of(context);
    setState(() => _enviando = true);
    try {
      final creada = await ref
          .read(horariosProvider.notifier)
          .crear(
            CrearPlantillaHorarioRequest(
              nombre: _nombre.text.trim(),
              diasSemana: _dias.toList()..sort(),
              horaInicio: _horaInicio.text,
              horaFin: _horaFin.text,
              duracionClaseMin: _duracion,
              vigenteDesde: _vigenteDesde,
              vigenteHasta: _vigenteHasta,
            ),
          );
      router.pop();
      mensajes.showSnackBar(SnackBar(content: Text('Horario creado: ${creada.franjasGeneradas} franjas generadas')));
    } catch (e) {
      // * Sin perder lo escrito: el error se muestra en el formulario
      if (mounted) setState(() => _errorGeneral = mensajeDeError(e));
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final errorDeDias = _intentoEnviar ? validarDias(_dias) : null;
    final vista = vistaPreviaFranjas(
      horaInicio: _horaInicio.text,
      horaFin: _horaFin.text,
      duracionMin: _duracion,
      cantidadDias: _dias.length,
    );

    return CockpitScaffold(
      appBar: AppBar(title: const Text('Nuevo horario')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
        child: Form(
          key: _formKey,
          autovalidateMode: AutovalidateMode.onUserInteraction,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _Etiqueta('Nombre', tema),
              TextFormField(
                key: const Key('campoNombre'),
                controller: _nombre,
                textCapitalization: TextCapitalization.sentences,
                inputFormatters: [LengthLimitingTextInputFormatter(nombreMaximo)],
                decoration: const InputDecoration(hintText: 'Por ejemplo, Mañanas entre semana'),
                validator: validarNombre,
              ),
              const SizedBox(height: 16),
              _Etiqueta('¿Qué días atiendes?', tema),
              SelectorDeDias(seleccionados: _dias, alAlternar: _alternarDia),
              if (errorDeDias != null)
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Text(
                    errorDeDias,
                    key: const Key('errorDias'),
                    style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.error),
                  ),
                ),
              const SizedBox(height: 16),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: _campoHora(
                      etiqueta: 'Desde las',
                      clave: 'campoHoraInicio',
                      controlador: _horaInicio,
                      porDefecto: const TimeOfDay(hour: 8, minute: 0),
                      validador: validarHoraInicio,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _campoHora(
                      etiqueta: 'Hasta las',
                      clave: 'campoHoraFin',
                      controlador: _horaFin,
                      sugerida: _finSugerido,
                      validador: (valor) =>
                          validarHoraFin(_horaInicio.text, valor) ??
                          validarVentana(_horaInicio.text, valor, _duracion.toString()),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              _Etiqueta('Duración de cada clase', tema),
              SegmentedButton<int>(
                key: const Key('campoDuracion'),
                segments: [
                  for (final minutos in _duracionesDeClase) ButtonSegment(value: minutos, label: Text('$minutos min')),
                ],
                selected: {_duracion},
                onSelectionChanged: (elegida) => setState(() => _duracion = elegida.first),
              ),
              const SizedBox(height: 16),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: _campoFecha(
                      etiqueta: 'Vigente desde',
                      clave: 'campoDesde',
                      controlador: _desde,
                      alTocar: _elegirDesde,
                      validador: (_) => validarVigenteDesde(_vigenteDesde, hoy: _hoy),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _campoFecha(
                      etiqueta: 'Hasta (opcional)',
                      clave: 'campoHasta',
                      controlador: _hasta,
                      alTocar: _elegirHasta,
                      quitar: _vigenteHasta == null ? null : _limpiarHasta,
                      validador: (_) => validarVigenteHasta(_vigenteHasta, _vigenteDesde, hoy: _hoy),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              _VistaPrevia(vista: vista),
              if (_errorGeneral != null) ...[
                const SizedBox(height: 16),
                Text(
                  _errorGeneral!,
                  key: const Key('errorGeneral'),
                  style: TextStyle(color: tema.colorScheme.error),
                ),
              ],
            ],
          ),
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
          child: FilledButton(
            key: const Key('botonGuardarHorario'),
            onPressed: _enviando ? null : _guardar,
            child: _enviando
                ? const Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2)),
                      SizedBox(width: 8),
                      Text('Guardando…'),
                    ],
                  )
                : const Text('Guardar horario'),
          ),
        ),
      ),
    );
  }

  Widget _campoHora({
    required String etiqueta,
    required String clave,
    required TextEditingController controlador,
    required String? Function(String?) validador,
    TimeOfDay porDefecto = const TimeOfDay(hour: 12, minute: 0),
    TimeOfDay Function()? sugerida,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _Etiqueta(etiqueta, Theme.of(context)),
        TextFormField(
          key: Key(clave),
          controller: controlador,
          readOnly: true,
          decoration: const InputDecoration(prefixIcon: Icon(Icons.schedule_outlined)),
          onTap: () => _elegirHora(controlador, sugerida?.call() ?? porDefecto),
          validator: validador,
        ),
      ],
    );
  }

  Widget _campoFecha({
    required String etiqueta,
    required String clave,
    required TextEditingController controlador,
    required VoidCallback alTocar,
    required String? Function(String?) validador,
    VoidCallback? quitar,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _Etiqueta(etiqueta, Theme.of(context)),
        TextFormField(
          key: Key(clave),
          controller: controlador,
          readOnly: true,
          decoration: InputDecoration(
            prefixIcon: const Icon(Icons.calendar_today_outlined),
            suffixIcon: quitar == null
                ? null
                : IconButton(tooltip: 'Quitar fecha', icon: const Icon(Icons.clear), onPressed: quitar),
          ),
          onTap: alTocar,
          validator: validador,
        ),
      ],
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

/// «Se crearán N franjas por día, M por semana», recalculado con cada cambio del formulario.
class _VistaPrevia extends StatelessWidget {
  final ({int porDia, int porSemana})? vista;

  const _VistaPrevia({required this.vista});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final texto = vista == null
        ? 'Elige los días y las horas para ver cuántas franjas se crearán.'
        : 'Se crearán ${vista!.porDia} ${vista!.porDia == 1 ? 'franja' : 'franjas'} por día, ${vista!.porSemana} por semana.';

    return Container(
      key: const Key('vistaPrevia'),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Color.alphaBlend(
          AppColors.ambar.withValues(alpha: 0.14),
          tema.colorScheme.surface,
        ),
        borderRadius: BorderRadius.circular(AppRadios.control),
        border: Border.all(color: AppColors.ambar.withValues(alpha: 0.45)),
      ),
      child: Row(
        children: [
          Icon(Icons.schedule, color: context.cockpit.acento),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              texto,
              style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurface),
            ),
          ),
        ],
      ),
    );
  }
}
