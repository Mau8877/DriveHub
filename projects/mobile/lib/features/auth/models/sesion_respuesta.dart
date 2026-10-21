import 'usuario_sesion.dart';

/// Sesión abierta o renovada (`/auth/login` y `/auth/refresh`).
///
/// * En el móvil el refresh token viaja en el cuerpo (no hay cookies).
class SesionRespuesta {
  final String accessToken;
  final String refreshToken;
  final int expiraEnSegundos;
  final UsuarioSesion usuario;

  const SesionRespuesta({
    required this.accessToken,
    required this.refreshToken,
    required this.expiraEnSegundos,
    required this.usuario,
  });

  factory SesionRespuesta.fromJson(Map<String, dynamic> json) {
    return SesionRespuesta(
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
      expiraEnSegundos: (json['expiraEnSegundos'] as num).toInt(),
      usuario: UsuarioSesion.fromJson(json['usuario'] as Map<String, dynamic>),
    );
  }
}
