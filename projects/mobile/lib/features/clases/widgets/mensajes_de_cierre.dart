import '../models/asistencia_clase.dart';

/// Los textos de cerrar y corregir una clase (CU17), en un solo lugar.

/// Lo que se le dice al instructor cuando elige «Ausente», ANTES de confirmar (regla de CU44: no hay devolución).
const String avisoDeInasistencia = 'El estudiante pierde la hora de esta clase.';

/// Lo que se le dice al instructor DESPUÉS de cerrar, según la asistencia que registró.
String avisoDeResultadoDeCierre(AsistenciaClase asistencia) => asistencia == AsistenciaClase.ausente
    ? 'Clase cerrada. Quedó registrada la inasistencia.'
    : 'Clase cerrada. Quedó completada.';

const String avisoDeCorreccionGuardada = 'Cambios guardados.';

/// Un kilometraje guardado se puede cambiar pero no borrar (el backend no lo admite).
const String avisoKilometrajeNoSeBorra = 'No se puede borrar el kilometraje; corrígelo o déjalo como estaba.';

/// Los códigos de error de negocio con los que cerrar o corregir deja de tener sentido: la clase no existe para este
/// instructor (`ERR_ACA_35`) o ya no se puede cerrar (`ERR_ACA_37`). Se cierra la hoja con el mensaje del servidor.
const codigosQueCierranElCierre = {'ERR_ACA_35', 'ERR_ACA_37'};
