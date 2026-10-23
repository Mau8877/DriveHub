/// Perfil de la cuenta propia (CU06, CU08a): lo que devuelve `GET /usuarios/me`.
class Perfil {
  final String codigo;
  final String nombres;
  final String apellidos;
  final DateTime fechaNacimiento;
  final String? telefono;
  final String email;
  final bool correoVerificado;
  final List<String> roles;

  /// `null` en el Super admin.
  final String? escuelaCodigo;
  final String? escuelaNombre;

  /// URL prefirmada de lectura (vale 60 minutos), o `null` si no tiene foto.
  final String? fotoUrl;

  final String ci;

  /// `MASCULINO` o `FEMENINO`.
  final String sexo;

  /// La calcula el servidor desde el punto del mapa; `null` si no se pudo (o en el Super admin).
  final String? direccion;
  final double? latitud;
  final double? longitud;

  /// Solo el estudiante la tiene y solo se lee: la asigna la escuela.
  final String? categoriaSigla;

  const Perfil({
    required this.codigo,
    required this.nombres,
    required this.apellidos,
    required this.fechaNacimiento,
    this.telefono,
    required this.email,
    required this.correoVerificado,
    required this.roles,
    this.escuelaCodigo,
    this.escuelaNombre,
    this.fotoUrl,
    required this.ci,
    required this.sexo,
    this.direccion,
    this.latitud,
    this.longitud,
    this.categoriaSigla,
  });

  factory Perfil.fromJson(Map<String, dynamic> json) {
    return Perfil(
      codigo: json['codigo'] as String,
      nombres: json['nombres'] as String,
      apellidos: json['apellidos'] as String,
      // * Llega como yyyy-MM-dd
      fechaNacimiento: DateTime.parse(json['fechaNacimiento'] as String),
      telefono: json['telefono'] as String?,
      email: json['email'] as String,
      correoVerificado: json['correoVerificado'] as bool? ?? false,
      roles: (json['roles'] as List<dynamic>).cast<String>(),
      escuelaCodigo: json['escuelaCodigo'] as String?,
      escuelaNombre: json['escuelaNombre'] as String?,
      fotoUrl: json['fotoUrl'] as String?,
      ci: json['ci'] as String,
      sexo: json['sexo'] as String,
      direccion: json['direccion'] as String?,
      latitud: (json['latitud'] as num?)?.toDouble(),
      longitud: (json['longitud'] as num?)?.toDouble(),
      categoriaSigla: json['categoriaSigla'] as String?,
    );
  }
}
