import 'dart:convert';

import 'package:flutter/services.dart';

/// Un parámetro de una acción del catálogo.
class ParametroAccion {
  final String nombre;
  final String tipo;
  final String descripcion;

  /// Los únicos valores permitidos, o `null` si es libre.
  final List<String>? valores;

  const ParametroAccion({required this.nombre, required this.tipo, required this.descripcion, this.valores});
}

/// Una acción del catálogo, con lo que se le explica al modelo (CU50).
class AccionDelCatalogo {
  final String nombre;
  final String descripcion;
  final Map<String, ParametroAccion> parametros;
  final List<String> requeridos;

  const AccionDelCatalogo({
    required this.nombre,
    required this.descripcion,
    required this.parametros,
    required this.requeridos,
  });

  /// Los parámetros de la función como JSON Schema.
  Map<String, dynamic> esquemaDeParametros() => {
    'type': 'object',
    'properties': {
      for (final p in parametros.values)
        p.nombre: {
          'type': p.tipo,
          if (p.valores != null) 'enum': p.valores,
          'description': p.descripcion,
        },
    },
    'required': requeridos,
  };

  /// La función en el formato de herramienta que recibe el modelo.
  Map<String, dynamic> comoHerramienta() => {
    'type': 'function',
    'function': {'name': nombre, 'description': descripcion, 'parameters': esquemaDeParametros()},
  };
}

/// Las acciones de la pantalla de Clases que el modelo puede elegir: `assets/ml/acciones_clases.json`.
class CatalogoAcciones {
  static const rutaAsset = 'assets/ml/acciones_clases.json';

  final Map<String, AccionDelCatalogo> acciones;

  const CatalogoAcciones(this.acciones);

  factory CatalogoAcciones.desdeJson(String texto) {
    final lista = (jsonDecode(texto) as Map<String, dynamic>)['acciones'] as List<dynamic>;
    final acciones = <String, AccionDelCatalogo>{};
    for (final item in lista.cast<Map<String, dynamic>>()) {
      final parametros = <String, ParametroAccion>{};
      for (final e in (item['parametros'] as Map<String, dynamic>).entries) {
        final p = e.value as Map<String, dynamic>;
        parametros[e.key] = ParametroAccion(
          nombre: e.key,
          tipo: p['type'] as String,
          descripcion: p['description'] as String,
          valores: (p['enum'] as List<dynamic>?)?.cast<String>(),
        );
      }
      final nombre = item['nombre'] as String;
      acciones[nombre] = AccionDelCatalogo(
        nombre: nombre,
        descripcion: item['descripcion'] as String,
        parametros: parametros,
        requeridos: (item['requeridos'] as List<dynamic>).cast<String>(),
      );
    }
    return CatalogoAcciones(acciones);
  }

  /// Lee el catálogo empaquetado en la app.
  static Future<CatalogoAcciones> cargar({AssetBundle? bundle}) async =>
      CatalogoAcciones.desdeJson(await (bundle ?? rootBundle).loadString(rutaAsset));

  /// Las funciones que se le pasan al modelo.
  List<Map<String, dynamic>> herramientas() => [for (final a in acciones.values) a.comoHerramienta()];
}
