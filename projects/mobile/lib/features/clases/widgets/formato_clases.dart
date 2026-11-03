import '../../../core/network/api_client.dart';
import '../../horarios/models/fechas.dart';
import '../models/clase_agenda.dart';
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

/// El día de la agenda del instructor como título: «Hoy», «Mañana» o «Lunes 27 oct».
String tituloDeDiaLargo(DateTime fecha, DateTime ahora) {
  final dia = soloFecha(fecha);
  final hoy = soloFecha(ahora);
  if (dia == hoy) return 'Hoy';
  if (dia == sumarDias(hoy, 1)) return 'Mañana';
  return '${diaConNumero(dia)} ${mesCorto(dia.month)}';
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

/// El estado de una clase de la agenda del instructor (CU16), en español. Una confirmada que ya terminó queda
/// «pendiente de cierre» (el cierre es de CU17).
String etiquetaDeEstadoAgenda(ClaseAgenda clase) {
  switch (clase.estado) {
    case EstadoReserva.completada:
      return 'Completada';
    case EstadoReserva.noAsistio:
      return 'No asistió';
    case EstadoReserva.cancelada:
      return 'Cancelada';
    case EstadoReserva.confirmada:
      return clase.terminada ? 'Pendiente de cierre' : 'Confirmada';
  }
}

/// La clase ya empezó según [ahora] (la hora del teléfono): su fecha y su hora de inicio no son posteriores a ahora.
/// Es solo para decidir qué acciones ofrecer; el servidor es quien manda (`ERR_ACA_33`).
bool laClaseYaEmpezo(Reserva reserva, DateTime ahora) {
  final partes = reserva.horaInicio.split(':');
  final inicio = DateTime(
    reserva.fecha.year,
    reserva.fecha.month,
    reserva.fecha.day,
    int.parse(partes[0]),
    int.parse(partes[1]),
  );
  return !inicio.isAfter(ahora);
}

/// El mensaje que se le muestra al estudiante cuando falla algo de sus clases: el del backend si es un error de
/// negocio (ya viene en español), o uno genérico si es de red.
String mensajeDeError(Object error) {
  if (error is ApiException) return error.message;
  return 'No pudimos conectarnos. Revisa tu conexión e inténtalo de nuevo.';
}
