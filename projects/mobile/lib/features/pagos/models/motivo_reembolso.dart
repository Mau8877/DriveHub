/// Por qué el estudiante pide el reembolso de una compra (CU31): la lista cerrada del backend.
///
/// * [valorApi] es el valor que viaja en la API; [texto], lo que ve la persona.
/// * Con [otro] la persona cuenta el motivo con sus palabras (de 5 a 200 caracteres).
enum MotivoReembolso {
  cambioDePlanes('CAMBIO_DE_PLANES', 'Cambié de planes'),
  mudanza('MUDANZA', 'Me mudé de ciudad'),
  sinTiempo('SIN_TIEMPO', 'No tengo tiempo para las clases'),
  compraPorError('COMPRA_POR_ERROR', 'Compré el plan por error'),
  problemasEconomicos('PROBLEMAS_ECONOMICOS', 'Tengo problemas económicos'),
  insatisfecho('INSATISFECHO', 'No quedé conforme con el servicio'),
  otro('OTRO', 'Otro');

  final String valorApi;
  final String texto;

  const MotivoReembolso(this.valorApi, this.texto);

  /// Largo mínimo y máximo de lo que se escribe con [otro].
  static const detalleMinimo = 5;
  static const detalleMaximo = 200;

  /// Si [detalle], ya recortado, tiene un largo que el backend acepta.
  static bool detalleValido(String detalle) {
    final largo = detalle.trim().length;
    return largo >= detalleMinimo && largo <= detalleMaximo;
  }
}
