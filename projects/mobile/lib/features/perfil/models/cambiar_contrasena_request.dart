/// Cuerpo de `POST /usuarios/me/cambiar-contrasena` (CU06).
///
/// * El dispositivo es siempre `MOVIL`: así el backend conserva la sesión del teléfono y cierra
/// * la de la web.
/// ! Las contraseñas nunca se guardan ni se escriben en la consola.
class CambiarContrasenaRequest {
  final String passwordActual;
  final String passwordNueva;

  const CambiarContrasenaRequest({
    required this.passwordActual,
    required this.passwordNueva,
  });

  Map<String, dynamic> toJson() => {
    'passwordActual': passwordActual,
    'passwordNueva': passwordNueva,
    'dispositivo': 'MOVIL',
  };
}
