import 'package:flutter/foundation.dart';

/// Qué originó un aviso (CU36). El valor de la API es el que viaja en el JSON.
enum TipoNotificacion {
  reserva(
    'RESERVA',
    'Clases y reservas',
    'Cuando reservas, cambias o cancelas una clase',
  ),
  pago('PAGO', 'Pagos', 'Tus compras de horas y reembolsos'),
  examen('EXAMEN', 'Exámenes', 'Fechas y resultados de tus exámenes'),
  recordatorio(
    'RECORDATORIO',
    'Recordatorios',
    'Avisos antes de tus clases y pendientes',
  );

  final String valorApi;
  final String titulo;
  final String detalle;

  const TipoNotificacion(this.valorApi, this.titulo, this.detalle);
}

/// Las preferencias de aviso de la persona (CU37): lo que devuelve y recibe `/preferencias-notificacion`.
///
/// * Canales y tipos son independientes: apagar el correo no apaga los pagos, y viceversa.
/// * El silencio solo frena las notificaciones push; son las dos horas (`HH:mm`, hora de Bolivia) o ninguna.
/// * Inmutable: cada cambio crea una copia (`conCanal`, `conTipo`, `conSilencio`).
@immutable
class PreferenciasNotificacion {
  final bool correo;
  final bool push;
  final bool enApp;
  final Map<TipoNotificacion, bool> tipos;
  final String? silencioDesde;
  final String? silencioHasta;

  const PreferenciasNotificacion({
    required this.correo,
    required this.push,
    required this.enApp,
    required this.tipos,
    this.silencioDesde,
    this.silencioHasta,
  });

  /// Lo que rige si la persona nunca configuró nada: todo activado y sin silencio.
  factory PreferenciasNotificacion.porDefecto() => PreferenciasNotificacion(
    correo: true,
    push: true,
    enApp: true,
    tipos: {for (final t in TipoNotificacion.values) t: true},
  );

  factory PreferenciasNotificacion.fromJson(Map<String, dynamic> json) {
    final canales = json['canales'] as Map<String, dynamic>;
    final tipos = json['tipos'] as Map<String, dynamic>;
    return PreferenciasNotificacion(
      correo: canales['email'] as bool,
      push: canales['push'] as bool,
      enApp: canales['inApp'] as bool,
      tipos: {
        for (final t in TipoNotificacion.values)
          // * Un tipo que el servidor no mande se toma como activado, igual que el por defecto
          t: (tipos[t.valorApi] as bool?) ?? true,
      },
      silencioDesde: json['silencioDesde'] as String?,
      silencioHasta: json['silencioHasta'] as String?,
    );
  }

  /// El cuerpo del `PUT`: siempre la configuración completa. Sin franja se mandan `null`, que el servicio
  /// interpreta como «sin silencio».
  Map<String, dynamic> toJson({String? email}) => {
    'canales': {'email': correo, 'push': push, 'inApp': enApp},
    'tipos': {for (final t in TipoNotificacion.values) t.valorApi: tipos[t]},
    'silencioDesde': silencioDesde,
    'silencioHasta': silencioHasta,
    'email': email,
  };

  bool get tieneSilencio => silencioDesde != null || silencioHasta != null;

  PreferenciasNotificacion conCanal({bool? correo, bool? push, bool? enApp}) =>
      PreferenciasNotificacion(
        correo: correo ?? this.correo,
        push: push ?? this.push,
        enApp: enApp ?? this.enApp,
        tipos: tipos,
        silencioDesde: silencioDesde,
        silencioHasta: silencioHasta,
      );

  PreferenciasNotificacion conTipo(TipoNotificacion tipo, bool activo) =>
      PreferenciasNotificacion(
        correo: correo,
        push: push,
        enApp: enApp,
        tipos: {...tipos, tipo: activo},
        silencioDesde: silencioDesde,
        silencioHasta: silencioHasta,
      );

  /// Reemplaza las dos horas a la vez; con `null` en las dos, quita el silencio.
  PreferenciasNotificacion conSilencio({String? desde, String? hasta}) =>
      PreferenciasNotificacion(
        correo: correo,
        push: push,
        enApp: enApp,
        tipos: tipos,
        silencioDesde: desde,
        silencioHasta: hasta,
      );

  @override
  bool operator ==(Object other) =>
      other is PreferenciasNotificacion &&
      other.correo == correo &&
      other.push == push &&
      other.enApp == enApp &&
      mapEquals(other.tipos, tipos) &&
      other.silencioDesde == silencioDesde &&
      other.silencioHasta == silencioHasta;

  @override
  int get hashCode => Object.hash(
    correo,
    push,
    enApp,
    Object.hashAll([for (final t in TipoNotificacion.values) tipos[t]]),
    silencioDesde,
    silencioHasta,
  );
}
