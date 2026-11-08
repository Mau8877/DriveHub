import '../../../core/offline/modelos_offline.dart';
import '../../horarios/models/fechas.dart';
import '../models/reservas_resultado.dart';

/// Revisa la respuesta de una reserva enviada desde la cola offline: el servidor la contesta con 200 aunque haya
/// rechazado alguna franja (ya la tomó otro, Pagos no respondió...), así que cada franja rechazada se avisa a la
/// persona con el motivo del servidor. Las reservadas no necesitan aviso.
List<ObservacionRechazo> verificarReservaOffline(Object? respuesta) {
  final resultado = ReservasResultado.fromJson(Map<String, dynamic>.from(respuesta! as Map));
  return [
    for (final franja in resultado.rechazadas)
      ObservacionRechazo(
        '${_cualFranja(franja.fecha, franja.horaInicio, franja.instructorNombre)}: ${franja.mensaje}',
        codigo: franja.error,
      ),
  ];
}

String _cualFranja(DateTime? fecha, String? horaInicio, String? instructor) {
  final partes = [
    if (fecha != null) fechaParaApi(fecha),
    ?horaInicio,
    if (instructor != null) 'con $instructor',
  ];
  return partes.isEmpty ? 'Una de las clases' : 'Clase del ${partes.join(' ')}';
}
