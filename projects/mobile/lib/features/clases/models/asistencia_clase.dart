/// Si el estudiante asistió a una clase, tal como la registra el instructor al cerrarla (CU17).
enum AsistenciaClase {
  presente('PRESENTE', 'Presente'),
  tarde('TARDE', 'Tarde'),
  ausente('AUSENTE', 'Ausente');

  /// El valor tal como viaja en la API.
  final String valorApi;

  /// El nombre en español para mostrar.
  final String etiqueta;

  const AsistenciaClase(this.valorApi, this.etiqueta);

  /// Un valor desconocido se trata como [presente]: el caso que no insinúa que el estudiante perdió su hora.
  factory AsistenciaClase.fromJson(String valor) {
    for (final asistencia in values) {
      if (asistencia.valorApi == valor) return asistencia;
    }
    return AsistenciaClase.presente;
  }
}
