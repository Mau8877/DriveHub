import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:mobile/core/network/pagina.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/models/franja_disponible.dart';
import 'package:mobile/features/clases/models/franja_rechazada.dart';
import 'package:mobile/features/clases/models/instructor_disponible.dart';
import 'package:mobile/features/clases/models/momento_dia.dart';
import 'package:mobile/features/clases/models/reserva.dart';
import 'package:mobile/features/clases/models/reservas_resultado.dart';
import 'package:mobile/features/clases/models/vista_reservas.dart';
import 'package:mobile/features/clases/providers/clases_service_provider.dart';
import 'package:mobile/features/clases/services/clases_service.dart';

/// Una franja libre de prueba. Está en el año 2099 para que nunca "ya haya empezado".
FranjaDisponible franjaDisponibleDePrueba({
  String codigo = 'f1',
  String horaInicio = '10:00',
  String horaFin = '11:00',
  String instructorCodigo = 'i1',
  String instructorNombre = 'Ivan Alfa',
  DateTime? fecha,
}) => FranjaDisponible(
  codigo: codigo,
  fecha: fecha ?? DateTime(2099, 11, 2),
  horaInicio: horaInicio,
  horaFin: horaFin,
  instructorCodigo: instructorCodigo,
  instructorNombre: instructorNombre,
);

InstructorDisponible instructorDePrueba({String codigo = 'i1', String nombre = 'Ivan Alfa'}) =>
    InstructorDisponible(codigo: codigo, nombre: nombre);

Reserva reservaDePrueba({
  String codigo = 'r1',
  DateTime? fecha,
  String horaInicio = '10:00',
  String horaFin = '11:00',
  String instructorNombre = 'Ivan Alfa',
  EstadoReserva estado = EstadoReserva.confirmada,
  bool terminada = false,
}) => Reserva(
  codigo: codigo,
  fecha: fecha ?? DateTime(2099, 11, 2),
  horaInicio: horaInicio,
  horaFin: horaFin,
  instructorCodigo: 'i1',
  instructorNombre: instructorNombre,
  estado: estado,
  terminada: terminada,
);

FranjaRechazada rechazadaDePrueba({
  String franjaCodigo = 'f2',
  String error = 'ERR_ACA_18',
  String mensaje = 'La franja ya no está disponible',
  String horaInicio = '11:00',
}) => FranjaRechazada(
  franjaCodigo: franjaCodigo,
  fecha: DateTime(2099, 11, 2),
  horaInicio: horaInicio,
  horaFin: '12:00',
  instructorNombre: 'Ivan Alfa',
  error: error,
  mensaje: mensaje,
);

/// Servicio de clases controlado desde el test: responde lo configurado y anota cada llamada.
///
/// * Un `Dio` vacío en el constructor: el real (ApiClient) necesita dotenv cargado.
class ClasesServiceFalso extends ClasesService {
  ClasesServiceFalso() : super(dio: Dio());

  List<FranjaDisponible> franjas = [];
  List<InstructorDisponible> instructores = [];
  List<Reserva> proximas = [];
  List<Reserva> historial = [];
  Object? errorFranjas;
  Object? errorInstructores;
  Object? errorReservas;

  /// Lo que responde `reservar`; por defecto, todas las franjas pedidas quedan reservadas.
  ReservasResultado Function(List<String> codigos)? alReservar;
  Object? errorAlReservar;
  Future<void> Function()? antesDeReservar;

  final List<({DateTime? fecha, MomentoDia momento, String? instructor})> pedidosDeFranjas = [];
  final List<DateTime?> pedidosDeInstructores = [];
  final List<List<String>> reservasPedidas = [];
  final List<VistaReservas> pedidosDeReservas = [];

  @override
  Future<Pagina<FranjaDisponible>> franjasDisponibles({
    DateTime? fecha,
    MomentoDia momento = MomentoDia.todos,
    String? instructorCodigo,
    int page = 1,
    int pageSize = 100,
  }) async {
    pedidosDeFranjas.add((fecha: fecha, momento: momento, instructor: instructorCodigo));
    final error = errorFranjas;
    if (error != null) throw error;
    return _pagina(franjas);
  }

  @override
  Future<List<InstructorDisponible>> instructoresDisponibles({DateTime? fecha}) async {
    pedidosDeInstructores.add(fecha);
    final error = errorInstructores;
    if (error != null) throw error;
    return instructores;
  }

  @override
  Future<ReservasResultado> reservar(List<String> franjaCodigos) async {
    reservasPedidas.add(List.of(franjaCodigos));
    await antesDeReservar?.call();
    final error = errorAlReservar;
    if (error != null) throw error;
    final respuesta = alReservar;
    if (respuesta != null) return respuesta(franjaCodigos);
    return ReservasResultado(
      reservadas: [for (final codigo in franjaCodigos) reservaDePrueba(codigo: 'r-$codigo')],
      rechazadas: const [],
      horasDisponibles: 0,
    );
  }

  @override
  Future<Pagina<Reserva>> reservas({required VistaReservas vista, int page = 1, int pageSize = 50}) async {
    pedidosDeReservas.add(vista);
    final error = errorReservas;
    if (error != null) throw error;
    return _pagina(vista == VistaReservas.proximas ? proximas : historial);
  }

  static Pagina<T> _pagina<T>(List<T> items) =>
      Pagina(items: items, page: 1, pageSize: 100, totalItems: items.length, totalPages: 1);
}

Override clasesServiceFalso(ClasesServiceFalso falso) => clasesServiceProvider.overrideWithValue(falso);
