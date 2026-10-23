/// Cuerpo de `POST /usuarios` (registro del estudiante, CU01).
///
/// * No tiene campo de rol a propósito: el backend asigna siempre ESTUDIANTE.
/// * CU08a: suma CI, sexo y la ubicación elegida en el mapa. No hay dirección ni categoría: la
/// * dirección la calcula el servidor y la categoría arranca en "sin categoría".
class RegistroEstudianteRequest {
  final String nombres;
  final String apellidos;
  final DateTime fechaNacimiento;
  final String email;
  final String password;
  final String? telefono;
  final String escuelaSlug;
  final String ci;

  /// `MASCULINO` o `FEMENINO`.
  final String sexo;
  final double latitud;
  final double longitud;

  const RegistroEstudianteRequest({
    required this.nombres,
    required this.apellidos,
    required this.fechaNacimiento,
    required this.email,
    required this.password,
    this.telefono,
    required this.escuelaSlug,
    required this.ci,
    required this.sexo,
    required this.latitud,
    required this.longitud,
  });

  Map<String, dynamic> toJson() {
    final mes = fechaNacimiento.month.toString().padLeft(2, '0');
    final dia = fechaNacimiento.day.toString().padLeft(2, '0');
    return {
      'nombres': nombres,
      'apellidos': apellidos,
      'fechaNacimiento': '${fechaNacimiento.year.toString().padLeft(4, '0')}-$mes-$dia',
      'email': email,
      'password': password,
      if (telefono != null && telefono!.isNotEmpty) 'telefono': telefono,
      'escuelaSlug': escuelaSlug,
      'ci': ci,
      'sexo': sexo,
      'latitud': latitud,
      'longitud': longitud,
    };
  }
}
