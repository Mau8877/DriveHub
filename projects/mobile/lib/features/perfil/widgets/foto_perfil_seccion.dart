import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/network/api_client.dart';
import '../../../shared/widgets/avatar_usuario.dart';
import '../models/perfil.dart';
import '../providers/perfil_provider.dart';
import '../validators/foto_validators.dart';
import '../validators/perfil_validators.dart';

/// Sección de la foto del perfil (CU06): elegir de la galería o la cámara, ver una vista previa,
/// guardar y quitar.
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

  Future<void> _elegir() async {
    final origen = await showModalBottomSheet<ImageSource>(
      context: context,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.photo_library_outlined),
              title: const Text('Galería'),
              onTap: () => Navigator.pop(context, ImageSource.gallery),
            ),
            ListTile(
              leading: const Icon(Icons.photo_camera_outlined),
              title: const Text('Cámara'),
              onTap: () => Navigator.pop(context, ImageSource.camera),
            ),
          ],
        ),
      ),
    );
    if (origen == null) return;

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

    return Column(
      children: [
        if (seleccion != null)
          ClipOval(
            child: Image.memory(
              seleccion.bytes,
              width: 112,
              height: 112,
              fit: BoxFit.cover,
            ),
          )
        else
          AvatarUsuario(
            nombres: p.nombres,
            apellidos: p.apellidos,
            fotoUrl: p.fotoUrl,
            radio: 56,
          ),
        const SizedBox(height: 12),
        if (seleccion == null)
          Wrap(
            spacing: 8,
            children: [
              OutlinedButton(
                onPressed: _ocupado ? null : _elegir,
                child: Text(p.fotoUrl == null ? 'Subir foto' : 'Cambiar foto'),
              ),
              if (p.fotoUrl != null)
                OutlinedButton(
                  onPressed: _ocupado ? null : _quitar,
                  child: const Text('Quitar foto'),
                ),
            ],
          )
        else
          Wrap(
            spacing: 8,
            children: [
              FilledButton(
                onPressed: _ocupado ? null : _guardar,
                child: _ocupado
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text('Guardar foto'),
              ),
              OutlinedButton(
                onPressed: _ocupado
                    ? null
                    : () => setState(() => _seleccion = null),
                child: const Text('Cancelar'),
              ),
            ],
          ),
        if (_error != null)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(
              _error!,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
              textAlign: TextAlign.center,
            ),
          ),
      ],
    );
  }
}
