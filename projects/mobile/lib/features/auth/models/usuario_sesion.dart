/// Datos básicos de la persona con sesión iniciada.
class UsuarioSesion {
  final String codigo;
  final String nombres;
  final String apellidos;
  final List<String> roles;
  final String? escuelaCodigo;

  /// El correo se verificó con el enlace enviado (CU04b). No bloquea nada:
  /// solo condiciona la recuperación de contraseña.
  final bool correoVerificado;

  /// URL prefirmada de la foto de perfil (CU06), o null si no tiene. Se renueva con cada
  /// renovación de la sesión.
  final String? fotoUrl;

  const UsuarioSesion({
    required this.codigo,
    required this.nombres,
    required this.apellidos,
    required this.roles,
    this.escuelaCodigo,
    this.correoVerificado = false,
    this.fotoUrl,
  });

  factory UsuarioSesion.fromJson(Map<String, dynamic> json) {
    return UsuarioSesion(
      codigo: json['codigo'] as String,
      nombres: json['nombres'] as String,
      apellidos: json['apellidos'] as String,
      roles: (json['roles'] as List<dynamic>).cast<String>(),
      escuelaCodigo: json['escuelaCodigo'] as String?,
      // * Las sesiones que ya estaban guardadas en el teléfono no traen el campo
      correoVerificado: json['correoVerificado'] as bool? ?? false,
      // * Idem: las sesiones guardadas antes de CU06 no traen la foto
      fotoUrl: json['fotoUrl'] as String?,
    );
  }

  Map<String, dynamic> toJson() => {
    'codigo': codigo,
    'nombres': nombres,
    'apellidos': apellidos,
    'roles': roles,
    'escuelaCodigo': escuelaCodigo,
    'correoVerificado': correoVerificado,
    'fotoUrl': fotoUrl,
  };
}
