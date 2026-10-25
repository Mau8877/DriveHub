/// Qué documenta un comprobante (CU30): la compra, o el reembolso de esa compra.
enum TipoComprobante {
  pago('PAGO', 'Comprobante de pago'),
  reembolso('REEMBOLSO', 'Comprobante de reembolso'),
  desconocido('DESCONOCIDO', 'Comprobante');

  final String valorApi;

  /// El texto que ve la persona.
  final String etiqueta;

  const TipoComprobante(this.valorApi, this.etiqueta);

  /// Un tipo que esta versión de la app no conoce queda como [desconocido]: no rompe la pantalla.
  factory TipoComprobante.fromJson(String? valor) {
    for (final tipo in values) {
      if (tipo.valorApi == valor) return tipo;
    }
    return desconocido;
  }
}

/// Un comprobante de una compra (CU30), tal como lo informa el detalle del pago.
///
/// * [disponible] es falso mientras su PDF se prepara: todavía no se puede descargar.
class ComprobanteResumen {
  final String codigo;
  final TipoComprobante tipo;
  final String numero;
  final bool disponible;

  const ComprobanteResumen({
    required this.codigo,
    required this.tipo,
    required this.numero,
    required this.disponible,
  });

  factory ComprobanteResumen.fromJson(Map<String, dynamic> json) =>
      ComprobanteResumen(
        codigo: json['codigo'] as String,
        tipo: TipoComprobante.fromJson(json['tipo'] as String?),
        numero: json['numero'] as String,
        disponible: json['disponible'] as bool? ?? false,
      );
}
