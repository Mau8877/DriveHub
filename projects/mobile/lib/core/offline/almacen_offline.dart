import 'dart:convert';
import 'dart:io';

import 'package:path_provider/path_provider.dart';

/// Dónde se guarda en el teléfono lo que hace falta para funcionar sin internet: las lecturas ya hechas
/// (caché), la cola de acciones pendientes y las que el servidor rechazó.
///
/// * Cada valor se identifica por un `espacio` (una persona: así dos cuentas en el mismo teléfono no se ven
/// * entre sí) y una `clave`. Los valores son JSON (mapas, listas, textos, números).
abstract class AlmacenOffline {
  Future<Object?> leer(String espacio, String clave);
  Future<void> escribir(String espacio, String clave, Object valor);
  Future<void> borrar(String espacio, String clave);
  Future<void> borrarEspacio(String espacio);
}

/// Implementación en archivos dentro de la carpeta privada de la app: `offline/{espacio}/{hash}.json`.
///
/// * El nombre del archivo es un hash de la clave (las claves son URLs largas); el archivo guarda también la clave
/// * para descartar una colisión.
/// * Se escribe en un archivo temporal y se renombra: un corte a mitad de escritura no deja un JSON a medias.
class AlmacenEnArchivos implements AlmacenOffline {
  final Future<Directory> Function() _raiz;

  AlmacenEnArchivos({Future<Directory> Function()? raiz})
    : _raiz = raiz ?? _raizPorDefecto;

  static Future<Directory> _raizPorDefecto() async {
    final base = await getApplicationSupportDirectory();
    return Directory('${base.path}${Platform.pathSeparator}offline');
  }

  Future<Directory> _carpeta(String espacio) async {
    final raiz = await _raiz();
    final carpeta = Directory('${raiz.path}${Platform.pathSeparator}${_seguro(espacio)}');
    if (!await carpeta.exists()) await carpeta.create(recursive: true);
    return carpeta;
  }

  Future<File> _archivo(String espacio, String clave) async {
    final carpeta = await _carpeta(espacio);
    return File('${carpeta.path}${Platform.pathSeparator}${_hash(clave)}.json');
  }

  @override
  Future<Object?> leer(String espacio, String clave) async {
    try {
      final archivo = await _archivo(espacio, clave);
      if (!await archivo.exists()) return null;
      final contenido = jsonDecode(await archivo.readAsString()) as Map<String, dynamic>;
      return contenido['k'] == clave ? contenido['v'] : null;
    } catch (_) {
      // * Un archivo corrupto equivale a no tener caché: nunca rompe una pantalla
      return null;
    }
  }

  @override
  Future<void> escribir(String espacio, String clave, Object valor) async {
    final archivo = await _archivo(espacio, clave);
    final temporal = File('${archivo.path}.tmp');
    await temporal.writeAsString(jsonEncode({'k': clave, 'v': valor}), flush: true);
    await temporal.rename(archivo.path);
  }

  @override
  Future<void> borrar(String espacio, String clave) async {
    final archivo = await _archivo(espacio, clave);
    if (await archivo.exists()) await archivo.delete();
  }

  @override
  Future<void> borrarEspacio(String espacio) async {
    final raiz = await _raiz();
    final carpeta = Directory('${raiz.path}${Platform.pathSeparator}${_seguro(espacio)}');
    if (await carpeta.exists()) await carpeta.delete(recursive: true);
  }

  /// FNV-1a de 64 bits en hexadecimal: estable entre ejecuciones (`hashCode` de Dart no lo es).
  static String _hash(String texto) {
    var h = BigInt.parse('cbf29ce484222325', radix: 16);
    final primo = BigInt.parse('100000001b3', radix: 16);
    final mascara = (BigInt.one << 64) - BigInt.one;
    for (final byte in utf8.encode(texto)) {
      h = ((h ^ BigInt.from(byte)) * primo) & mascara;
    }
    return h.toRadixString(16).padLeft(16, '0');
  }

  static String _seguro(String espacio) => espacio.replaceAll(RegExp(r'[^A-Za-z0-9_-]'), '_');
}

/// Almacén en memoria, para los tests.
class AlmacenEnMemoria implements AlmacenOffline {
  final Map<String, Map<String, String>> _datos = {};

  @override
  Future<Object?> leer(String espacio, String clave) async {
    final texto = _datos[espacio]?[clave];
    return texto == null ? null : jsonDecode(texto);
  }

  @override
  Future<void> escribir(String espacio, String clave, Object valor) async {
    (_datos[espacio] ??= {})[clave] = jsonEncode(valor);
  }

  @override
  Future<void> borrar(String espacio, String clave) async => _datos[espacio]?.remove(clave);

  @override
  Future<void> borrarEspacio(String espacio) async => _datos.remove(espacio);
}
