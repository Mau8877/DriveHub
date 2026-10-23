/// Estado de un horario semanal (CU12). Pausar y reanudar se pueden repetir; desactivar es definitivo.
enum EstadoPlantilla {
  activa('ACTIVA'),
  pausada('PAUSADA'),
  inactiva('INACTIVA');

  /// El valor tal como viaja en la API.
  final String valorApi;

  const EstadoPlantilla(this.valorApi);

  /// Un valor desconocido lanza [FormatException]: mejor fallar que mostrar un estado inventado.
  factory EstadoPlantilla.fromJson(String valor) {
    for (final estado in values) {
      if (estado.valorApi == valor) return estado;
    }
    throw FormatException('Estado de horario desconocido: $valor');
  }
}
