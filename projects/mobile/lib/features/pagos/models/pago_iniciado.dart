import 'estado_pago.dart';

/// La compra recién iniciada (CU26): `POST /pagos`.
///
/// * [urlPago] es la página de pago de la pasarela; es nula si el pago ya no admite pagarse (vencido o fallido).
/// * [montoUsd] es lo que se cobra y [montoBs] el costo del plan, como referencia. Solo para mostrar.
class PagoIniciado {
  final String codigo;
  final EstadoPago estado;
  final String? urlPago;
  final DateTime expiraEn;
  final String planNombre;
  final int horas;
  final double montoUsd;
  final double montoBs;

  const PagoIniciado({
    required this.codigo,
    required this.estado,
    required this.urlPago,
    required this.expiraEn,
    required this.planNombre,
    required this.horas,
    required this.montoUsd,
    required this.montoBs,
  });

  factory PagoIniciado.fromJson(Map<String, dynamic> json) => PagoIniciado(
    codigo: json['codigo'] as String,
    estado: EstadoPago.fromJson(json['estado'] as String),
    urlPago: json['urlPago'] as String?,
    expiraEn: DateTime.parse(json['expiraEn'] as String),
    planNombre: json['planNombre'] as String,
    horas: (json['horas'] as num).toInt(),
    montoUsd: (json['montoUsd'] as num).toDouble(),
    montoBs: (json['montoBs'] as num).toDouble(),
  );
}
