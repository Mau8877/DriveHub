/// Las acciones que el asistente de voz sabe hacer en la pantalla de Clases (CU50).
///
/// * El [nombre] es el de la función en `assets/ml/acciones_clases.json`, el mismo con el que se entrenó el modelo.
/// * Un test compara este enum con el asset: una acción nueva se agrega a los dos y al entrenamiento.
enum NombreAccion {
  verMisClases('ver_mis_clases'),
  verHistorial('ver_historial'),
  buscarHorarios('buscar_horarios'),
  seleccionarHorario('seleccionar_horario'),
  limpiarFiltros('limpiar_filtros'),
  cancelarClase('cancelar_clase'),
  reprogramarClase('reprogramar_clase'),
  verHorasDisponibles('ver_horas_disponibles'),
  fueraDeAlcance('fuera_de_alcance');

  /// El nombre de la función tal como lo devuelve el modelo.
  final String nombre;

  const NombreAccion(this.nombre);

  /// La acción con ese nombre, o `null` si no es del catálogo.
  static NombreAccion? desdeNombre(String nombre) {
    for (final accion in values) {
      if (accion.nombre == nombre) return accion;
    }
    return null;
  }

  /// Las que cambian datos: siempre piden confirmación expresa antes de ejecutarse.
  bool get cambiaDatos => this == cancelarClase || this == reprogramarClase;
}
