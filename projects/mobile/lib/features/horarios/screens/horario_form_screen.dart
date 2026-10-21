import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../models/crear_plantilla_horario_request.dart';
import '../models/fechas.dart';
import '../providers/horarios_provider.dart';
import '../validators/horarios_validators.dart';
import '../widgets/errores.dart';

/// Formulario de un horario nuevo (CU12).
///
/// * Las reglas de validación son las del backend (horarios_validators.dart). El cruce con otros
/// * horarios solo lo sabe el backend: su error (`ERR_ACA_04`) se muestra arriba del botón.
class HorarioFormScreen extends ConsumerStatefulWidget {
  const HorarioFormScreen({super.key});

  @override
  ConsumerState<HorarioFormScreen> createState() => _HorarioFormScreenState();
}

class _HorarioFormScreenState extends ConsumerState<HorarioFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _horaInicio = TextEditingController();
  final _horaFin = TextEditingController();
  final _duracion = TextEditingController(text: '60');
  final _desde = TextEditingController();
  final _hasta = TextEditingController();

  int _diaSemana = 1;
  DateTime? _vigenteDesde;
  DateTime? _vigenteHasta;
  bool _enviando = false;
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
    for (final c in [_horaInicio, _horaFin, _duracion, _desde, _hasta]) {
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
    final clase = int.tryParse(_duracion.text.trim()) ?? 60;
    final minutos = (inicio.hour * 60 + inicio.minute + clase).clamp(0, 23 * 60 + 59);
    return TimeOfDay(hour: minutos ~/ 60, minute: minutos % 60);
  }

  Future<void> _elegirHora(TextEditingController controlador, TimeOfDay porDefecto) async {
    final elegida = await showTimePicker(
      context: context,
      initialTime: _horaInicial(controlador, porDefecto),
      builder: (contexto, hijo) => MediaQuery(
        data: MediaQuery.of(contexto).copyWith(alwaysUse24HourFormat: true),
        child: hijo!,
      ),
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

  Future<void> _guardar() async {
    setState(() => _errorGeneral = null);
    if (!_formKey.currentState!.validate()) return;

    final mensajes = ScaffoldMessenger.of(context);
    final router = GoRouter.of(context);
    setState(() => _enviando = true);
    try {
      final creada = await ref
          .read(horariosProvider.notifier)
          .crear(
            CrearPlantillaHorarioRequest(
              diaSemana: _diaSemana,
              horaInicio: _horaInicio.text,
              horaFin: _horaFin.text,
              duracionClaseMin: int.parse(_duracion.text.trim()),
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
    return Scaffold(
      appBar: AppBar(title: const Text('Nuevo horario')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          autovalidateMode: AutovalidateMode.onUserInteraction,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              DropdownButtonFormField<int>(
                key: const Key('campoDia'),
                initialValue: _diaSemana,
                decoration: const InputDecoration(labelText: 'Día de la semana'),
                items: [
                  for (var dia = 1; dia <= 7; dia++)
                    DropdownMenuItem(
                      value: dia,
                      child: Text('${nombreDeDia(dia)[0].toUpperCase()}${nombreDeDia(dia).substring(1)}'),
                    ),
                ],
                onChanged: (valor) => setState(() => _diaSemana = valor ?? 1),
              ),
              const SizedBox(height: 16),
              TextFormField(
                key: const Key('campoHoraInicio'),
                controller: _horaInicio,
                readOnly: true,
                decoration: const InputDecoration(
                  labelText: 'Hora de inicio',
                  suffixIcon: Icon(Icons.schedule_outlined),
                ),
                onTap: () => _elegirHora(_horaInicio, const TimeOfDay(hour: 8, minute: 0)),
                validator: validarHoraInicio,
              ),
              const SizedBox(height: 16),
              TextFormField(
                key: const Key('campoHoraFin'),
                controller: _horaFin,
                readOnly: true,
                decoration: const InputDecoration(
                  labelText: 'Hora de fin',
                  suffixIcon: Icon(Icons.schedule_outlined),
                ),
                onTap: () => _elegirHora(_horaFin, _finSugerido()),
                validator: (valor) =>
                    validarHoraFin(_horaInicio.text, valor) ??
                    validarVentana(_horaInicio.text, valor, _duracion.text),
              ),
              const SizedBox(height: 16),
              TextFormField(
                key: const Key('campoDuracion'),
                controller: _duracion,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Duración de cada clase (minutos)'),
                validator: (valor) =>
                    validarDuracion(valor) ?? validarVentana(_horaInicio.text, _horaFin.text, valor),
              ),
              const SizedBox(height: 16),
              TextFormField(
                key: const Key('campoDesde'),
                controller: _desde,
                readOnly: true,
                decoration: const InputDecoration(
                  labelText: 'Vigente desde',
                  suffixIcon: Icon(Icons.calendar_today_outlined),
                ),
                onTap: _elegirDesde,
                validator: (_) => validarVigenteDesde(_vigenteDesde, hoy: _hoy),
              ),
              const SizedBox(height: 16),
              TextFormField(
                key: const Key('campoHasta'),
                controller: _hasta,
                readOnly: true,
                decoration: InputDecoration(
                  labelText: 'Vigente hasta (opcional)',
                  suffixIcon: _vigenteHasta == null
                      ? const Icon(Icons.calendar_today_outlined)
                      : IconButton(
                          tooltip: 'Quitar fecha',
                          icon: const Icon(Icons.clear),
                          onPressed: _limpiarHasta,
                        ),
                ),
                onTap: _elegirHasta,
                validator: (_) => validarVigenteHasta(_vigenteHasta, _vigenteDesde, hoy: _hoy),
              ),
              if (_errorGeneral != null) ...[
                const SizedBox(height: 16),
                Text(
                  _errorGeneral!,
                  key: const Key('errorGeneral'),
                  style: TextStyle(color: Theme.of(context).colorScheme.error),
                ),
              ],
              const SizedBox(height: 24),
              FilledButton(
                key: const Key('botonGuardarHorario'),
                onPressed: _enviando ? null : _guardar,
                child: _enviando
                    ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Text('Guardar'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
