/// Cuerpo de `POST /usuarios` (registro del estudiante, CU01).
///
/// * No tiene campo de rol a propósito: el backend asigna siempre ESTUDIANTE.
class RegistroEstudianteRequest {
  final String nombres;
  final String apellidos;
  final DateTime fechaNacimiento;
  final String email;
  final String password;
  final String? telefono;
  final String escuelaSlug;

  const RegistroEstudianteRequest({
    required this.nombres,
    required this.apellidos,
    required this.fechaNacimiento,
    required this.email,
    required this.password,
    this.telefono,
    required this.escuelaSlug,
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
    };
  }
}
