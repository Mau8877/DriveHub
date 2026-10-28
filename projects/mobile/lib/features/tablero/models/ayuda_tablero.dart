import 'luz_tablero.dart';

/// Una luz guardada en el historial de una ayuda (CU40, CU42).
class IndicadorGuardado {
  final String codigo;
  final String nombre;
  final double confianza;
  final SeveridadTablero severidad;

  const IndicadorGuardado({
    required this.codigo,
    required this.nombre,
    required this.confianza,
    required this.severidad,
  });

  factory IndicadorGuardado.fromJson(Map<String, dynamic> json) =>
      IndicadorGuardado(
        codigo: json['codigo'] as String,
        nombre: json['nombre'] as String,
        confianza: (json['confianza'] as num).toDouble(),
        severidad: SeveridadTablero.fromJson(json['severidad'] as String),
      );
}

/// Una ayuda del tablero guardada en el servidor (CU40 al crearla, CU42 en el historial).
class AyudaTablero {
  final String codigo;
  final List<IndicadorGuardado> indicadores;
  final String respuesta;
  final String modelo;
  final int latenciaMs;

  /// La miniatura de la foto, si se guardó.
  final String? imagenUrl;

  /// Si al estudiante le sirvió la ayuda; `null` = no opinó.
  final bool? util;
  final DateTime creadaEn;

  const AyudaTablero({
    required this.codigo,
    required this.indicadores,
    required this.respuesta,
    required this.modelo,
    required this.latenciaMs,
    this.imagenUrl,
    this.util,
    required this.creadaEn,
  });

  factory AyudaTablero.fromJson(Map<String, dynamic> json) => AyudaTablero(
    codigo: json['codigo'] as String,
    indicadores: (json['indicadores'] as List<dynamic>)
        .map((i) => IndicadorGuardado.fromJson(i as Map<String, dynamic>))
        .toList(),
    respuesta: json['respuesta'] as String,
    modelo: json['modelo'] as String,
    latenciaMs: json['latenciaMs'] as int,
    imagenUrl: json['imagenUrl'] as String?,
    util: json['util'] as bool?,
    creadaEn: DateTime.parse(json['creadaEn'] as String),
  );

  /// La luz más probable, la que se muestra como título de la ayuda.
  IndicadorGuardado? get principal =>
      indicadores.isEmpty ? null : indicadores.first;
}
