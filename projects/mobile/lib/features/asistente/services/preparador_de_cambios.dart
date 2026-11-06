import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../clases/models/estado_reserva.dart';
import '../../clases/models/instructor_disponible.dart';
import '../../clases/models/reserva.dart';
import '../../clases/models/vista_reservas.dart';
import '../../clases/providers/franjas_disponibles_provider.dart';
import '../../clases/widgets/formato_clases.dart';
import '../../clases/widgets/mensajes_de_cancelacion.dart';
import '../../horarios/models/fechas.dart';
import '../models/accion_asistente.dart';
import '../models/nombre_accion.dart';
import '../models/preparacion_de_cambio.dart';
import 'resolvedor_de_datos.dart';

/// Identifica QUÉ clase quiso cancelar o reprogramar la persona por voz (CU50). No cambia nada del servidor.
///
/// * Cancelar y reprogramar cambian datos, así que la confirmación es obligatoria y la hace la pantalla con el
///   diálogo y la hoja que ya existen en `clases` (CU15): el asistente no tiene un camino propio de cancelación.
/// * Sin datos apunta a la próxima clase; con día, hora o instructor, a la que cuadra. Si cuadran varias, la
///   persona elige: nunca se actúa sobre una clase que ella no vio.
/// * Respeta las reglas de CU15: no ofrece una clase que ya empezó, y reprogramar solo si todavía está a tiempo
///   (`cancelacionATiempo`, que decide el servidor).
class PreparadorDeCambios {
  final Ref _ref;
  final ResolvedorDeDatos _resolvedor;
  final DateTime Function() _ahora;

  PreparadorDeCambios(this._ref, {this._resolvedor = const ResolvedorDeDatos(), DateTime Function()? ahora})
    : _ahora = ahora ?? DateTime.now;

  Future<PreparacionDeCambio> preparar(AccionAsistente a) async {
    final tipo = switch (a.accion) {
      NombreAccion.cancelarClase => TipoCambio.cancelar,
      NombreAccion.reprogramarClase => TipoCambio.reprogramar,
      _ => throw ArgumentError.value(a.accion.nombre, 'accion', 'no es un cambio de una clase'),
    };

    final List<Reserva> reservas;
    try {
      reservas = await _ref.read(reservasListProvider(VistaReservas.proximas).future);
    } catch (_) {
      return const NadaQueCambiar('No pude consultar tus clases. Revisa tu conexión e intenta de nuevo.');
    }

    final ahora = _ahora();
    final vigentes = [
      for (final r in reservas)
        if (r.estado == EstadoReserva.confirmada && !r.terminada && !laClaseYaEmpezo(r, ahora)) r,
    ]..sort(_porInicio);
    if (vigentes.isEmpty) return const NadaQueCambiar('No tienes clases por venir.');

    var candidatas = vigentes;
    if (a.dia != null) {
      final fecha = _resolvedor.resolverDia(a.dia!, ahora);
      if (fecha == null) return const NadaQueCambiar('No entendí qué día.');
      candidatas = [for (final r in candidatas) if (soloFecha(r.fecha) == fecha) r];
    }
    if (a.hora != null) {
      candidatas = [for (final r in candidatas) if (r.horaInicio == a.hora) r];
    }
    if (a.instructor != null) {
      final resolucion = _resolvedor.resolverInstructor(a.instructor!, _instructoresDe(vigentes));
      switch (resolucion) {
        case InstructorEncontrado(:final instructor):
          candidatas = [for (final r in candidatas) if (r.instructorCodigo == instructor.codigo) r];
        case InstructorAmbiguo():
        case InstructorSinCoincidencia():
          return NadaQueCambiar('No encontré una clase tuya con "${a.instructor}".');
      }
    }
    if (candidatas.isEmpty) return const NadaQueCambiar('No encontré esa clase entre tus próximas clases.');

    final sinFiltros = a.dia == null && a.hora == null && a.instructor == null;
    if (candidatas.length > 1 && !sinFiltros) return VariasPosibles(tipo: tipo, candidatas: candidatas);

    // * Sin datos: la próxima clase (la primera de las vigentes)
    final reserva = candidatas.first;
    return _proponer(tipo, reserva);
  }

  /// Lo que se le dice a la persona sobre la clase elegida, o por qué no se puede cambiar. También sirve cuando la
  /// persona eligió una de [VariasPosibles].
  PreparacionDeCambio proponer(TipoCambio tipo, Reserva reserva) => _proponer(tipo, reserva);

  PreparacionDeCambio _proponer(TipoCambio tipo, Reserva reserva) {
    final clase = 'tu clase ${_deCuando(reserva)} a las ${reserva.horaInicio} con ${reserva.instructorNombre}';
    switch (tipo) {
      case TipoCambio.cancelar:
        return PropuestaDeCambio(
          tipo: tipo,
          reserva: reserva,
          resumen: 'Cancelar $clase. ${avisoPrevioDeCancelacion(aTiempo: reserva.cancelacionATiempo)}',
        );
      case TipoCambio.reprogramar:
        if (!reserva.cancelacionATiempo) {
          return NadaQueCambiar(
            'No se puede reprogramar $clase: faltan menos de $horasDePlazo horas. Todavía puedes cancelarla.',
          );
        }
        return PropuestaDeCambio(
          tipo: tipo,
          reserva: reserva,
          resumen: 'Mover $clase. Elige el nuevo horario.',
        );
    }
  }

  /// «de hoy», «de mañana» o «del viernes 7/11».
  String _deCuando(Reserva r) {
    final hoy = soloFecha(_ahora());
    final fecha = soloFecha(r.fecha);
    if (fecha == hoy) return 'de hoy';
    if (fecha == sumarDias(hoy, 1)) return 'de mañana';
    return 'del ${nombreDeDia(fecha.weekday)} ${fecha.day}/${fecha.month}';
  }

  List<InstructorDisponible> _instructoresDe(List<Reserva> reservas) {
    final vistos = <String, InstructorDisponible>{};
    for (final r in reservas) {
      vistos.putIfAbsent(
        r.instructorCodigo,
        () => InstructorDisponible(codigo: r.instructorCodigo, nombre: r.instructorNombre),
      );
    }
    return vistos.values.toList();
  }

  int _porInicio(Reserva a, Reserva b) {
    final porFecha = soloFecha(a.fecha).compareTo(soloFecha(b.fecha));
    return porFecha != 0 ? porFecha : a.horaInicio.compareTo(b.horaInicio);
  }
}

/// El preparador de cambios del asistente (CU50). Un provider para poder sustituirlo en los tests.
final preparadorDeCambiosProvider = Provider<PreparadorDeCambios>((ref) => PreparadorDeCambios(ref));
