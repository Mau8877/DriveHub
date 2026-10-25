/// El estado de una compra tal como lo ve el estudiante (CU28): lo que informa `GET /pagos`.
///
/// * `REEMBOLSO_EN_PROCESO` y `REEMBOLSADO` no son estados guardados del pago: los deriva el backend.
/// * Un valor que esta versión de la app no conoce queda como [desconocido]: no rompe la pantalla.
enum EstadoPagoVisible {
  pendiente('PENDIENTE', 'Pendiente'),
  confirmado('CONFIRMADO', 'Confirmado'),
  fallido('FALLIDO', 'Fallido'),
  reembolsoEnProceso('REEMBOLSO_EN_PROCESO', 'Reembolso en proceso'),
  reembolsado('REEMBOLSADO', 'Reembolsado'),
  desconocido('DESCONOCIDO', 'Sin estado');

  /// El valor tal como viaja en la API.
  final String valorApi;

  /// El texto que ve el estudiante.
  final String etiqueta;

  const EstadoPagoVisible(this.valorApi, this.etiqueta);

  factory EstadoPagoVisible.fromJson(String valor) {
    for (final estado in values) {
      if (estado.valorApi == valor) return estado;
    }
    return desconocido;
  }
}
