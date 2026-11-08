import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:mobile/core/network/pagina.dart';
import 'package:mobile/features/clases/models/actualizar_clase_request.dart';
import 'package:mobile/features/clases/models/asistencia_clase.dart';
import 'package:mobile/features/clases/models/catalogo_pruebas.dart';
import 'package:mobile/features/clases/models/cerrar_clase_request.dart';
import 'package:mobile/features/clases/models/clase_agenda.dart';
import 'package:mobile/features/clases/models/clase_dictada.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/models/evaluar_clase_request.dart';
import 'package:mobile/features/clases/models/franja_disponible.dart';
import 'package:mobile/features/clases/models/franja_rechazada.dart';
import 'package:mobile/features/clases/models/hora_de_cancelacion.dart';
import 'package:mobile/features/clases/models/instructor_disponible.dart';
import 'package:mobile/features/clases/models/momento_dia.dart';
import 'package:mobile/features/clases/models/prueba_clase.dart';
import 'package:mobile/features/clases/models/reserva.dart';
import 'package:mobile/features/clases/models/reservas_resultado.dart';
import 'package:mobile/features/clases/models/resultado_cancelacion.dart';
import 'package:mobile/features/clases/models/resultado_cancelacion_instructor.dart';
import 'package:mobile/features/clases/models/tipo_prueba.dart';
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
  bool cancelacionATiempo = false,
}) => Reserva(
  codigo: codigo,
  fecha: fecha ?? DateTime(2099, 11, 2),
  horaInicio: horaInicio,
  horaFin: horaFin,
  instructorCodigo: 'i1',
  instructorNombre: instructorNombre,
  estado: estado,
  terminada: terminada,
  cancelacionATiempo: cancelacionATiempo,
);

/// Una clase de la agenda del instructor de prueba, en el año 2099 para que nunca "ya haya empezado".
ClaseAgenda claseDeAgendaDePrueba({
  String codigo = 'r1',
  DateTime? fecha,
  String horaInicio = '10:00',
  String horaFin = '11:00',
  String estudianteNombre = 'Eva Estudiante',
  EstadoReserva estado = EstadoReserva.confirmada,
  bool terminada = false,
  bool cancelable = false,
  bool cerrable = false,
  String? claseCodigo,
  bool evaluable = false,
  bool conPrueba = false,
}) => ClaseAgenda(
  codigo: codigo,
  fecha: fecha ?? DateTime(2099, 11, 2),
  horaInicio: horaInicio,
  horaFin: horaFin,
  estudianteCodigo: 'e1',
  estudianteNombre: estudianteNombre,
  estado: estado,
  terminada: terminada,
  cancelable: cancelable,
  cerrable: cerrable,
  claseCodigo: claseCodigo,
  evaluable: evaluable,
  conPrueba: conPrueba,
);

/// Una clase ya dictada y cerrada de prueba (CU17), en el año 2099.
ClaseDictada claseDictadaDePrueba({
  String codigo = 'c1',
  AsistenciaClase asistencia = AsistenciaClase.presente,
  String estudianteNombre = 'Eva Estudiante',
  String? tema,
  String? observaciones,
  int? kmInicio,
  int? kmFin,
}) => ClaseDictada(
  codigo: codigo,
  fecha: DateTime(2099, 11, 2),
  estudianteCodigo: 'e1',
  estudianteNombre: estudianteNombre,
  asistencia: asistencia,
  tema: tema,
  observaciones: observaciones,
  kmInicio: kmInicio,
  kmFin: kmFin,
);

/// El catálogo de criterios de prueba (CU18): tres tipos de dos criterios con máximo 10, para que los tests sean cortos.
CatalogoPruebas catalogoDePruebaDePrueba() => CatalogoPruebas(
  tipos: [
    for (final tipo in TipoPrueba.values)
      TipoDelCatalogo(
        tipo: tipo,
        etiqueta: tipo.etiqueta,
        criterios: [
          CriterioDelCatalogo(codigo: '${tipo.name}_a', descripcion: 'Criterio A de ${tipo.etiqueta}', maximo: 10),
          CriterioDelCatalogo(codigo: '${tipo.name}_b', descripcion: 'Criterio B de ${tipo.etiqueta}', maximo: 10),
        ],
      ),
  ],
);

/// Una prueba de clase ya guardada (CU18), con los dos criterios del catálogo de prueba.
PruebaClase pruebaClaseDePrueba({
  String codigo = 'p1',
  String claseCodigo = 'c1',
  TipoPrueba tipo = TipoPrueba.manejo,
  double puntaje = 8,
  String? observaciones,
}) {
  final total = puntaje * 10;
  return PruebaClase(
    codigo: codigo,
    claseCodigo: claseCodigo,
    tipo: tipo,
    criterios: [
      CriterioEvaluado(
        codigo: '${tipo.name}_a',
        descripcion: 'Criterio A de ${tipo.etiqueta}',
        puntaje: puntaje,
        maximo: 10,
      ),
      CriterioEvaluado(
        codigo: '${tipo.name}_b',
        descripcion: 'Criterio B de ${tipo.etiqueta}',
        puntaje: puntaje,
        maximo: 10,
      ),
    ],
    puntajeTotal: total,
    resultado: total >= 70 ? ResultadoPrueba.aprobado : ResultadoPrueba.reprobado,
    observaciones: observaciones,
    fecha: DateTime(2099, 11, 2),
  );
}

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

  /// Lo que responde `cancelar`; por defecto, la hora se devuelve y quedan 4 h.
  ResultadoCancelacion Function(String codigo)? alCancelar;
  Object? errorAlCancelar;
  Future<void> Function()? antesDeCancelar;
  Object? errorAlReprogramar;
  Future<void> Function()? antesDeReprogramar;

  final List<String> cancelacionesPedidas = [];
  final List<({String reserva, String franja})> reprogramacionesPedidas = [];
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
  Future<ReservasResultado> reservar(List<String> franjaCodigos, {String? descripcion, List<FranjaDisponible>? franjas}) async {
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
  Future<ResultadoCancelacion> cancelar(String codigoReserva, {String? descripcion}) async {
    cancelacionesPedidas.add(codigoReserva);
    await antesDeCancelar?.call();
    final error = errorAlCancelar;
    if (error != null) throw error;
    final respuesta = alCancelar;
    if (respuesta != null) return respuesta(codigoReserva);
    return ResultadoCancelacion(
      reserva: reservaDePrueba(codigo: codigoReserva, estado: EstadoReserva.cancelada),
      horaDevuelta: HoraDeCancelacion.devuelta,
      horasDisponibles: 4,
    );
  }

  @override
  Future<Reserva> reprogramar(String codigoReserva, String franjaCodigo, {String? descripcion}) async {
    reprogramacionesPedidas.add((reserva: codigoReserva, franja: franjaCodigo));
    await antesDeReprogramar?.call();
    final error = errorAlReprogramar;
    if (error != null) throw error;
    return reservaDePrueba(codigo: codigoReserva, cancelacionATiempo: true);
  }

  @override
  Future<Pagina<Reserva>> reservas({required VistaReservas vista, int page = 1, int pageSize = 50}) async {
    pedidosDeReservas.add(vista);
    final error = errorReservas;
    if (error != null) throw error;
    return _pagina(vista == VistaReservas.proximas ? proximas : historial);
  }

  /// La agenda del instructor (CU16): lo mismo para cualquier día; cada pedido anota el día.
  List<ClaseAgenda> clasesDeAgenda = [];
  Object? errorAgenda;
  final List<DateTime> pedidosDeAgenda = [];

  /// Lo que responde `cancelarComoInstructor`; por defecto, la hora se devuelve.
  ResultadoCancelacionInstructor Function(String codigo)? alCancelarComoInstructor;
  Object? errorAlCancelarComoInstructor;
  Future<void> Function()? antesDeCancelarComoInstructor;
  final List<String> cancelacionesDeInstructorPedidas = [];

  /// Lo que responde `cerrarClase`; por defecto, la clase queda cerrada con lo pedido.
  ClaseDictada Function(CerrarClaseRequest request)? alCerrarClase;
  Object? errorAlCerrarClase;
  Future<void> Function()? antesDeCerrarClase;
  final List<CerrarClaseRequest> cierresPedidos = [];

  /// Lo que responde `claseDictada` (lo guardado de la clase que se va a corregir).
  ClaseDictada claseGuardada = claseDictadaDePrueba();
  Object? errorClaseDictada;
  final List<String> clasesDictadasPedidas = [];

  /// Lo que responde `actualizarClase`; por defecto, la clase guardada con lo corregido.
  ClaseDictada Function(String codigo, ActualizarClaseRequest request)? alActualizarClase;
  Object? errorAlActualizarClase;
  Future<void> Function()? antesDeActualizarClase;
  final List<({String codigo, ActualizarClaseRequest request})> correccionesPedidas = [];

  @override
  Future<ClaseDictada> cerrarClase(CerrarClaseRequest request, {String? descripcion}) async {
    cierresPedidos.add(request);
    await antesDeCerrarClase?.call();
    final error = errorAlCerrarClase;
    if (error != null) throw error;
    final respuesta = alCerrarClase;
    if (respuesta != null) return respuesta(request);
    return claseDictadaDePrueba(
      asistencia: request.asistencia,
      tema: request.tema,
      observaciones: request.observaciones,
      kmInicio: request.kmInicio,
      kmFin: request.kmFin,
    );
  }

  @override
  Future<ClaseDictada> claseDictada(String codigoClase) async {
    clasesDictadasPedidas.add(codigoClase);
    final error = errorClaseDictada;
    if (error != null) throw error;
    return claseGuardada;
  }

  @override
  Future<ClaseDictada> actualizarClase(String codigoClase, ActualizarClaseRequest request, {String? descripcion}) async {
    correccionesPedidas.add((codigo: codigoClase, request: request));
    await antesDeActualizarClase?.call();
    final error = errorAlActualizarClase;
    if (error != null) throw error;
    final respuesta = alActualizarClase;
    if (respuesta != null) return respuesta(codigoClase, request);
    return claseDictadaDePrueba(
      codigo: codigoClase,
      asistencia: claseGuardada.asistencia,
      tema: request.tema ?? claseGuardada.tema,
      observaciones: request.observaciones ?? claseGuardada.observaciones,
      kmInicio: request.kmInicio ?? claseGuardada.kmInicio,
      kmFin: request.kmFin ?? claseGuardada.kmFin,
    );
  }

  /// Lo que responde `catalogoDePruebas` (CU18).
  CatalogoPruebas catalogo = catalogoDePruebaDePrueba();
  Object? errorCatalogo;
  int pedidosDeCatalogo = 0;

  /// Las pruebas que ya tiene cada clase (por código de clase) y lo que responde `evaluarClase`.
  final Map<String, List<PruebaClase>> pruebasPorClase = {};
  Object? errorPruebasDeClase;
  final List<String> pruebasPedidas = [];
  PruebaClase Function(EvaluarClaseRequest request)? alEvaluarClase;
  Object? errorAlEvaluarClase;
  Future<void> Function()? antesDeEvaluarClase;
  final List<EvaluarClaseRequest> evaluacionesPedidas = [];

  @override
  Future<CatalogoPruebas> catalogoDePruebas() async {
    pedidosDeCatalogo++;
    final error = errorCatalogo;
    if (error != null) throw error;
    return catalogo;
  }

  @override
  Future<List<PruebaClase>> pruebasDeClase(String claseCodigo) async {
    pruebasPedidas.add(claseCodigo);
    final error = errorPruebasDeClase;
    if (error != null) throw error;
    return pruebasPorClase[claseCodigo] ?? const [];
  }

  /// Por defecto, la prueba queda guardada con el puntaje de su primer criterio en todos (los tests lo controlan con
  /// [alEvaluarClase]) y se suma a las pruebas de esa clase.
  @override
  Future<PruebaClase> evaluarClase(EvaluarClaseRequest request, {String? descripcion}) async {
    evaluacionesPedidas.add(request);
    await antesDeEvaluarClase?.call();
    final error = errorAlEvaluarClase;
    if (error != null) throw error;
    final respuesta = alEvaluarClase;
    final prueba = respuesta != null
        ? respuesta(request)
        : pruebaClaseDePrueba(
            claseCodigo: request.claseCodigo,
            tipo: request.tipo,
            puntaje: request.criterios.first.puntaje,
          );
    pruebasPorClase.putIfAbsent(request.claseCodigo, () => []).add(prueba);
    return prueba;
  }

  @override
  Future<Pagina<ClaseAgenda>> agenda({required DateTime fecha, int page = 1, int pageSize = 50}) async {
    pedidosDeAgenda.add(fecha);
    final error = errorAgenda;
    if (error != null) throw error;
    return _pagina(clasesDeAgenda);
  }

  @override
  Future<ResultadoCancelacionInstructor> cancelarComoInstructor(String codigoReserva, {String? descripcion}) async {
    cancelacionesDeInstructorPedidas.add(codigoReserva);
    await antesDeCancelarComoInstructor?.call();
    final error = errorAlCancelarComoInstructor;
    if (error != null) throw error;
    final respuesta = alCancelarComoInstructor;
    if (respuesta != null) return respuesta(codigoReserva);
    return ResultadoCancelacionInstructor(
      clase: claseDeAgendaDePrueba(codigo: codigoReserva, estado: EstadoReserva.cancelada),
      horaDevuelta: HoraDeCancelacion.devuelta,
    );
  }

  static Pagina<T> _pagina<T>(List<T> items) =>
      Pagina(items: items, page: 1, pageSize: 100, totalItems: items.length, totalPages: 1);
}

Override clasesServiceFalso(ClasesServiceFalso falso) => clasesServiceProvider.overrideWithValue(falso);
