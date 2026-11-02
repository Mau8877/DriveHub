import '../models/hora_de_cancelacion.dart';

/// Los textos de cancelar una clase como instructor (CU16), en un solo lugar.
///
/// * El instructor no tiene plazo: el estudiante siempre recupera su hora y la franja se bloquea, así que el aviso
///   previo es fijo y no depende de la hora de la clase.

/// Lo que se le dice al instructor ANTES de confirmar.
const String avisoPrevioDeCancelacionInstructor =
    'Se le devolverá 1 hora al estudiante.\nLa franja quedará bloqueada.';

/// Lo que se le dice al instructor DESPUÉS de cancelar, según qué pasó con la hora del estudiante.
///
/// * [HoraDeCancelacion.perdida] no ocurre con el instructor; se tolera sin prometer ni negar la hora.
String avisoDeResultadoDeCancelacionInstructor(HoraDeCancelacion hora) {
  switch (hora) {
    case HoraDeCancelacion.devuelta:
      return 'Clase cancelada. Se devolvió la hora al estudiante.';
    case HoraDeCancelacion.enProceso:
      return 'Clase cancelada. La hora del estudiante se devolverá en unos minutos.';
    case HoraDeCancelacion.perdida:
      return 'Clase cancelada.';
  }
}

/// Los códigos de error de negocio con los que cancelar deja de tener sentido: la clase no existe para este
/// instructor (`ERR_ACA_35`) o ya no se puede cancelar (`ERR_ACA_36`). Se cierra el diálogo con el mensaje del
/// servidor.
const codigosQueCierranLaCancelacionDelInstructor = {'ERR_ACA_35', 'ERR_ACA_36'};
