/// Cuenta recién creada por el registro (sin contraseña ni sesión).
class UsuarioRegistrado {
  final String codigo;
  final String email;
  final String nombres;
  final String apellidos;

  const UsuarioRegistrado({
    required this.codigo,
    required this.email,
    required this.nombres,
    required this.apellidos,
  });

  factory UsuarioRegistrado.fromJson(Map<String, dynamic> json) {
    return UsuarioRegistrado(
      codigo: json['codigo'] as String,
      email: json['email'] as String,
      nombres: json['nombres'] as String,
      apellidos: json['apellidos'] as String,
    );
  }
}
