/// El momento del día por el que el estudiante filtra las franjas (CU14).
///
/// * Se traduce a `filter[horaDesde]` (inclusiva) y `filter[horaHasta]` (exclusiva) sobre la hora de inicio. La
/// * noche no tiene tope: llega hasta el final del día.
enum MomentoDia {
  todos('Todos', null, null),
  manana('Mañana', '00:00', '12:00'),
  tarde('Tarde', '12:00', '18:00'),
  noche('Noche', '18:00', null);

  final String etiqueta;
  final String? horaDesde;
  final String? horaHasta;

  const MomentoDia(this.etiqueta, this.horaDesde, this.horaHasta);
}
