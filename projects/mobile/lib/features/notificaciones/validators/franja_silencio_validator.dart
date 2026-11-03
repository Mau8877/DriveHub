/// Valida las horas de silencio de las preferencias (CU37): las dos o ninguna, y no iguales.
///
/// * Es la misma regla del backend (`ERR_NOT_03`); allá manda, esto solo da feedback inmediato.
/// * La franja puede cruzar la medianoche (22:00 a 06:00), así que no se compara cuál es mayor.
String? validarFranjaSilencio(String? desde, String? hasta) {
  if ((desde == null) != (hasta == null)) {
    return 'Elige las dos horas de silencio, o ninguna';
  }
  if (desde != null && desde == hasta) {
    return 'La hora de inicio y la de fin del silencio no pueden ser iguales';
  }
  return null;
}
