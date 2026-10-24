/// Cuerpo de `POST /usuarios/me/cambiar-correo` (CU06): el enlace llega al correo nuevo.
///
/// ! La contraseña nunca se guarda ni se escribe en la consola.
class CambiarCorreoRequest {
  final String passwordActual;
  final String correoNuevo;

  const CambiarCorreoRequest({
    required this.passwordActual,
    required this.correoNuevo,
  });

  Map<String, dynamic> toJson() => {
    'passwordActual': passwordActual,
    'correoNuevo': correoNuevo,
  };
}
