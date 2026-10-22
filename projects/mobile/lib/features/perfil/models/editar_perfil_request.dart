/// Cuerpo de `PATCH /usuarios/me` (CU06): solo los campos que cambian.
///
/// * `borrarTelefono` manda `telefono: null` explícito, que es como el backend sabe que hay que
/// * borrarlo (un campo ausente significa "no tocar").
class EditarPerfilRequest {
  final String? nombres;
  final String? apellidos;
  final String? telefono;
  final bool borrarTelefono;
  final DateTime? fechaNacimiento;

  const EditarPerfilRequest({
    this.nombres,
    this.apellidos,
    this.telefono,
    this.borrarTelefono = false,
    this.fechaNacimiento,
  });

  bool get estaVacio =>
      nombres == null &&
      apellidos == null &&
      telefono == null &&
      !borrarTelefono &&
      fechaNacimiento == null;

  Map<String, dynamic> toJson() => {
    if (nombres != null) 'nombres': nombres,
    if (apellidos != null) 'apellidos': apellidos,
    if (borrarTelefono)
      'telefono': null
    else if (telefono != null)
      'telefono': telefono,
    if (fechaNacimiento != null)
      'fechaNacimiento': _comoFecha(fechaNacimiento!),
  };

  static String _comoFecha(DateTime fecha) {
    final mes = fecha.month.toString().padLeft(2, '0');
    final dia = fecha.day.toString().padLeft(2, '0');
    return '${fecha.year}-$mes-$dia';
  }
}
