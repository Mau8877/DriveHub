import '../../auth/validators/registro_validators.dart' as registro;

/// Reglas de los formularios del perfil (CU06).
///
/// * Los campos de datos personales son los mismos del registro: se reutilizan las reglas de
/// * `features/auth/validators/registro_validators.dart` para que no se desalineen (80, 80, 20,
/// * edad ≥ 18). Mensajes en español; cada función devuelve el error o `null` si es válido.
String? validarNombres(String? valor) => registro.validarNombres(valor);

String? validarApellidos(String? valor) => registro.validarApellidos(valor);

String? validarTelefono(String? valor) => registro.validarTelefono(valor);

String? validarFechaNacimiento(DateTime? fecha, {DateTime? hoy}) =>
    registro.validarFechaNacimiento(fecha, hoy: hoy);

// * CU08a: el CI, el sexo y la ubicación también son los del registro (misma regla del mismo
// * campo): una feature importa de otra solo con justificación de negocio, y es esta.
String? validarCi(String? valor) => registro.validarCi(valor);

String? validarSexo(String? valor) => registro.validarSexo(valor);

String? validarUbicacion(double? latitud, double? longitud) =>
    registro.validarUbicacion(latitud, longitud);

/// La contraseña actual solo tiene que estar escrita.
String? validarContrasenaActual(String? valor) {
  if (valor == null || valor.isEmpty) {
    return 'La contraseña actual es obligatoria';
  }
  return null;
}

/// La contraseña nueva sigue la regla del registro (9 a 64 caracteres).
String? contrasenaNueva(String? valor) => registro.validarPassword(valor);

/// La confirmación debe ser igual a la contraseña nueva.
String? confirmacion(String? valor, String nueva) {
  if (valor == null || valor.isEmpty) return 'Confirma la contraseña nueva';
  if (valor != nueva) return 'Las contraseñas no coinciden';
  return null;
}

/// La nueva no puede ser igual a la actual (el backend lo reafirma con `ERR_IDE_15`).
String? distintaDeLaActual(String? nueva, String actual) {
  if (nueva != null && nueva.isNotEmpty && nueva == actual) {
    return 'La contraseña nueva debe ser distinta de la actual';
  }
  return null;
}

/// El correo nuevo: formato válido y hasta 120 caracteres (regla del registro).
String? validarCorreoNuevo(String? valor) => registro.validarEmail(valor);

/// Mensajes en español de los códigos de negocio del perfil, sin detalles técnicos.
const Map<String, String> mensajesPerfil = {
  'ERR_IDE_01': 'Ese correo ya está en uso',
  'ERR_IDE_02': 'Debes tener al menos 18 años',
  'ERR_IDE_14': 'La contraseña actual no es correcta',
  'ERR_IDE_15': 'La contraseña nueva debe ser distinta de la actual',
  'ERR_IDE_16':
      'El archivo no es una imagen válida. Usa una foto JPG, PNG o WebP',
  'ERR_IDE_17': 'La imagen no puede superar 5 MB',
  'ERR_IDE_18': 'Ese ya es tu correo',
  'ERR_IDE_19': 'Este CI ya está registrado',
};

const String mensajeGenerico = 'Ocurrió un error. Intenta más tarde';

/// El mensaje de un código de negocio del perfil, o el genérico si no está en el mapa.
String mensajeDeCodigo(String? codigo) =>
    mensajesPerfil[codigo] ?? mensajeGenerico;
