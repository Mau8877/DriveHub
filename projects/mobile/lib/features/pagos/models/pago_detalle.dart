import 'pago_resumen.dart';
import 'reembolso_previsto.dart';

/// El detalle de una compra (CU28): `GET /pagos/{codigo}`.
///
/// * [reembolsable] lo decide el backend; la app solo lo muestra. [reembolso] es `null` si no lo es.
/// * [horasRestantes] y [confirmadoEn] son `null` mientras el pago no se confirmó.
class PagoDetalle extends PagoResumen {
  final int? horasRestantes;
  final DateTime? confirmadoEn;
  final bool reembolsable;
  final ReembolsoPrevisto? reembolso;

  const PagoDetalle({
    required super.codigo,
    required super.planNombre,
    required super.horas,
    required super.montoUsd,
    required super.montoBs,
    required super.estado,
    required super.creadoEn,
    this.horasRestantes,
    this.confirmadoEn,
    required this.reembolsable,
    this.reembolso,
  });

  factory PagoDetalle.fromJson(Map<String, dynamic> json) {
    final resumen = PagoResumen.fromJson(json);
    final horasRestantes = json['horasRestantes'] as num?;
    final confirmadoEn = json['confirmadoEn'] as String?;
    final reembolso = json['reembolso'] as Map<String, dynamic>?;
    return PagoDetalle(
      codigo: resumen.codigo,
      planNombre: resumen.planNombre,
      horas: resumen.horas,
      montoUsd: resumen.montoUsd,
      montoBs: resumen.montoBs,
      estado: resumen.estado,
      creadoEn: resumen.creadoEn,
      horasRestantes: horasRestantes?.toInt(),
      confirmadoEn: confirmadoEn == null
          ? null
          : DateTime.parse(confirmadoEn).toLocal(),
      reembolsable: json['reembolsable'] as bool? ?? false,
      reembolso: reembolso == null ? null : ReembolsoPrevisto.fromJson(reembolso),
    );
  }
}
