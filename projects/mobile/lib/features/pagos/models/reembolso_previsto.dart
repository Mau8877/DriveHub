/// Cuánto recibiría el estudiante si pide el reembolso de una compra (CU28), en dólares.
class ReembolsoPrevisto {
  final double montoDevuelto;
  final double montoRetenido;
  final int porcentajeRetenido;

  const ReembolsoPrevisto({
    required this.montoDevuelto,
    required this.montoRetenido,
    required this.porcentajeRetenido,
  });

  factory ReembolsoPrevisto.fromJson(Map<String, dynamic> json) =>
      ReembolsoPrevisto(
        montoDevuelto: (json['montoDevuelto'] as num).toDouble(),
        montoRetenido: (json['montoRetenido'] as num).toDouble(),
        porcentajeRetenido: (json['porcentajeRetenido'] as num).toInt(),
      );
}
