import 'accion_asistente.dart';

/// Por qué la salida del modelo no se pudo usar.
enum MotivoRechazo {
  /// El nombre no es una acción del catálogo (el modelo se inventó una función).
  desconocida,

  /// Falta un dato obligatorio (por ejemplo, la hora al elegir un horario).
  faltaDato,

  /// Un argumento que no existe para esa acción, de otro tipo o fuera de los valores permitidos.
  argumentoInvalido,

  /// El modelo dice que la petición no se puede hacer desde la pantalla de Clases.
  fueraDeAlcance,
}

/// Lo que sale de leer la respuesta del modelo (CU50): una acción lista para resolver y ejecutar, o un rechazo.
sealed class ResultadoInterpretacion {
  const ResultadoInterpretacion();
}

class AccionEntendida extends ResultadoInterpretacion {
  final AccionAsistente accion;

  const AccionEntendida(this.accion);
}

class AccionRechazada extends ResultadoInterpretacion {
  final MotivoRechazo motivo;

  const AccionRechazada(this.motivo);
}
