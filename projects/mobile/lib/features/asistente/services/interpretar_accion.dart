import '../models/accion_asistente.dart';
import '../models/catalogo_acciones.dart';
import '../models/nombre_accion.dart';
import '../models/resultado_interpretacion.dart';

/// Convierte la llamada a función que devolvió el modelo en una acción segura, o en un rechazo (CU50).
///
/// * El modelo se equivoca (se midió ~12 % en las frases de prueba), así que NADA de lo que devuelve se ejecuta
///   sin pasar por acá: la función tiene que estar en el catálogo, cada argumento tiene que existir para esa
///   función, ser del tipo y de los valores permitidos, y no puede faltar ninguno obligatorio.
/// * Un argumento inválido rechaza TODA la acción: ignorarlo y seguir con los demás ejecutaría algo distinto de
///   lo que la persona pidió (por ejemplo, un filtro de instructor que se pierde).
ResultadoInterpretacion interpretarAccion(
  String nombre,
  Map<String, dynamic> argumentos,
  CatalogoAcciones catalogo,
) {
  final accion = NombreAccion.desdeNombre(nombre);
  final definicion = catalogo.acciones[nombre];
  if (accion == null || definicion == null) return const AccionRechazada(MotivoRechazo.desconocida);
  if (accion == NombreAccion.fueraDeAlcance) return const AccionRechazada(MotivoRechazo.fueraDeAlcance);

  final valores = <String, String>{};
  for (final entrada in argumentos.entries) {
    final parametro = definicion.parametros[entrada.key];
    final valor = entrada.value;
    if (parametro == null || valor is! String) return const AccionRechazada(MotivoRechazo.argumentoInvalido);
    final texto = valor.trim();
    if (texto.isEmpty) continue;
    if (parametro.valores != null && !parametro.valores!.contains(texto)) {
      return const AccionRechazada(MotivoRechazo.argumentoInvalido);
    }
    if (entrada.key == 'hora' && !_horaValida.hasMatch(texto)) {
      return const AccionRechazada(MotivoRechazo.argumentoInvalido);
    }
    valores[entrada.key] = texto;
  }

  for (final requerido in definicion.requeridos) {
    if (!valores.containsKey(requerido)) return const AccionRechazada(MotivoRechazo.faltaDato);
  }

  return AccionEntendida(
    AccionAsistente(
      accion: accion,
      dia: valores['dia'],
      momento: valores['momento'],
      instructor: valores['instructor'],
      hora: valores['hora'],
    ),
  );
}

final RegExp _horaValida = RegExp(r'^([01]\d|2[0-3]):[0-5]\d$');
