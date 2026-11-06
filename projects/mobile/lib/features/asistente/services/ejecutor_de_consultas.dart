import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../clases/models/instructor_disponible.dart';
import '../../clases/models/momento_dia.dart';
import '../../clases/models/reserva.dart';
import '../../clases/models/vista_reservas.dart';
import '../../clases/providers/clases_pestana_provider.dart';
import '../../clases/providers/clases_service_provider.dart';
import '../../clases/providers/franjas_disponibles_provider.dart';
import '../../clases/providers/franjas_seleccionadas_provider.dart';
import '../../horarios/models/fechas.dart';
import '../../pagos/providers/saldo_horas_provider.dart';
import '../models/accion_asistente.dart';
import '../models/nombre_accion.dart';
import '../models/resultado_ejecucion.dart';
import 'resolvedor_de_datos.dart';

/// Ejecuta las acciones de CONSULTA del asistente sobre la pantalla de Clases (CU50): deja la pantalla como si la
/// persona hubiera tocado esos controles.
///
/// * No cambia datos del servidor: cancelar y reprogramar son del `PreparadorDeCambios`. Elegir un horario solo
///   lo marca; reservar lo hace la barra «Confirmar» de CU14.
/// * Valida todo antes de tocar la pantalla: si el instructor dicho no existe, no cambia ni la fecha ni el momento.
/// * Los datos que la persona no mencionó no se tocan (un «busca por la tarde» conserva el instructor elegido);
///   `limpiar_filtros` es la forma de volver a todos.
class EjecutorDeConsultas {
  final Ref _ref;
  final ResolvedorDeDatos _resolvedor;
  final DateTime Function() _ahora;

  EjecutorDeConsultas(this._ref, {this._resolvedor = const ResolvedorDeDatos(), DateTime Function()? ahora})
    : _ahora = ahora ?? DateTime.now;

  Future<ResultadoEjecucion> ejecutar(AccionAsistente a) async {
    try {
      return switch (a.accion) {
        NombreAccion.verMisClases => await _verMisClases(a),
        NombreAccion.verHistorial => _verHistorial(),
        NombreAccion.buscarHorarios => await _buscarHorarios(a),
        NombreAccion.seleccionarHorario => await _seleccionarHorario(a),
        NombreAccion.limpiarFiltros => _limpiarFiltros(),
        NombreAccion.verHorasDisponibles => await _verHorasDisponibles(),
        NombreAccion.cancelarClase ||
        NombreAccion.reprogramarClase ||
        NombreAccion.fueraDeAlcance => throw ArgumentError.value(
          a.accion.nombre,
          'accion',
          'no es una consulta: va al PreparadorDeCambios o no se ejecuta',
        ),
      };
    } on ArgumentError {
      rethrow;
    } catch (_) {
      // * Sin red o el servidor falló: la persona lo ve como un aviso, no como un error de la app
      return const ResultadoEjecucion.fallo('No pude consultarlo. Revisa tu conexión e intenta de nuevo.');
    }
  }

  // ---- Mis clases ----

  Future<ResultadoEjecucion> _verMisClases(AccionAsistente a) async {
    _ref.read(clasesPestanaUiProvider.notifier).elegir(PestanaClases.misClases);
    final reservas = await _ref.read(reservasListProvider(VistaReservas.proximas).future);
    if (a.dia == null && a.instructor == null) {
      return ResultadoEjecucion.ok(
        reservas.isEmpty ? 'Todavía no tienes clases reservadas.' : 'Estas son tus próximas clases.',
      );
    }

    var coinciden = reservas;
    String cuando = '';
    if (a.dia != null) {
      final fecha = _resolvedor.resolverDia(a.dia!, _ahora());
      if (fecha == null) return const ResultadoEjecucion.fallo('No entendí qué día.');
      coinciden = [for (final r in coinciden) if (soloFecha(r.fecha) == fecha) r];
      cuando = ' ${_cuando(fecha)}';
    }
    var conInstructor = '';
    if (a.instructor != null) {
      final instructores = _instructoresDe(reservas);
      final resolucion = _resolvedor.resolverInstructor(a.instructor!, instructores);
      switch (resolucion) {
        case InstructorEncontrado(:final instructor):
          coinciden = [for (final r in coinciden) if (r.instructorCodigo == instructor.codigo) r];
          conInstructor = ' con ${instructor.nombre}';
        case InstructorAmbiguo(:final candidatos):
          return ResultadoEjecucion.fallo('¿Con cuál instructor: ${_nombres(candidatos)}?');
        case InstructorSinCoincidencia():
          return ResultadoEjecucion.fallo('No tienes clases con "${a.instructor}".');
      }
    }

    if (coinciden.isEmpty) return ResultadoEjecucion.ok('No tienes clases$cuando$conInstructor.');
    final detalle = coinciden.take(3).map(_describir).join('; ');
    return ResultadoEjecucion.ok('Tienes ${_clases(coinciden.length)}$cuando$conInstructor: $detalle.');
  }

  ResultadoEjecucion _verHistorial() {
    _ref.read(clasesPestanaUiProvider.notifier).elegir(PestanaClases.historial);
    return const ResultadoEjecucion.ok('Este es tu historial de clases.');
  }

  // ---- Reservar ----

  Future<ResultadoEjecucion> _buscarHorarios(AccionAsistente a) async {
    final (fecha, errorDia) = _fechaPedida(a.dia);
    if (errorDia != null) return errorDia;
    final momento = a.momento == null ? null : _resolvedor.resolverMomento(a.momento!);
    if (a.momento != null && momento == null) return const ResultadoEjecucion.fallo('No entendí el momento del día.');

    InstructorDisponible? instructor;
    if (a.instructor != null) {
      final DateTime fechaConsulta = fecha ?? _ref.read(reservarFechaUiProvider);
      final lista = await _ref.read(clasesServiceProvider).instructoresDisponibles(fecha: fechaConsulta);
      final resolucion = _resolverEntre(a.instructor!, lista, fechaConsulta);
      if (resolucion is ResultadoEjecucion) return resolucion;
      instructor = resolucion as InstructorDisponible;
    }

    _ref.read(clasesPestanaUiProvider.notifier).elegir(PestanaClases.reservar);
    if (fecha != null) _ref.read(reservarFechaUiProvider.notifier).elegir(fecha);
    if (momento != null) _ref.read(reservarMomentoUiProvider.notifier).elegir(momento);
    if (instructor != null) _ref.read(reservarInstructorUiProvider.notifier).elegir(instructor.codigo);

    final partes = [
      _cuando(fecha ?? _ref.read(reservarFechaUiProvider)),
      if (momento != null) _momento(momento),
      if (instructor != null) 'con ${instructor.nombre}',
    ];
    return ResultadoEjecucion.ok('Mostrando horarios libres ${partes.join(', ')}.');
  }

  Future<ResultadoEjecucion> _seleccionarHorario(AccionAsistente a) async {
    final hora = a.hora;
    if (hora == null) return const ResultadoEjecucion.fallo('No entendí a qué hora.');
    final (fecha, errorDia) = _fechaPedida(a.dia);
    if (errorDia != null) return errorDia;

    final DateTime fechaConsulta = fecha ?? _ref.read(reservarFechaUiProvider);
    InstructorDisponible? instructor;
    if (a.instructor != null) {
      final lista = await _ref.read(clasesServiceProvider).instructoresDisponibles(fecha: fechaConsulta);
      final resolucion = _resolverEntre(a.instructor!, lista, fechaConsulta);
      if (resolucion is ResultadoEjecucion) return resolucion;
      instructor = resolucion as InstructorDisponible;
    }

    // * La búsqueda de horarios de la pantalla va al día pedido, con el instructor dicho
    _ref.read(clasesPestanaUiProvider.notifier).elegir(PestanaClases.reservar);
    if (fecha != null) _ref.read(reservarFechaUiProvider.notifier).elegir(fecha);
    if (instructor != null) _ref.read(reservarInstructorUiProvider.notifier).elegir(instructor.codigo);

    final franjas = await _ref.read(franjasDisponiblesListProvider.future);
    final candidatas = [for (final f in franjas) if (f.horaInicio == hora) f];
    if (candidatas.isEmpty) {
      return ResultadoEjecucion.fallo('No hay un horario libre a las $hora ${_cuando(fechaConsulta)}.');
    }
    if (candidatas.length > 1) {
      final nombres = candidatas.map((f) => f.instructorNombre).toSet().join(', ');
      return ResultadoEjecucion.fallo('A las $hora hay varios instructores: $nombres. ¿Con cuál?');
    }

    final franja = candidatas.single;
    if (_ref.read(franjasSeleccionadasUiProvider).containsKey(franja.codigo)) {
      return ResultadoEjecucion.ok('Ese horario ya está marcado. Toca «Confirmar» para reservar.');
    }
    final saldo = await _ref.read(saldoHorasProvider.future);
    final marcado = _ref
        .read(franjasSeleccionadasUiProvider.notifier)
        .alternar(franja, horasDisponibles: saldo);
    if (!marcado) {
      return saldo <= 0
          ? const ResultadoEjecucion.fallo('No tienes horas disponibles para reservar.')
          : ResultadoEjecucion.fallo('No puedes marcar más horarios: te quedan ${_horas(saldo)}.');
    }
    return ResultadoEjecucion.ok(
      'Marqué el horario de las $hora con ${franja.instructorNombre}. Toca «Confirmar» para reservar.',
    );
  }

  ResultadoEjecucion _limpiarFiltros() {
    _ref.read(clasesPestanaUiProvider.notifier).elegir(PestanaClases.reservar);
    _ref.read(reservarMomentoUiProvider.notifier).elegir(MomentoDia.todos);
    _ref.read(reservarInstructorUiProvider.notifier).elegir(null);
    return const ResultadoEjecucion.ok('Quité los filtros de momento e instructor.');
  }

  Future<ResultadoEjecucion> _verHorasDisponibles() async {
    final saldo = await _ref.read(saldoHorasProvider.future);
    return ResultadoEjecucion.ok(saldo <= 0 ? 'No tienes horas disponibles.' : 'Te quedan ${_horas(saldo)}.');
  }

  // ---- Ayudas ----

  /// La fecha del día pedido, `null` si no pidió día (se queda la que ya está), o el fallo si no se entiende.
  (DateTime?, ResultadoEjecucion?) _fechaPedida(String? dia) {
    if (dia == null) return (null, null);
    final fecha = _resolvedor.resolverDia(dia, _ahora());
    if (fecha == null) return (null, const ResultadoEjecucion.fallo('No entendí qué día.'));
    return (fecha, null);
  }

  /// El instructor entre [lista], o el [ResultadoEjecucion] de fallo que explica por qué no se pudo elegir uno.
  Object _resolverEntre(String dicho, List<InstructorDisponible> lista, DateTime fecha) {
    final resolucion = _resolvedor.resolverInstructor(dicho, lista);
    switch (resolucion) {
      case InstructorEncontrado(:final instructor):
        return instructor;
      case InstructorAmbiguo(:final candidatos):
        return ResultadoEjecucion.fallo('¿Con cuál instructor: ${_nombres(candidatos)}?');
      case InstructorSinCoincidencia(:final opciones):
        return ResultadoEjecucion.fallo(
          opciones.isEmpty
              ? 'No hay instructores con horarios libres ${_cuando(fecha)}.'
              : 'No encontré a "$dicho". Hay horarios con ${_nombres(opciones)}.',
        );
    }
  }

  List<InstructorDisponible> _instructoresDe(List<Reserva> reservas) {
    final vistos = <String, InstructorDisponible>{};
    for (final r in reservas) {
      vistos.putIfAbsent(r.instructorCodigo, () => InstructorDisponible(codigo: r.instructorCodigo, nombre: r.instructorNombre));
    }
    return vistos.values.toList();
  }

  String _nombres(List<InstructorDisponible> lista) => lista.map((i) => i.nombre).join(', ');

  String _describir(Reserva r) => '${_cuando(r.fecha)} ${r.horaInicio} con ${r.instructorNombre}';

  String _cuando(DateTime fecha) {
    final hoy = soloFecha(_ahora());
    if (fecha == hoy) return 'hoy';
    if (fecha == sumarDias(hoy, 1)) return 'mañana';
    return 'el ${nombreDeDia(fecha.weekday)} ${fecha.day}/${fecha.month}';
  }

  String _momento(MomentoDia m) => switch (m) {
    MomentoDia.manana => 'por la mañana',
    MomentoDia.tarde => 'por la tarde',
    MomentoDia.noche => 'por la noche',
    MomentoDia.todos => 'a cualquier hora',
  };

  String _horas(int n) => n == 1 ? '1 hora' : '$n horas';

  String _clases(int n) => n == 1 ? '1 clase' : '$n clases';
}

/// El ejecutor de consultas del asistente (CU50). Un provider para poder sustituirlo en los tests.
final ejecutorDeConsultasProvider = Provider<EjecutorDeConsultas>((ref) => EjecutorDeConsultas(ref));
