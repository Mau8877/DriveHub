import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/network/api_client.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/widgets/avatar_usuario.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../models/perfil.dart';
import '../providers/perfil_provider.dart';
import '../validators/foto_validators.dart';
import '../validators/perfil_validators.dart';

/// Lo que se puede hacer con la foto desde la hoja de opciones.
enum _AccionFoto { galeria, camara, quitar }

/// Cabecera del perfil (CU06): la foto con su anillo ámbar y el botón de cámara, el nombre y la
/// escuela. Elegir de la galería o la cámara, ver una vista previa, guardar y quitar.
///
/// * Revisa la imagen antes de enviarla solo para dar feedback inmediato (formato, peso y
/// * dimensiones); el backend decide. Una imagen rechazada nunca se envía.
/// * Se pide `maxWidth`/`maxHeight` de 4000 al selector para que la cámara no genere imágenes mayores.
class FotoPerfilSeccion extends ConsumerStatefulWidget {
  final Perfil perfil;

  const FotoPerfilSeccion({super.key, required this.perfil});

  @override
  ConsumerState<FotoPerfilSeccion> createState() => _FotoPerfilSeccionState();
}

class _Seleccion {
  final Uint8List bytes;
  final String nombre;
  final String tipo;

  const _Seleccion(this.bytes, this.nombre, this.tipo);
}

class _FotoPerfilSeccionState extends ConsumerState<FotoPerfilSeccion> {
  final _selector = ImagePicker();
  _Seleccion? _seleccion;
  bool _ocupado = false;
  String? _error;

  Future<void> _abrirOpciones() async {
    final tieneFoto = widget.perfil.fotoUrl != null;
    final accion = await showModalBottomSheet<_AccionFoto>(
      context: context,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 0, 24, 8),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  'Foto de perfil',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ),
            ),
            ListTile(
              leading: const Icon(Icons.photo_library_outlined),
              title: const Text('Galería'),
              subtitle: const Text('Elegir una foto guardada'),
              onTap: () => Navigator.pop(context, _AccionFoto.galeria),
            ),
            ListTile(
              leading: const Icon(Icons.photo_camera_outlined),
              title: const Text('Cámara'),
              subtitle: const Text('Tomar una foto ahora'),
              onTap: () => Navigator.pop(context, _AccionFoto.camara),
            ),
            if (tieneFoto)
              ListTile(
                leading: Icon(
                  Icons.delete_outline,
                  color: Theme.of(context).colorScheme.error,
                ),
                title: Text(
                  'Quitar foto',
                  style: TextStyle(color: Theme.of(context).colorScheme.error),
                ),
                onTap: () => Navigator.pop(context, _AccionFoto.quitar),
              ),
          ],
        ),
      ),
    );
    switch (accion) {
      case null:
        return;
      case _AccionFoto.galeria:
        return _elegir(ImageSource.gallery);
      case _AccionFoto.camara:
        return _elegir(ImageSource.camera);
      case _AccionFoto.quitar:
        return _quitar();
    }
  }

  Future<void> _elegir(ImageSource origen) async {
    final archivo = await _selector.pickImage(
      source: origen,
      maxWidth: fotoMaxLadoPx.toDouble(),
      maxHeight: fotoMaxLadoPx.toDouble(),
    );
    if (archivo == null) return;

    final bytes = await archivo.readAsBytes();
    final problema = await _revisar(archivo.name, bytes);
    if (!mounted) return;
    setState(() {
      _error = problema;
      _seleccion = problema == null
          ? _Seleccion(bytes, archivo.name, tipoDeImagen(archivo.name)!)
          : null;
    });
  }

  Future<String?> _revisar(String nombre, Uint8List bytes) async {
    if (tipoDeImagen(nombre) == null) return mensajeFotoFormato;
    try {
      final medidas = await leerDimensiones(bytes);
      return validarFoto(
        nombre: nombre,
        bytes: bytes.length,
        ancho: medidas.ancho,
        alto: medidas.alto,
      );
    } catch (_) {
      return mensajeFotoIlegible;
    }
  }

  Future<void> _guardar() async {
    final seleccion = _seleccion;
    if (seleccion == null) return;
    final mensajes = ScaffoldMessenger.of(context);
    setState(() {
      _ocupado = true;
      _error = null;
    });
    try {
      await ref
          .read(perfilProvider.notifier)
          .subirFoto(
            seleccion.bytes,
            nombre: seleccion.nombre,
            tipo: seleccion.tipo,
          );
      mensajes.showSnackBar(const SnackBar(content: Text('Foto actualizada')));
      if (mounted) setState(() => _seleccion = null);
    } on ApiException catch (e) {
      // * La foto actual sigue como estaba; se conserva la elegida para reintentar o cancelar
      if (mounted) setState(() => _error = mensajeDeCodigo(e.code));
    } catch (_) {
      if (mounted) setState(() => _error = mensajeGenerico);
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  Future<void> _quitar() async {
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('¿Quitar tu foto?'),
        content: const Text('Tu cuenta volverá a mostrar tus iniciales.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('No'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Sí, quitar'),
          ),
        ],
      ),
    );
    if (confirmado != true || !mounted) return;

    final mensajes = ScaffoldMessenger.of(context);
    setState(() {
      _ocupado = true;
      _error = null;
    });
    try {
      await ref.read(perfilProvider.notifier).quitarFoto();
      mensajes.showSnackBar(const SnackBar(content: Text('Foto eliminada')));
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = mensajeDeCodigo(e.code));
    } catch (_) {
      if (mounted) setState(() => _error = mensajeGenerico);
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final p = widget.perfil;
    final seleccion = _seleccion;
    final tema = Theme.of(context);

    return Column(
      children: [
        Stack(
          clipBehavior: Clip.none,
          children: [
            // * Anillo ámbar alrededor de la foto, separado por un aro del color de fondo
            Container(
              padding: const EdgeInsets.all(4),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(color: AppColors.ambar, width: 2.5),
              ),
              child: seleccion != null
                  ? ClipOval(
                      child: Image.memory(
                        seleccion.bytes,
                        width: 104,
                        height: 104,
                        fit: BoxFit.cover,
                      ),
                    )
                  : AvatarUsuario(
                      nombres: p.nombres,
                      apellidos: p.apellidos,
                      fotoUrl: p.fotoUrl,
                      radio: 52,
                    ),
            ),
            Positioned(
              right: -2,
              bottom: -2,
              child: Material(
                color: tema.colorScheme.primary,
                shape: CircleBorder(
                  side: BorderSide(
                    color: tema.scaffoldBackgroundColor,
                    width: 3,
                  ),
                ),
                child: IconButton(
                  tooltip: p.fotoUrl == null ? 'Subir foto' : 'Cambiar foto',
                  icon: const Icon(Icons.photo_camera_outlined, size: 18),
                  color: tema.colorScheme.onPrimary,
                  onPressed: (_ocupado || seleccion != null)
                      ? null
                      : _abrirOpciones,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 14),
        Text(
          '${p.nombres} ${p.apellidos}',
          style: tema.textTheme.titleLarge,
          textAlign: TextAlign.center,
        ),
        if (p.escuelaNombre != null)
          Text(p.escuelaNombre!, style: tema.textTheme.bodySmall),
        const SizedBox(height: 12),
        if (seleccion != null)
          Wrap(
            spacing: 8,
            children: [
              SizedBox(
                width: 150,
                child: FilledButton(
                  onPressed: _ocupado ? null : _guardar,
                  child: _ocupado
                      ? const IndicadorBoton()
                      : const Text('Guardar foto'),
                ),
              ),
              SizedBox(
                width: 130,
                child: OutlinedButton(
                  onPressed: _ocupado
                      ? null
                      : () => setState(() => _seleccion = null),
                  child: const Text('Cancelar'),
                ),
              ),
            ],
          )
        else
          Text(
            'JPG, PNG o WebP · hasta 5 MB · máx. 4000×4000 px',
            style: tema.textTheme.bodySmall,
            textAlign: TextAlign.center,
          ),
        if (_error != null)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(
              _error!,
              style: TextStyle(color: tema.colorScheme.error),
              textAlign: TextAlign.center,
            ),
          ),
      ],
    );
  }
}
