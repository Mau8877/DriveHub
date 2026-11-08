import '../../../core/offline/modelos_offline.dart';
import '../../horarios/models/fechas.dart';
import '../models/clase_agenda.dart';
import '../models/estado_reserva.dart';
import '../models/franja_disponible.dart';
import '../models/reserva.dart';

/// Lo que una operación de clases hecha sin conexión le hace a la pantalla mientras espera enviarse (actualización
/// optimista): la clase se ve como «pendiente» y sus botones se apagan, para no hacer dos veces lo mismo.
///
/// * No cambia datos: es solo cómo se dibuja. Cuando el servidor decide, la acción sale de la cola: si la aceptó, la
/// * lista se vuelve a pedir con el estado real; si la rechazó, la clase vuelve a verse como estaba y el aviso de
/// * `AvisoOffline` explica por qué.
class EfectoOffline {
  static const reservar = 'reservar';
  static const cancelar = 'cancelar';
  static const reprogramar = 'reprogramar';
  static const cerrar = 'cerrar';
  static const corregir = 'corregir';
  static const evaluar = 'evaluar';

  const EfectoOffline._();
}

/// El texto de la insignia de una operación pendiente.
String etiquetaDePendiente(AccionPendiente accion) => switch (accion.efecto) {
  EfectoOffline.reservar => 'Reserva pendiente',
  EfectoOffline.cancelar => 'Cancelación pendiente',
  EfectoOffline.reprogramar => 'Reprogramación pendiente',
  EfectoOffline.cerrar => 'Cierre pendiente',
  EfectoOffline.corregir => 'Corrección pendiente',
  EfectoOffline.evaluar => 'Evaluación pendiente',
  _ => 'Pendiente de enviar',
};

/// La operación pendiente que toca a una clase de la agenda del instructor: por el código de la reserva (cancelar,
/// cerrar) o por el de la clase ya cerrada (corregir, evaluar).
AccionPendiente? pendienteDeClase(EstadoOffline estado, ClaseAgenda clase) {
  final porReserva = estado.pendienteSobre(clase.codigo);
  if (porReserva != null) return porReserva;
  final claseCodigo = clase.claseCodigo;
  return claseCodigo == null ? null : estado.pendienteSobre(claseCodigo);
}

/// Lo que se guarda de una franja para poder dibujar su reserva pendiente.
Map<String, dynamic> datosDeFranja(FranjaDisponible franja) => {
  'franjaCodigo': franja.codigo,
  'fecha': fechaParaApi(franja.fecha),
  'horaInicio': franja.horaInicio,
  'horaFin': franja.horaFin,
  'instructorCodigo': franja.instructorCodigo,
  'instructorNombre': franja.instructorNombre,
};

/// Las clases que el estudiante reservó sin conexión y todavía no se enviaron, dibujadas como reservas provisionales.
///
/// * Se identifican por el código de su franja (la reserva real aún no existe). Si la franja ya tiene una operación
/// * posterior encima (por ejemplo, la reservó y luego reprogramó otra a esa misma franja) igual se muestra una vez.
List<Reserva> reservasPendientes(EstadoOffline estado) {
  final vistas = <String>{};
  final reservas = <Reserva>[];
  for (final accion in estado.pendientesConEfecto(EfectoOffline.reservar)) {
    final franjas = accion.datos?['franjas'];
    if (franjas is! List) continue;
    for (final crudo in franjas) {
      final f = Map<String, dynamic>.from(crudo as Map);
      final codigo = f['franjaCodigo'] as String;
      if (!vistas.add(codigo)) continue;
      reservas.add(
        Reserva(
          codigo: codigo,
          fecha: fechaDesdeApi(f['fecha'] as String),
          horaInicio: f['horaInicio'] as String,
          horaFin: f['horaFin'] as String,
          instructorCodigo: f['instructorCodigo'] as String,
          instructorNombre: f['instructorNombre'] as String,
          estado: EstadoReserva.confirmada,
          terminada: false,
        ),
      );
    }
  }
  return reservas;
}
