/// Las dos listas de clases del estudiante (CU14): las que están por venir y el historial.
enum VistaReservas {
  proximas('proximas'),
  historial('historial');

  /// El valor tal como viaja en `filter[vista]`.
  final String valorApi;

  const VistaReservas(this.valorApi);
}
