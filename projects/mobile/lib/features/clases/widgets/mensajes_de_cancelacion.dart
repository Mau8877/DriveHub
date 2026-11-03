import '../models/hora_de_cancelacion.dart';

/// Los textos de cancelar y reprogramar una clase (CU15), en un solo lugar.
///
/// * El «12» de los avisos es texto fijo: el plazo real lo decide el servidor (`cancelacionATiempo` de cada clase).
///   Si el plazo cambia en el servidor se ajusta [horasDePlazo] acá y nada más.
const int horasDePlazo = 12;

/// Lo que se le dice al estudiante ANTES de confirmar, según si todavía está a tiempo de recuperar la hora.
String avisoPrevioDeCancelacion({required bool aTiempo}) =>
    aTiempo ? 'Se te devolverá 1 hora.' : 'Cancelas con menos de $horasDePlazo horas: perderás la hora.';

/// Lo que se le dice al estudiante DESPUÉS de cancelar, según qué pasó con su hora.
String avisoDeResultadoDeCancelacion(HoraDeCancelacion hora) {
  switch (hora) {
    case HoraDeCancelacion.devuelta:
      return 'Clase cancelada. Se te devolvió 1 hora.';
    case HoraDeCancelacion.perdida:
      return 'Clase cancelada. No se devolvió la hora.';
    case HoraDeCancelacion.enProceso:
      return 'Clase cancelada. Tu hora se devolverá en unos minutos.';
  }
}

/// Los códigos de error de negocio con los que cancelar o reprogramar deja de tener sentido: la clase ya no existe
/// para el estudiante, no está confirmada, ya empezó o se pasó el plazo. Se cierra lo que estaba abierto.
const codigosQueCierranLaAccion = {'ERR_ACA_32', 'ERR_ACA_33', 'ERR_ACA_34'};
