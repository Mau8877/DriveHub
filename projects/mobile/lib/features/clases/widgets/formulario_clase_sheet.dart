import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../models/actualizar_clase_request.dart';
import '../models/asistencia_clase.dart';
import '../models/cerrar_clase_request.dart';
import '../models/clase_agenda.dart';
import '../models/clase_dictada.dart';
import '../providers/clase_dictada_provider.dart';
import '../providers/gestion_clase_provider.dart';
import 'formato_clases.dart';
import 'mensajes_de_cierre.dart';
import 'validadores_clase.dart';

/// Abre la hoja para cerrar una clase que ya empezó (CU17).
Future<void> mostrarCerrarClase(BuildContext context, ClaseAgenda clase) =>
    _abrir(context, FormularioClaseSheet.cerrar(clase: clase));

/// Abre la hoja para corregir una clase ya cerrada (CU17). La clase tiene que traer su `claseCodigo`.
Future<void> mostrarEditarClase(BuildContext context, ClaseAgenda clase) =>
    _abrir(context, FormularioClaseSheet.editar(clase: clase));

Future<void> _abrir(BuildContext context, Widget hoja) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    // * Mientras la llamada está en curso no se cierra tocando afuera ni deslizando; para salir está «Volver»
    isDismissible: false,
    enableDrag: false,
    builder: (_) => hoja,
  );
}

enum _Modo { cerrar, editar }

/// El formulario de una clase (CU17): al cerrarla se elige la asistencia y se completan tema, observaciones y
/// kilómetros; al corregirla se ven los datos guardados y se cambia lo que haga falta (la asistencia no se cambia).
///
/// * Con `Ausente` (o al corregir una clase a la que no asistió) solo quedan las observaciones: no hay tema ni
///   kilómetros de una clase que no se dictó.
/// * Mientras la llamada está en curso los botones quedan deshabilitados: un doble toque manda una sola petición.
/// * Sin red la hoja queda abierta y el botón pasa a «Reintentar»; nunca se da por cerrada una clase sin respuesta.
/// * `ERR_ACA_35` y `ERR_ACA_37` cierran la hoja con el mensaje del servidor (la agenda ya se refrescó); `ERR_ACA_38` y
///   otros errores de negocio se muestran dentro, sin perder lo escrito.
class FormularioClaseSheet extends ConsumerWidget {
  final ClaseAgenda clase;
  final _Modo _modo;

  const FormularioClaseSheet.cerrar({super.key, required this.clase}) : _modo = _Modo.cerrar;

  const FormularioClaseSheet.editar({super.key, required this.clase}) : _modo = _Modo.editar;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tema = Theme.of(context);
    final editando = _modo == _Modo.editar;

    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 12),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(editando ? 'Editar clase' : 'Cerrar clase', style: tema.textTheme.titleLarge),
            const SizedBox(height: 2),
            Text(
              '${tituloDeDia(clase.fecha, DateTime.now())} · ${rangoDeHoras(clase.horaInicio, clase.horaFin)}',
              key: const Key('claseACerrar'),
              style: tema.textTheme.titleMedium,
            ),
            Text(
              clase.estudianteNombre,
              style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: 16),
            if (!editando)
              _CuerpoFormulario(clase: clase, inicial: null)
            else
              ref
                  .watch(claseDictadaDetailProvider(clase.claseCodigo!))
                  .when(
                    loading: () => const Padding(
                      padding: EdgeInsets.all(32),
                      child: Center(child: CircularProgressIndicator()),
                    ),
                    error: (error, _) => _ErrorAlCargar(clase: clase, error: error),
                    data: (guardada) => _CuerpoFormulario(clase: clase, inicial: guardada),
                  ),
          ],
        ),
      ),
    );
  }
}

/// No se pudo traer lo guardado de la clase que se quiere corregir.
class _ErrorAlCargar extends ConsumerWidget {
  final ClaseAgenda clase;
  final Object error;

  const _ErrorAlCargar({required this.clase, required this.error});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        AvisoCockpit(
          key: const Key('errorCargarClase'),
          tono: TonoAviso.error,
          texto: mensajeDeError(error),
          accion: TextButton(
            onPressed: () => ref.invalidate(claseDictadaDetailProvider(clase.claseCodigo!)),
            child: const Text('Reintentar'),
          ),
        ),
        const SizedBox(height: 12),
        OutlinedButton(
          key: const Key('volverDeCierre'),
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Volver'),
        ),
      ],
    );
  }
}

/// El formulario con sus campos y botones. [inicial] es lo guardado al corregir (nulo al cerrar).
class _CuerpoFormulario extends ConsumerStatefulWidget {
  final ClaseAgenda clase;
  final ClaseDictada? inicial;

  const _CuerpoFormulario({required this.clase, required this.inicial});

  @override
  ConsumerState<_CuerpoFormulario> createState() => _CuerpoFormularioState();
}

class _CuerpoFormularioState extends ConsumerState<_CuerpoFormulario> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _tema;
  late final TextEditingController _observaciones;
  late final TextEditingController _kmInicio;
  late final TextEditingController _kmFin;
  AsistenciaClase? _asistencia;
  bool _enviando = false;
  bool _esDeRed = false;
  String? _error;

  bool get _editando => widget.inicial != null;

  /// La asistencia que manda: la elegida al cerrar, la guardada al corregir.
  AsistenciaClase? get _asistenciaActual => _editando ? widget.inicial!.asistencia : _asistencia;

  bool get _ausente => _asistenciaActual == AsistenciaClase.ausente;

  @override
  void initState() {
    super.initState();
    final guardada = widget.inicial;
    _tema = TextEditingController(text: guardada?.tema ?? '');
    _observaciones = TextEditingController(text: guardada?.observaciones ?? '');
    _kmInicio = TextEditingController(text: guardada?.kmInicio?.toString() ?? '');
    _kmFin = TextEditingController(text: guardada?.kmFin?.toString() ?? '');
  }

  @override
  void dispose() {
    _tema.dispose();
    _observaciones.dispose();
    _kmInicio.dispose();
    _kmFin.dispose();
    super.dispose();
  }

  /// Con «Ausente» no hay tema ni kilómetros: se ocultan y se vacían, así no queda nada escrito que se pueda enviar.
  void _elegirAsistencia(AsistenciaClase? elegida) {
    setState(() {
      _asistencia = elegida;
      if (elegida == AsistenciaClase.ausente) {
        _tema.clear();
        _kmInicio.clear();
        _kmFin.clear();
      }
    });
  }

  /// El texto escrito, recortado, o `null` si quedó en blanco.
  static String? _texto(TextEditingController controlador) {
    final texto = controlador.text.trim();
    return texto.isEmpty ? null : texto;
  }

  Future<void> _enviar() async {
    if (_enviando) return;
    if (!(_formKey.currentState?.validate() ?? false)) return;
    // * El contexto del navegador sobrevive a esta hoja: con él se avisa después de cerrarla
    final navegador = Navigator.of(context);
    final contexto = navegador.context;
    final cierre = _editando ? null : _pedidoDeCierre();
    final correccion = _editando ? _pedidoDeCorreccion() : null;
    if (_editando && correccion == null) return; // * un kilometraje que no se puede borrar: ya se avisó
    if (correccion != null && correccion.estaVacio) {
      navegador.pop();
      return;
    }
    setState(() {
      _enviando = true;
      _error = null;
    });
    try {
      final gestion = ref.read(gestionClaseProvider.notifier);
      if (cierre != null) {
        await gestion.cerrar(cierre);
      } else {
        await gestion.actualizar(widget.inicial!.codigo, correccion!);
      }
      if (!mounted || !contexto.mounted) return;
      navegador.pop();
      avisar(contexto, cierre != null ? avisoDeResultadoDeCierre(cierre.asistencia) : avisoDeCorreccionGuardada);
    } catch (e) {
      if (!mounted) return;
      if (e is ApiException && codigosQueCierranElCierre.contains(e.code)) {
        navegador.pop();
        if (contexto.mounted) avisar(contexto, e.message);
        return;
      }
      setState(() {
        _enviando = false;
        _esDeRed = e is! ApiException;
        _error = e is Exception ? mensajeDeError(e) : 'No pudimos guardar. Inténtalo de nuevo.';
      });
    }
  }

  CerrarClaseRequest _pedidoDeCierre() {
    final ausente = _ausente;
    return CerrarClaseRequest(
      reservaCodigo: widget.clase.codigo,
      asistencia: _asistencia!,
      tema: ausente ? null : _texto(_tema),
      observaciones: _texto(_observaciones),
      kmInicio: ausente ? null : kilometrajeDe(_kmInicio.text),
      kmFin: ausente ? null : kilometrajeDe(_kmFin.text),
    );
  }

  /// Solo lo que cambió respecto de lo guardado. Un texto borrado viaja vacío (así se borra). Devuelve `null` si se
  /// quiso borrar un kilometraje, que el backend no admite: se avisa y no se envía nada.
  ActualizarClaseRequest? _pedidoDeCorreccion() {
    final guardada = widget.inicial!;
    final observaciones = _texto(_observaciones) ?? '';
    final cambioObservaciones = observaciones != (guardada.observaciones ?? '') ? observaciones : null;
    if (_ausente) return ActualizarClaseRequest(observaciones: cambioObservaciones);

    final tema = _texto(_tema) ?? '';
    final kmInicio = kilometrajeDe(_kmInicio.text);
    final kmFin = kilometrajeDe(_kmFin.text);
    if ((kmInicio == null && guardada.kmInicio != null) || (kmFin == null && guardada.kmFin != null)) {
      setState(() => _error = avisoKilometrajeNoSeBorra);
      return null;
    }
    return ActualizarClaseRequest(
      tema: tema != (guardada.tema ?? '') ? tema : null,
      observaciones: cambioObservaciones,
      kmInicio: kmInicio != null && kmInicio != guardada.kmInicio ? kmInicio : null,
      kmFin: kmFin != null && kmFin != guardada.kmFin ? kmFin : null,
    );
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final puedeEnviar = !_enviando && (_editando || _asistencia != null);

    return Form(
      key: _formKey,
      autovalidateMode: AutovalidateMode.onUserInteraction,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (_editando)
            Text(
              'Asistencia: ${widget.inicial!.asistencia.etiqueta}',
              key: const Key('asistenciaFija'),
              style: tema.textTheme.titleSmall,
            )
          else ...[
            Text('Asistencia', style: tema.textTheme.titleSmall),
            const SizedBox(height: 8),
            SegmentedButton<AsistenciaClase>(
              key: const Key('asistencia'),
              showSelectedIcon: false,
              emptySelectionAllowed: true,
              segments: [for (final a in AsistenciaClase.values) ButtonSegment(value: a, label: Text(a.etiqueta))],
              selected: {?_asistencia},
              onSelectionChanged: _enviando ? null : (elegida) => _elegirAsistencia(elegida.firstOrNull),
            ),
          ],
          if (!_editando && _ausente) ...[
            const SizedBox(height: 12),
            const AvisoCockpit(key: Key('avisoInasistencia'), tono: TonoAviso.advertencia, texto: avisoDeInasistencia),
          ],
          const SizedBox(height: 16),
          if (!_ausente) ...[
            TextFormField(
              key: const Key('campoTema'),
              controller: _tema,
              enabled: !_enviando,
              maxLength: maxTema,
              maxLengthEnforcement: MaxLengthEnforcement.none,
              textCapitalization: TextCapitalization.sentences,
              decoration: const InputDecoration(labelText: 'Tema (opcional)'),
              validator: validarTema,
            ),
            const SizedBox(height: 8),
          ],
          TextFormField(
            key: const Key('campoObservaciones'),
            controller: _observaciones,
            enabled: !_enviando,
            maxLength: maxObservaciones,
            maxLengthEnforcement: MaxLengthEnforcement.none,
            minLines: 3,
            maxLines: 5,
            textCapitalization: TextCapitalization.sentences,
            decoration: const InputDecoration(labelText: 'Observaciones (opcional)'),
            validator: validarObservaciones,
          ),
          if (!_ausente) ...[
            const SizedBox(height: 8),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: TextFormField(
                    key: const Key('campoKmInicio'),
                    controller: _kmInicio,
                    enabled: !_enviando,
                    keyboardType: TextInputType.number,
                    inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    decoration: const InputDecoration(labelText: 'Km inicial (opcional)'),
                    validator: validarKilometraje,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: TextFormField(
                    key: const Key('campoKmFin'),
                    controller: _kmFin,
                    enabled: !_enviando,
                    keyboardType: TextInputType.number,
                    inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    decoration: const InputDecoration(labelText: 'Km final (opcional)'),
                    validator: (valor) => validarKmFinal(_kmInicio.text, valor),
                  ),
                ),
              ],
            ),
          ],
          if (_error != null) ...[
            const SizedBox(height: 12),
            AvisoCockpit(key: const Key('errorCierre'), tono: TonoAviso.error, texto: _error!),
          ],
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  key: const Key('volverDeCierre'),
                  onPressed: _enviando ? null : () => Navigator.of(context).pop(),
                  child: const Text('Volver'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: FilledButton(
                  key: const Key('confirmarCierre'),
                  onPressed: puedeEnviar ? _enviar : null,
                  child: _enviando
                      ? const IndicadorBoton()
                      : Text(_esDeRed ? 'Reintentar' : (_editando ? 'Guardar' : 'Cerrar clase')),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
