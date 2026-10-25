/// Estado de un pago (CU26): lo que informa `GET /pagos/{codigo}/estado`.
///
/// * `REVERTIDO` es un pago reembolsado.
/// * Un valor que esta versión de la app no conoce queda como [desconocido]: no rompe la pantalla, y quien
/// * lo recibe lo trata como "todavía sin novedades".
enum EstadoPago {
  pendiente('PENDIENTE'),
  confirmado('CONFIRMADO'),
  fallido('FALLIDO'),
  revertido('REVERTIDO'),
  desconocido('DESCONOCIDO');

  /// El valor tal como viaja en la API.
  final String valorApi;

  const EstadoPago(this.valorApi);

  factory EstadoPago.fromJson(String valor) {
    for (final estado in values) {
      if (estado.valorApi == valor) return estado;
    }
    return desconocido;
  }

  /// El pago ya no va a cambiar solo: o se acreditó, o falló, o se reembolsó.
  bool get esFinal => this == confirmado || this == fallido || this == revertido;
}
