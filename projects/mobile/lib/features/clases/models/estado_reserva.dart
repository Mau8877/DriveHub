/// Estado de una reserva tal como lo ve el estudiante (CU14).
///
/// * `PENDIENTE_PAGO` no existe acá: el backend no lo muestra al estudiante.
enum EstadoReserva {
  confirmada('CONFIRMADA'),
  completada('COMPLETADA'),
  noAsistio('NO_ASISTIO'),
  cancelada('CANCELADA');

  /// El valor tal como viaja en la API.
  final String valorApi;

  const EstadoReserva(this.valorApi);

  /// Un valor desconocido se trata como [confirmada]: un estado nuevo del backend no rompe la lista.
  factory EstadoReserva.fromJson(String valor) {
    for (final estado in values) {
      if (estado.valorApi == valor) return estado;
    }
    return EstadoReserva.confirmada;
  }
}
