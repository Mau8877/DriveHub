/// Cuerpo de `PATCH /usuarios/me` (CU06, CU08a): solo los campos que cambian.
///
/// * La ubicación va como par (`latitud` y `longitud`) y el servidor recalcula la dirección. No
/// * hay `direccion` ni `categoriaSigla`: la dirección se calcula y la categoría la fija la escuela.
/// * `borrarTelefono` manda `telefono: null` explícito, que es como el backend sabe que hay que
/// * borrarlo (un campo ausente significa "no tocar").
class EditarPerfilRequest {
  final String? nombres;
  final String? apellidos;
  final String? telefono;
  final bool borrarTelefono;
  final DateTime? fechaNacimiento;
  final String? ci;
  final String? sexo;
  final double? latitud;
  final double? longitud;

  const EditarPerfilRequest({
    this.nombres,
    this.apellidos,
    this.telefono,
    this.borrarTelefono = false,
    this.fechaNacimiento,
    this.ci,
    this.sexo,
    this.latitud,
    this.longitud,
  });

  bool get estaVacio =>
      nombres == null &&
      apellidos == null &&
      telefono == null &&
      !borrarTelefono &&
      fechaNacimiento == null &&
      ci == null &&
      sexo == null &&
      latitud == null &&
      longitud == null;

  Map<String, dynamic> toJson() => {
    if (nombres != null) 'nombres': nombres,
    if (apellidos != null) 'apellidos': apellidos,
    if (borrarTelefono)
      'telefono': null
    else if (telefono != null)
      'telefono': telefono,
    if (fechaNacimiento != null)
      'fechaNacimiento': _comoFecha(fechaNacimiento!),
    if (ci != null) 'ci': ci,
    if (sexo != null) 'sexo': sexo,
    if (latitud != null && longitud != null) ...{
      'latitud': latitud,
      'longitud': longitud,
    },
  };

  static String _comoFecha(DateTime fecha) {
    final mes = fecha.month.toString().padLeft(2, '0');
    final dia = fecha.day.toString().padLeft(2, '0');
    return '${fecha.year}-$mes-$dia';
  }
}
