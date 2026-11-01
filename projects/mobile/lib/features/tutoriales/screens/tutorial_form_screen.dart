import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../models/tutorial.dart';
import '../models/tutorial_request.dart';
import '../providers/selector_pdf_provider.dart';
import '../providers/tutoriales_providers.dart';
import '../validators/tutoriales_validators.dart';
import '../widgets/errores.dart';

/// Las categorías que ofrece el formulario mientras el catálogo de la escuela no tenga caso de uso.
const List<(String, String)> _categorias = [
  ('General', 'GENERAL'),
  ('Categoría A', 'A'),
  ('Categoría B', 'B'),
  ('Categoría C', 'C'),
];

/// Formulario para crear o editar un tutorial (CU33): título, descripción, categoría, a quién va dirigido, etiquetas y
/// el recurso (un PDF o un enlace de YouTube).
///
/// * Con [tutorial] edita ese tutorial; sin él crea uno nuevo, que nace como borrador.
/// * El PDF se sube apenas se elige y el tutorial lo referencia. Al editar, el recurso solo se manda si cambió.
/// * Las reglas de validación son las del backend (tutoriales_validators.dart).
class TutorialFormScreen extends ConsumerStatefulWidget {
  final Tutorial? tutorial;

  const TutorialFormScreen({super.key, this.tutorial});

  @override
  ConsumerState<TutorialFormScreen> createState() => _TutorialFormScreenState();
}

class _TutorialFormScreenState extends ConsumerState<TutorialFormScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _titulo;
  late final TextEditingController _descripcion;
  late final TextEditingController _etiquetas;
  late final TextEditingController _enlace;

  late String _categoria;
  late Set<AudienciaTutorial> _audiencia;
  late TipoTutorial _tipo;

  /// El PDF recién subido (null = ninguno nuevo; al editar un PDF, se conserva el que ya tiene).
  ArchivoSubido? _archivoNuevo;
  String? _errorPdf;
  bool _subiendo = false;
  bool _enviando = false;
  bool _intentoEnviar = false;
  String? _errorGeneral;

  Tutorial? get _original => widget.tutorial;

  @override
  void initState() {
    super.initState();
    final t = _original;
    _titulo = TextEditingController(text: t?.titulo);
    _descripcion = TextEditingController(text: t?.descripcion);
    _etiquetas = TextEditingController(text: t?.etiquetas.join(', '));
    _enlace = TextEditingController(
      text: t != null && t.tipo == TipoTutorial.enlace ? t.recursoUrl : '',
    );
    _categoria = t?.categoria ?? 'GENERAL';
    _audiencia = {...?t?.audiencia};
    _tipo = t?.tipo ?? TipoTutorial.enlace;
  }

  @override
  void dispose() {
    for (final c in [_titulo, _descripcion, _etiquetas, _enlace]) {
      c.dispose();
    }
    super.dispose();
  }

  /// ¿Hace falta un recurso nuevo? Al crear sí; al editar, si cambió el tipo o el enlace.
  bool get _recursoCambio {
    final t = _original;
    if (t == null || _tipo != t.tipo) return true;
    return _tipo == TipoTutorial.enlace
        ? _enlace.text.trim() != t.recursoUrl
        : _archivoNuevo != null;
  }

  Future<void> _elegirPdf() async {
    setState(() => _errorPdf = null);
    final elegido = await ref.read(selectorDePdfProvider)();
    if (elegido == null || !mounted) return;
    final tamano = validarTamanoPdf(elegido.bytes.length);
    if (tamano != null) {
      setState(() => _errorPdf = tamano);
      return;
    }
    setState(() => _subiendo = true);
    try {
      final archivo = await ref
          .read(tutorialAccionesProvider)
          .subirPdf(elegido.bytes, nombre: elegido.nombre);
      if (mounted) setState(() => _archivoNuevo = archivo);
    } catch (error) {
      if (mounted) setState(() => _errorPdf = mensajeDeErrorDeTutorial(error));
    } finally {
      if (mounted) setState(() => _subiendo = false);
    }
  }

  Future<void> _guardar() async {
    setState(() {
      _errorGeneral = null;
      _intentoEnviar = true;
    });
    final valido = _formKey.currentState!.validate();
    final audienciaOk = validarAudiencia(_audiencia) == null;
    final pdfFalta =
        _tipo == TipoTutorial.pdf && _recursoCambio && _archivoNuevo == null;
    if (pdfFalta) setState(() => _errorPdf = 'Elige el PDF del tutorial');
    if (!valido || !audienciaOk || pdfFalta) return;

    final router = GoRouter.of(context);
    final request = TutorialRequest(
      titulo: _titulo.text,
      descripcion: _descripcion.text,
      categoria: _categoria,
      tipo: _tipo,
      audiencia: [
        for (final a in AudienciaTutorial.values)
          if (_audiencia.contains(a)) a,
      ],
      etiquetas: separarEtiquetas(_etiquetas.text),
      enlace: _tipo == TipoTutorial.enlace && _recursoCambio
          ? _enlace.text
          : null,
      archivo: _tipo == TipoTutorial.pdf ? _archivoNuevo : null,
    );

    setState(() => _enviando = true);
    try {
      final acciones = ref.read(tutorialAccionesProvider);
      final original = _original;
      if (original == null) {
        await acciones.crear(request);
      } else {
        await acciones.editar(original.codigo, request);
      }
      if (!mounted) return;
      avisar(
        context,
        original == null
            ? 'Tutorial creado como borrador'
            : 'Tutorial actualizado',
      );
      router.pop();
    } catch (error) {
      if (mounted) {
        setState(() {
          _enviando = false;
          _errorGeneral = mensajeDeErrorDeTutorial(error);
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final editando = _original != null;
    final ocupado = _enviando || _subiendo;

    return CockpitScaffold(
      appBar: AppBar(
        title: Text(editando ? 'Editar tutorial' : 'Nuevo tutorial'),
      ),
      body: Form(
        key: _formKey,
        autovalidateMode: _intentoEnviar
            ? AutovalidateMode.onUserInteraction
            : AutovalidateMode.disabled,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
          children: [
            TextFormField(
              key: const Key('campoTituloTutorial'),
              controller: _titulo,
              decoration: const InputDecoration(labelText: 'Título'),
              validator: validarTitulo,
            ),
            const SizedBox(height: 14),
            TextFormField(
              key: const Key('campoDescripcionTutorial'),
              controller: _descripcion,
              decoration: const InputDecoration(
                labelText: 'Descripción (opcional)',
              ),
              maxLines: 3,
              validator: validarDescripcion,
            ),
            const SizedBox(height: 18),
            Text('Categoría', style: tema.textTheme.labelLarge),
            const SizedBox(height: 6),
            Wrap(
              spacing: 8,
              children: [
                for (final (texto, valor) in _categorias)
                  ChoiceChip(
                    key: Key('categoriaTutorial-$valor'),
                    label: Text(texto),
                    selected: _categoria == valor,
                    onSelected: (_) => setState(() => _categoria = valor),
                  ),
              ],
            ),
            const SizedBox(height: 18),
            Text('¿A quién va dirigido?', style: tema.textTheme.labelLarge),
            const SizedBox(height: 6),
            Wrap(
              spacing: 8,
              children: [
                for (final a in AudienciaTutorial.values)
                  FilterChip(
                    key: Key('audiencia-${a.valorApi}'),
                    label: Text(a.etiqueta),
                    selected: _audiencia.contains(a),
                    onSelected: (v) => setState(() {
                      v ? _audiencia.add(a) : _audiencia.remove(a);
                    }),
                  ),
              ],
            ),
            if (_intentoEnviar && validarAudiencia(_audiencia) != null)
              Padding(
                padding: const EdgeInsets.only(top: 6),
                child: Text(
                  validarAudiencia(_audiencia)!,
                  key: const Key('errorAudienciaTutorial'),
                  style: tema.textTheme.bodySmall?.copyWith(
                    color: tema.colorScheme.error,
                  ),
                ),
              ),
            const SizedBox(height: 18),
            TextFormField(
              key: const Key('campoEtiquetasTutorial'),
              controller: _etiquetas,
              decoration: const InputDecoration(
                labelText: 'Etiquetas (opcional)',
                helperText: 'Separadas por comas',
              ),
              validator: validarEtiquetas,
            ),
            const SizedBox(height: 18),
            Text('Contenido', style: tema.textTheme.labelLarge),
            const SizedBox(height: 6),
            SegmentedButton<TipoTutorial>(
              key: const Key('selectorTipoTutorial'),
              segments: const [
                ButtonSegment(
                  value: TipoTutorial.enlace,
                  icon: Icon(Icons.play_circle_outline),
                  label: Text('Video de YouTube'),
                ),
                ButtonSegment(
                  value: TipoTutorial.pdf,
                  icon: Icon(Icons.picture_as_pdf_outlined),
                  label: Text('PDF'),
                ),
              ],
              selected: {_tipo},
              onSelectionChanged: (s) => setState(() {
                _tipo = s.first;
                _errorPdf = null;
              }),
            ),
            const SizedBox(height: 12),
            if (_tipo == TipoTutorial.enlace)
              TextFormField(
                key: const Key('campoEnlaceTutorial'),
                controller: _enlace,
                keyboardType: TextInputType.url,
                decoration: const InputDecoration(
                  labelText: 'Enlace del video',
                  hintText: 'https://youtu.be/…',
                ),
                validator: validarEnlaceYoutube,
              )
            else
              _SelectorPdf(
                nombre:
                    _archivoNuevo?.nombre ??
                    (_original?.tipo == TipoTutorial.pdf
                        ? _original?.recursoNombre
                        : null),
                subiendo: _subiendo,
                error: _errorPdf,
                onElegir: ocupado ? null : _elegirPdf,
              ),
            if (_errorGeneral != null) ...[
              const SizedBox(height: 16),
              AvisoCockpit(texto: _errorGeneral!, tono: TonoAviso.error),
            ],
            const SizedBox(height: 24),
            FilledButton(
              key: const Key('botonGuardarTutorial'),
              onPressed: ocupado ? null : _guardar,
              child: _enviando
                  ? const IndicadorBoton()
                  : Text(editando ? 'Guardar cambios' : 'Crear tutorial'),
            ),
          ],
        ),
      ),
    );
  }
}

class _SelectorPdf extends StatelessWidget {
  final String? nombre;
  final bool subiendo;
  final String? error;
  final VoidCallback? onElegir;

  const _SelectorPdf({
    required this.nombre,
    required this.subiendo,
    required this.error,
    required this.onElegir,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        OutlinedButton.icon(
          key: const Key('botonElegirPdfTutorial'),
          onPressed: onElegir,
          icon: subiendo
              ? const IndicadorBoton()
              : const Icon(Icons.upload_file_outlined),
          label: Text(nombre == null ? 'Elegir PDF' : 'Cambiar PDF'),
        ),
        if (nombre != null)
          Padding(
            padding: const EdgeInsets.only(top: 6),
            child: Text(
              nombre!,
              key: const Key('nombrePdfTutorial'),
              style: tema.textTheme.bodySmall,
            ),
          ),
        if (error != null)
          Padding(
            padding: const EdgeInsets.only(top: 6),
            child: Text(
              error!,
              key: const Key('errorPdfTutorial'),
              style: tema.textTheme.bodySmall?.copyWith(
                color: tema.colorScheme.error,
              ),
            ),
          ),
      ],
    );
  }
}
