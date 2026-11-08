import '../../core/offline/modelos_offline.dart';

/// Lo que se le dice a la persona cuando una acción quedó guardada por falta de conexión.
///
/// * No promete que se cumpla: el servidor la valida al volver internet y, si no la acepta, se avisa.
String avisoDeAccionEncolada(AccionEncolada accion) =>
    'Sin conexión: guardamos «${accion.descripcion}». Se enviará al volver internet y el servidor la validará.';

/// Igual, para cancelar como estudiante: ahí el servidor decide al recibirla si se devuelve la hora.
String avisoDeCancelacionEncolada(AccionEncolada accion) =>
    'Sin conexión: guardamos «${accion.descripcion}». Se enviará al volver internet; el servidor decidirá entonces '
    'si recuperas la hora.';
