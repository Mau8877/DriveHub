import 'package:flutter/foundation.dart';

import 'preferencias_notificacion.dart';

/// Un aviso de la bandeja de la persona (CU38): lo que devuelve `/notificaciones`.
///
/// * [tipo] es `null` si el servidor manda uno que esta versión de la app no conoce: se muestra como «Aviso»
///   y no rompe el listado.
/// * Inmutable: marcarlo como leído crea una copia ([copiar]).
@immutable
class Notificacion {
  final String codigoNotificacion;
  final TipoNotificacion? tipo;
  final String titulo;
  final String mensaje;
  final bool leida;
  final DateTime creadaEn;

  const Notificacion({
    required this.codigoNotificacion,
    required this.tipo,
    required this.titulo,
    required this.mensaje,
    required this.leida,
    required this.creadaEn,
  });

  factory Notificacion.fromJson(Map<String, dynamic> json) => Notificacion(
    codigoNotificacion: json['codigoNotificacion'] as String,
    tipo: _tipoDe(json['tipo'] as String?),
    titulo: json['titulo'] as String,
    mensaje: json['mensaje'] as String,
    leida: json['leida'] as bool,
    creadaEn: DateTime.parse(json['creadaEn'] as String).toLocal(),
  );

  /// El nombre del tipo para mostrar; «Aviso» si es uno desconocido.
  String get etiquetaTipo => tipo?.titulo ?? 'Aviso';

  Notificacion copiar({bool? leida}) => Notificacion(
    codigoNotificacion: codigoNotificacion,
    tipo: tipo,
    titulo: titulo,
    mensaje: mensaje,
    leida: leida ?? this.leida,
    creadaEn: creadaEn,
  );

  @override
  bool operator ==(Object other) =>
      other is Notificacion &&
      other.codigoNotificacion == codigoNotificacion &&
      other.tipo == tipo &&
      other.titulo == titulo &&
      other.mensaje == mensaje &&
      other.leida == leida &&
      other.creadaEn == creadaEn;

  @override
  int get hashCode =>
      Object.hash(codigoNotificacion, tipo, titulo, mensaje, leida, creadaEn);

  static TipoNotificacion? _tipoDe(String? valor) {
    for (final t in TipoNotificacion.values) {
      if (t.valorApi == valor) return t;
    }
    return null;
  }
}
