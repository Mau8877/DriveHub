import '../../clases/models/reserva.dart';
import 'preparacion_de_cambio.dart';

/// Lo que muestra el asistente de voz en cada momento (CU50). Estado puro de UI: no guarda datos del servidor.
sealed class EstadoAsistente {
  const EstadoAsistente();
}

/// No está haciendo nada.
class Inactivo extends EstadoAsistente {
  const Inactivo();
}

/// Está grabando mientras la persona mantiene pulsado el micrófono.
class Escuchando extends EstadoAsistente {
  const Escuchando();
}

/// Convirtiendo la voz en texto ([texto] es `null`) o eligiendo la acción ([texto] es lo que se oyó).
class Entendiendo extends EstadoAsistente {
  final String? texto;

  const Entendiendo({this.texto});
}

/// Se hizo lo pedido, o no se pudo y [mensaje] explica por qué ([exito] es falso).
class Resultado extends EstadoAsistente {
  final String texto;
  final String mensaje;
  final bool exito;

  const Resultado({required this.texto, required this.mensaje, required this.exito});
}

/// Se identificó UNA clase para cancelar o reprogramar: la pantalla abre la confirmación que ya existe en `clases`.
/// El asistente no cambia nada por su cuenta.
class PideCambio extends EstadoAsistente {
  final String texto;
  final PropuestaDeCambio propuesta;

  const PideCambio({required this.texto, required this.propuesta});
}

/// Cuadran varias clases: la persona elige una de [candidatas].
class Elegir extends EstadoAsistente {
  final String texto;
  final TipoCambio tipo;
  final List<Reserva> candidatas;

  const Elegir({required this.texto, required this.tipo, required this.candidatas});
}

/// No se entendió o no se puede hacer desde la pantalla de Clases; [mensaje] dice qué hacer.
class NoEntendi extends EstadoAsistente {
  final String? texto;
  final String mensaje;

  const NoEntendi({this.texto, required this.mensaje});
}

/// La persona no concedió el permiso de micrófono.
class SinPermiso extends EstadoAsistente {
  const SinPermiso();
}

/// Algo falló por dentro (modelo ausente, error inesperado); [mensaje] es para la persona, sin detalles técnicos.
class Fallo extends EstadoAsistente {
  final String mensaje;

  const Fallo(this.mensaje);
}
