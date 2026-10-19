/// Datos básicos de la persona con sesión iniciada.
class UsuarioSesion {
  final String codigo;
  final String nombres;
  final String apellidos;
  final List<String> roles;
  final String? escuelaCodigo;

  const UsuarioSesion({
    required this.codigo,
    required this.nombres,
    required this.apellidos,
    required this.roles,
    this.escuelaCodigo,
  });

  factory UsuarioSesion.fromJson(Map<String, dynamic> json) {
    return UsuarioSesion(
      codigo: json['codigo'] as String,
      nombres: json['nombres'] as String,
      apellidos: json['apellidos'] as String,
      roles: (json['roles'] as List<dynamic>).cast<String>(),
      escuelaCodigo: json['escuelaCodigo'] as String?,
    );
  }

  Map<String, dynamic> toJson() => {
    'codigo': codigo,
    'nombres': nombres,
    'apellidos': apellidos,
    'roles': roles,
    'escuelaCodigo': escuelaCodigo,
  };
}
