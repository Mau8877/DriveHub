import '../../../core/network/api_client.dart';
import '../../horarios/models/fechas.dart';
import '../models/estado_reserva.dart';
import '../models/reserva.dart';

/// El día de una clase como título de grupo: «Hoy», «Mañana» o «lun 27 oct».
String tituloDeDia(DateTime fecha, DateTime ahora) {
  final dia = soloFecha(fecha);
  final hoy = soloFecha(ahora);
  if (dia == hoy) return 'Hoy';
  if (dia == sumarDias(hoy, 1)) return 'Mañana';
  final nombre = nombreDeDia(dia.weekday);
  return '${nombre.substring(0, 3)} ${dia.day} ${mesCorto(dia.month)}';
}

/// El rango de una clase: `08:00 – 09:00`.
String rangoDeHoras(String horaInicio, String horaFin) => '$horaInicio – $horaFin';

/// El estado de una clase en el historial, en español: el color del distintivo solo lo refuerza.
String etiquetaDeEstado(Reserva reserva) {
  switch (reserva.estado) {
    case EstadoReserva.completada:
      return 'Realizada';
    case EstadoReserva.noAsistio:
      return 'No asistió';
    case EstadoReserva.cancelada:
      return 'Cancelada';
    case EstadoReserva.confirmada:
      return reserva.terminada ? 'Pendiente de cierre' : 'Confirmada';
  }
}

/// El mensaje que se le muestra al estudiante cuando falla algo de sus clases: el del backend si es un error de
/// negocio (ya viene en español), o uno genérico si es de red.
String mensajeDeError(Object error) {
  if (error is ApiException) return error.message;
  return 'No pudimos conectarnos. Revisa tu conexión e inténtalo de nuevo.';
}
