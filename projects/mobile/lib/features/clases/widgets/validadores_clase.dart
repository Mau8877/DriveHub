// * Las reglas de los datos de una clase (CU17) en un solo lugar, iguales a las del backend: el tema admite hasta 120
// * caracteres, las observaciones hasta 500 y cada kilometraje es un entero de 0 a 9 999 999 (el final no puede ser
// * menor que el inicial). Son funciones puras: devuelven el mensaje en español o `null` si el valor es válido.
// ! Si una regla cambia, se cambia en el backend, acá y en las fichas de CU17 a la vez.

const int maxTema = 120;
const int maxObservaciones = 500;
const int maxKilometraje = 9999999;

/// El tema es opcional. Se mide el texto tal como se escribió (el backend también lo mide sin recortar).
String? validarTema(String? valor) =>
    (valor?.length ?? 0) > maxTema ? 'El tema admite hasta $maxTema caracteres' : null;

String? validarObservaciones(String? valor) =>
    (valor?.length ?? 0) > maxObservaciones ? 'Las observaciones admiten hasta $maxObservaciones caracteres' : null;

/// Un kilometraje es opcional: vacío vale. Si hay algo, son solo dígitos y no pasa del máximo.
String? validarKilometraje(String? valor) {
  final texto = valor?.trim() ?? '';
  if (texto.isEmpty) return null;
  if (!RegExp(r'^\d+$').hasMatch(texto)) return 'Escribe solo números';
  if (int.parse(texto) > maxKilometraje) return 'El kilometraje es demasiado grande';
  return null;
}

/// El kilometraje final: lo mismo que [validarKilometraje] y, si están los dos, no menor que el inicial.
String? validarKmFinal(String? kmInicio, String? kmFin) {
  final propio = validarKilometraje(kmFin);
  if (propio != null) return propio;
  final inicio = kilometrajeDe(kmInicio);
  final fin = kilometrajeDe(kmFin);
  if (inicio != null && fin != null && fin < inicio) {
    return 'El kilometraje final no puede ser menor que el inicial';
  }
  return null;
}

/// El número que escribió la persona, o `null` si el campo está vacío o no es un entero.
int? kilometrajeDe(String? valor) {
  final texto = valor?.trim() ?? '';
  return RegExp(r'^\d+$').hasMatch(texto) ? int.tryParse(texto) : null;
}
