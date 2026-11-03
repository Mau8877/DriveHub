import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../models/catalogo_pruebas.dart';
import '../models/clase_agenda.dart';
import '../models/evaluar_clase_request.dart';
import '../models/prueba_clase.dart';
import '../models/tipo_prueba.dart';
import '../providers/catalogo_pruebas_provider.dart';
import '../providers/gestion_prueba_clase_provider.dart';
import '../providers/pruebas_de_clase_provider.dart';
import '../../../shared/widgets/insignia.dart';
import 'formato_clases.dart';
import 'mensajes_de_prueba.dart';
import 'validadores_clase.dart';
import 'validadores_prueba.dart';

/// Abre la hoja para evaluar una clase ya cerrada con asistencia (CU18). La clase tiene que traer su `claseCodigo`.
Future<void> mostrarEvaluarClase(BuildContext context, ClaseAgenda clase) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    // * No se cierra tocando afuera ni deslizando, ni siquiera sin enviar: para salir está «Volver», así un toque
    // * fuera de lugar no pierde los puntajes ya puestos
    isDismissible: false,
    enableDrag: false,
    builder: (_) => EvaluarClaseSheet(clase: clase),
  );
}

/// El formulario de una prueba de clase (CU18): se elige el tipo, se puntúa cada criterio y se confirma.
///
/// * Los criterios los entrega el servidor; un criterio arranca «sin puntuar» y «Guardar prueba» queda apagado hasta
///   que todos tengan puntaje. El total y el resultado que se ven mientras se puntúa son una vista previa.
/// * Los tipos que la clase ya tiene quedan deshabilitados.
/// * Mientras la llamada está en curso los botones quedan deshabilitados: un doble toque manda una sola petición.
/// * Sin red la hoja queda abierta y el botón pasa a «Reintentar»; nunca se da por evaluada una clase sin respuesta.
/// * `ERR_ACA_35`, `ERR_ACA_39` y `ERR_ACA_40` cierran la hoja con el mensaje del servidor (la agenda ya se
///   refrescó); `ERR_ACA_41` y otros errores de negocio se muestran dentro, sin perder lo escrito.
class EvaluarClaseSheet extends ConsumerWidget {
  final ClaseAgenda clase;

  const EvaluarClaseSheet({super.key, required this.clase});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tema = Theme.of(context);
    final catalogo = ref.watch(catalogoPruebasProvider);
    final hechas = ref.watch(pruebasDeClaseDetailProvider(clase.claseCodigo!));

    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 12),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Evaluar clase', style: tema.textTheme.titleLarge),
            const SizedBox(height: 2),
            Text(
              '${tituloDeDia(clase.fecha, DateTime.now())} · ${rangoDeHoras(clase.horaInicio, clase.horaFin)}',
              key: const Key('claseAEvaluar'),
              style: tema.textTheme.titleMedium,
            ),
            Text(
              clase.estudianteNombre,
              style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: 16),
            // * Con datos ya cargados el formulario se queda aunque se esté refrescando (evaluar invalida las pruebas de
            // * la clase): si se reemplazara por el indicador se perdería lo puntuado y el error del envío
            if (catalogo.hasValue && hechas.hasValue)
              _CuerpoEvaluar(
                clase: clase,
                catalogo: catalogo.requireValue,
                hechos: {for (final p in hechas.requireValue) p.tipo},
              )
            else if (catalogo.isLoading || hechas.isLoading)
              const Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: CircularProgressIndicator()),
              )
            else
              _ErrorAlCargar(
                error: catalogo.error ?? hechas.error!,
                alReintentar: () {
                  ref.invalidate(catalogoPruebasProvider);
                  ref.invalidate(pruebasDeClaseDetailProvider(clase.claseCodigo!));
                },
              ),
          ],
        ),
      ),
    );
  }
}

/// No se pudieron traer los criterios o las pruebas ya hechas.
class _ErrorAlCargar extends StatelessWidget {
  final Object error;
  final VoidCallback alReintentar;

  const _ErrorAlCargar({required this.error, required this.alReintentar});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        AvisoCockpit(
          key: const Key('errorCargarPrueba'),
          tono: TonoAviso.error,
          texto: mensajeDeError(error),
          accion: TextButton(onPressed: alReintentar, child: const Text('Reintentar')),
        ),
        const SizedBox(height: 12),
        OutlinedButton(
          key: const Key('volverDePrueba'),
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Volver'),
        ),
      ],
    );
  }
}

class _CuerpoEvaluar extends ConsumerStatefulWidget {
  final ClaseAgenda clase;
  final CatalogoPruebas catalogo;
  final Set<TipoPrueba> hechos;

  const _CuerpoEvaluar({required this.clase, required this.catalogo, required this.hechos});

  @override
  ConsumerState<_CuerpoEvaluar> createState() => _CuerpoEvaluarState();
}

class _CuerpoEvaluarState extends ConsumerState<_CuerpoEvaluar> {
  final _formKey = GlobalKey<FormState>();
  final _observaciones = TextEditingController();
  late TipoPrueba? _tipo;
  final Map<String, double?> _puntajes = {};
  bool _enviando = false;
  bool _esDeRed = false;
  String? _error;

  /// Los tipos que el servidor trajo y la clase todavía no tiene, en el orden del catálogo.
  List<TipoDelCatalogo> get _disponibles =>
      widget.catalogo.tipos.where((t) => !widget.hechos.contains(t.tipo)).toList();

  TipoDelCatalogo? get _tipoActual => _tipo == null ? null : widget.catalogo.de(_tipo!);

  List<CriterioDelCatalogo> get _criterios => _tipoActual?.criterios ?? const [];

  @override
  void initState() {
    super.initState();
    _tipo = _disponibles.firstOrNull?.tipo;
  }

  @override
  void dispose() {
    _observaciones.dispose();
    super.dispose();
  }

  /// Cambiar de tipo reinicia los puntajes: los criterios son otros.
  void _elegirTipo(TipoPrueba tipo) {
    setState(() {
      _tipo = tipo;
      _puntajes.clear();
      _error = null;
    });
  }

  Future<void> _enviar() async {
    if (_enviando) return;
    if (!(_formKey.currentState?.validate() ?? false)) return;
    final pendiente = validarCriteriosDePrueba(_puntajes, _criterios);
    if (pendiente != null) {
      setState(() => _error = pendiente);
      return;
    }
    // * El contexto del navegador sobrevive a esta hoja: con él se avisa después de cerrarla
    final navegador = Navigator.of(context);
    final contexto = navegador.context;
    final texto = _observaciones.text.trim();
    final pedido = EvaluarClaseRequest(
      claseCodigo: widget.clase.claseCodigo!,
      tipo: _tipo!,
      criterios: [for (final c in _criterios) PuntajeDeCriterio(codigo: c.codigo, puntaje: _puntajes[c.codigo]!)],
      observaciones: texto.isEmpty ? null : texto,
    );
    setState(() {
      _enviando = true;
      _error = null;
    });
    try {
      final PruebaClase prueba = await ref.read(gestionPruebaClaseProvider.notifier).evaluar(pedido);
      if (!mounted || !contexto.mounted) return;
      navegador.pop();
      avisar(contexto, avisoDePruebaRegistrada(prueba));
    } catch (e) {
      if (!mounted) return;
      if (e is ApiException && codigosQueCierranLaPrueba.contains(e.code)) {
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

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    if (_disponibles.isEmpty) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const AvisoCockpit(key: Key('todasLasPruebas'), tono: TonoAviso.info, texto: avisoTodasLasPruebasHechas),
          const SizedBox(height: 12),
          OutlinedButton(
            key: const Key('volverDePrueba'),
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Volver'),
          ),
        ],
      );
    }

    final criterios = _criterios;
    final total = totalDePrueba(_puntajes, criterios);
    final puedeEnviar = !_enviando && total != null;

    return Form(
      key: _formKey,
      autovalidateMode: AutovalidateMode.onUserInteraction,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Tipo de prueba', style: tema.textTheme.titleSmall),
          const SizedBox(height: 8),
          SegmentedButton<TipoPrueba>(
            key: const Key('tipoPrueba'),
            showSelectedIcon: false,
            segments: [
              for (final t in widget.catalogo.tipos)
                ButtonSegment(
                  value: t.tipo,
                  enabled: !widget.hechos.contains(t.tipo),
                  label: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(t.etiqueta),
                      if (widget.hechos.contains(t.tipo)) Text('Hecha', style: tema.textTheme.labelSmall),
                    ],
                  ),
                ),
            ],
            selected: {?_tipo},
            onSelectionChanged: _enviando ? null : (elegido) => _elegirTipo(elegido.first),
          ),
          const SizedBox(height: 16),
          for (final criterio in criterios)
            _FilaDeCriterio(
              criterio: criterio,
              puntaje: _puntajes[criterio.codigo],
              habilitado: !_enviando,
              alCambiar: (valor) => setState(() => _puntajes[criterio.codigo] = valor),
            ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: Text(
                  total == null ? 'Total: — / 100' : 'Total: ${formatoPuntaje(total)} / 100',
                  key: const Key('totalPrueba'),
                  style: tema.textTheme.titleMedium,
                ),
              ),
              if (total != null)
                Insignia(
                  resultadoDePrueba(total).etiqueta,
                  key: const Key('resultadoPrueba'),
                  tono: resultadoDePrueba(total) == ResultadoPrueba.aprobado
                      ? TonoInsignia.exito
                      : TonoInsignia.advertencia,
                ),
            ],
          ),
          const SizedBox(height: 12),
          TextFormField(
            key: const Key('observacionesPrueba'),
            controller: _observaciones,
            enabled: !_enviando,
            maxLength: maxObservaciones,
            maxLengthEnforcement: MaxLengthEnforcement.none,
            minLines: 2,
            maxLines: 4,
            textCapitalization: TextCapitalization.sentences,
            decoration: const InputDecoration(labelText: 'Observaciones (opcional)'),
            validator: validarObservacionesDePrueba,
          ),
          if (_error != null) ...[
            const SizedBox(height: 12),
            AvisoCockpit(key: const Key('errorPrueba'), tono: TonoAviso.error, texto: _error!),
          ],
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  key: const Key('volverDePrueba'),
                  onPressed: _enviando ? null : () => Navigator.of(context).pop(),
                  child: const Text('Volver'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: FilledButton(
                  key: const Key('guardarPrueba'),
                  onPressed: puedeEnviar ? _enviar : null,
                  child: _enviando ? const IndicadorBoton() : Text(_esDeRed ? 'Reintentar' : 'Guardar prueba'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

/// Un criterio con su control de 0 a su máximo, de medio en medio punto. Sin puntuar se ve atenuado y con «—».
class _FilaDeCriterio extends StatelessWidget {
  final CriterioDelCatalogo criterio;
  final double? puntaje;
  final bool habilitado;
  final ValueChanged<double> alCambiar;

  const _FilaDeCriterio({
    required this.criterio,
    required this.puntaje,
    required this.habilitado,
    required this.alCambiar,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final sinPuntuar = puntaje == null;

    return Column(
      key: Key('criterio-${criterio.codigo}'),
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(criterio.descripcion, style: tema.textTheme.bodyMedium),
        Row(
          children: [
            Expanded(
              child: Slider(
                key: Key('slider-${criterio.codigo}'),
                value: puntaje ?? 0,
                min: 0,
                max: criterio.maximo,
                divisions: (criterio.maximo / pasoDePuntaje).round(),
                label: formatoPuntaje(puntaje ?? 0),
                // * Sin puntuar el control va con el color secundario del tema: se ve que falta
                activeColor: sinPuntuar ? tema.colorScheme.outline : null,
                onChanged: habilitado ? alCambiar : null,
              ),
            ),
            SizedBox(
              width: 52,
              child: Text(
                sinPuntuar ? '—' : formatoPuntaje(puntaje!),
                key: Key('valor-${criterio.codigo}'),
                textAlign: TextAlign.end,
                style: tema.textTheme.titleSmall?.copyWith(
                  color: sinPuntuar ? tema.colorScheme.onSurfaceVariant : null,
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }
}
