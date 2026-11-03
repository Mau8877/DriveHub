import '../models/prueba_clase.dart';
import 'validadores_prueba.dart';

/// Los textos de evaluar una clase (CU18), en un solo lugar.

/// Lo que se le dice al instructor DESPUÉS de evaluar: el puntaje y el resultado que devolvió el servidor.
String avisoDePruebaRegistrada(PruebaClase prueba) =>
    'Prueba registrada: ${formatoPuntaje(prueba.puntajeTotal)} puntos, ${prueba.resultado.etiqueta.toLowerCase()}.';

/// Cuando la clase ya tiene las tres pruebas no queda nada por evaluar.
const String avisoTodasLasPruebasHechas = 'Esta clase ya tiene las tres pruebas.';

/// Los códigos de error de negocio con los que evaluar deja de tener sentido: la clase no existe para este instructor
/// (`ERR_ACA_35`), no se puede evaluar (`ERR_ACA_39`) o ya tiene una prueba de ese tipo (`ERR_ACA_40`). Se cierra la
/// hoja con el mensaje del servidor y la agenda ya se refrescó.
const codigosQueCierranLaPrueba = {'ERR_ACA_35', 'ERR_ACA_39', 'ERR_ACA_40'};
