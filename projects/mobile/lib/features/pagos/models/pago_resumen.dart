import 'estado_pago_visible.dart';

/// Una compra del historial (CU28): un elemento de `GET /pagos`.
///
/// * Los montos son solo para mostrar (la app no suma ni compara dinero): el backend manda.
class PagoResumen {
  final String codigo;
  final String planNombre;
  final int horas;
  final double montoUsd;
  final double montoBs;
  final EstadoPagoVisible estado;
  final DateTime creadoEn;

  const PagoResumen({
    required this.codigo,
    required this.planNombre,
    required this.horas,
    required this.montoUsd,
    required this.montoBs,
    required this.estado,
    required this.creadoEn,
  });

  factory PagoResumen.fromJson(Map<String, dynamic> json) => PagoResumen(
    codigo: json['codigo'] as String,
    planNombre: json['planNombre'] as String,
    horas: (json['horas'] as num).toInt(),
    montoUsd: (json['montoUsd'] as num).toDouble(),
    montoBs: (json['montoBs'] as num).toDouble(),
    estado: EstadoPagoVisible.fromJson(json['estado'] as String),
    creadoEn: DateTime.parse(json['creadoEn'] as String).toLocal(),
  );
}
