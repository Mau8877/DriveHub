/// Qué pasó con la hora del estudiante al cancelar una clase (CU15).
///
/// * [devuelta]: la hora volvió a su billetera.
/// * [perdida]: cancelación tardía (menos del plazo): la franja se liberó pero la hora no se devuelve.
/// * [enProceso]: Pagos no respondió; la cancelación se aceptó y el sistema devuelve la hora solo cuando vuelva.
enum HoraDeCancelacion {
  devuelta('DEVUELTA'),
  perdida('PERDIDA'),
  enProceso('EN_PROCESO');

  /// El valor tal como viaja en la API.
  final String valorApi;
  const HoraDeCancelacion(this.valorApi);

  /// Un valor desconocido se trata como [enProceso]: es el caso prudente, no promete ni niega la hora.
  factory HoraDeCancelacion.fromJson(String valor) {
    for (final hora in values) {
      if (hora.valorApi == valor) return hora;
    }
    return HoraDeCancelacion.enProceso;
  }
}
