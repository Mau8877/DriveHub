import '../models/catalogo_pruebas.dart';
import '../models/prueba_clase.dart';
import 'validadores_clase.dart';

// * Las reglas de los datos de una prueba de clase (CU18) en un solo lugar, iguales a las del backend: cada criterio
// * se puntúa de 0 a su máximo, de medio en medio punto; el total es lo obtenido sobre la suma de los máximos, sobre
// * 100 con dos decimales; aprueba con 70 o más; las observaciones admiten hasta 500 caracteres. Son funciones puras.
// ! Si una regla cambia, se cambia en el backend, acá y en las fichas de CU18 a la vez. El total que se ve mientras se
// ! puntúa es solo una vista previa: manda el que devuelve el servidor.

/// El total desde el cual una prueba queda aprobada.
const double puntajeDeAprobacion = 70;

/// El puntaje de cada criterio avanza de a medio punto.
const double pasoDePuntaje = 0.5;

/// El total sobre 100 con dos decimales, o `null` si falta puntuar algún criterio.
double? totalDePrueba(Map<String, double?> puntajes, List<CriterioDelCatalogo> criterios) {
  if (criterios.isEmpty) return null;
  var obtenido = 0.0;
  var maximo = 0.0;
  for (final criterio in criterios) {
    final puntaje = puntajes[criterio.codigo];
    if (puntaje == null) return null;
    obtenido += puntaje;
    maximo += criterio.maximo;
  }
  return ((obtenido * 100 / maximo) * 100).round() / 100;
}

ResultadoPrueba resultadoDePrueba(double total) =>
    total >= puntajeDeAprobacion ? ResultadoPrueba.aprobado : ResultadoPrueba.reprobado;

/// La descripción del primer criterio sin puntaje, o `null` si ya están todos puntuados.
String? primerCriterioSinPuntuar(Map<String, double?> puntajes, List<CriterioDelCatalogo> criterios) {
  for (final criterio in criterios) {
    if (puntajes[criterio.codigo] == null) return criterio.descripcion;
  }
  return null;
}

/// «Falta puntuar: …» con el primer criterio pendiente, o `null` si están todos.
String? validarCriteriosDePrueba(Map<String, double?> puntajes, List<CriterioDelCatalogo> criterios) {
  final pendiente = primerCriterioSinPuntuar(puntajes, criterios);
  return pendiente == null ? null : 'Falta puntuar: $pendiente';
}

/// Las observaciones son opcionales. Se mide el texto tal como se escribió (el backend también lo mide sin recortar).
String? validarObservacionesDePrueba(String? valor) =>
    (valor?.length ?? 0) > maxObservaciones ? 'Las observaciones admiten hasta $maxObservaciones caracteres' : null;

/// Un puntaje como se lee en español: sin ceros sobrantes y con coma decimal (`8`, `7,5`, `71,67`).
String formatoPuntaje(double valor) {
  if (valor == valor.truncateToDouble()) return valor.toInt().toString();
  var texto = valor.toStringAsFixed(2);
  if (texto.endsWith('0')) texto = texto.substring(0, texto.length - 1);
  return texto.replaceAll('.', ',');
}
