/// Reglas de validación del registro del estudiante (CU01).
///
/// * Deben coincidir con las del backend y las de la web
/// * (projects/frontend/identidad/.../registro.reglas.ts). Mensajes en español.
/// * Cada función devuelve el mensaje de error o `null` si el valor es
/// * válido, listas para usarse como `validator` de un `TextFormField`.
const int edadMinimaRegistro = 18;
const int passwordMin = 9;
const int passwordMax = 64;
const int nombresMax = 80;
const int apellidosMax = 80;
const int emailMax = 120;
const int telefonoMax = 20;
const int ciMin = 5;
const int ciMax = 15;

final RegExp _formatoEmail = RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$');
final RegExp _formatoCi = RegExp(r'^[A-Za-z0-9-]+$');

String? _obligatorio(String? valor, String mensaje) {
  if (valor == null || valor.trim().isEmpty) return mensaje;
  return null;
}

String? validarNombres(String? valor) {
  return _obligatorio(valor, 'Los nombres son obligatorios') ??
      (valor!.length > nombresMax
          ? 'Los nombres no pueden superar $nombresMax caracteres'
          : null);
}

String? validarApellidos(String? valor) {
  return _obligatorio(valor, 'Los apellidos son obligatorios') ??
      (valor!.length > apellidosMax
          ? 'Los apellidos no pueden superar $apellidosMax caracteres'
          : null);
}

String? validarEmail(String? valor) {
  final obligatorio = _obligatorio(valor, 'El correo es obligatorio');
  if (obligatorio != null) return obligatorio;
  final correo = valor!.trim();
  if (!_formatoEmail.hasMatch(correo)) return 'El correo no es válido';
  if (correo.length > emailMax) {
    return 'El correo no puede superar $emailMax caracteres';
  }
  return null;
}

String? validarPassword(String? valor) {
  if (valor == null || valor.isEmpty) return 'La contraseña es obligatoria';
  if (valor.length < passwordMin || valor.length > passwordMax) {
    return 'La contraseña debe tener entre $passwordMin y $passwordMax caracteres';
  }
  return null;
}

String? validarTelefono(String? valor) {
  if (valor == null || valor.isEmpty) return null;
  if (valor.length > telefonoMax) {
    return 'El teléfono no puede superar $telefonoMax caracteres';
  }
  return null;
}

/// CI (CU08a): de [ciMin] a [ciMax] caracteres, solo letras, números y guion, sin espacios.
String? validarCi(String? valor) {
  final obligatorio = _obligatorio(valor, 'El CI es obligatorio');
  if (obligatorio != null) return obligatorio;
  final ci = valor!.trim();
  if (ci.length < ciMin || ci.length > ciMax || !_formatoCi.hasMatch(ci)) {
    return 'El CI debe tener entre $ciMin y $ciMax caracteres: letras, números y guion';
  }
  return null;
}

String? validarSexo(String? valor) {
  return (valor == null || valor.isEmpty) ? 'Elige tu sexo' : null;
}

/// El punto del mapa: obligatorio, con las dos coordenadas y dentro de rango.
String? validarUbicacion(double? latitud, double? longitud) {
  if (latitud == null || longitud == null) return 'Elige tu ubicación en el mapa';
  if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
    return 'La ubicación no es válida';
  }
  return null;
}

String? validarCodigoEscuela(String? valor) {
  return _obligatorio(valor, 'El código de la escuela es obligatorio');
}

/// Años cumplidos de [nacimiento] a la fecha [hoy].
int edadEnAnios(DateTime nacimiento, DateTime hoy) {
  var edad = hoy.year - nacimiento.year;
  final cumplioEsteAnio =
      hoy.month > nacimiento.month ||
      (hoy.month == nacimiento.month && hoy.day >= nacimiento.day);
  if (!cumplioEsteAnio) edad -= 1;
  return edad;
}

/// Obligatoria, en el pasado y con al menos [edadMinimaRegistro] años
/// cumplidos; el día en que cumple 18 es válido.
String? validarFechaNacimiento(DateTime? fecha, {DateTime? hoy}) {
  if (fecha == null) return 'La fecha de nacimiento es obligatoria';
  final ahora = hoy ?? DateTime.now();
  final hoySinHora = DateTime(ahora.year, ahora.month, ahora.day);
  final nacimiento = DateTime(fecha.year, fecha.month, fecha.day);
  if (!nacimiento.isBefore(hoySinHora)) {
    return 'La fecha de nacimiento debe estar en el pasado';
  }
  if (edadEnAnios(nacimiento, hoySinHora) < edadMinimaRegistro) {
    return 'Debes tener al menos $edadMinimaRegistro años';
  }
  return null;
}
