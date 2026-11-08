import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/pagina.dart';
import '../../../core/offline/modelos_offline.dart';
import '../../horarios/models/fechas.dart';
import '../models/actualizar_clase_request.dart';
import '../widgets/pendientes_offline.dart';
import '../models/catalogo_pruebas.dart';
import '../models/cerrar_clase_request.dart';
import '../models/clase_agenda.dart';
import '../models/clase_dictada.dart';
import '../models/evaluar_clase_request.dart';
import '../models/franja_disponible.dart';
import '../models/instructor_disponible.dart';
import '../models/momento_dia.dart';
import '../models/prueba_clase.dart';
import '../models/reprogramar_request.dart';
import '../models/reserva.dart';
import '../models/reservar_franjas_request.dart';
import '../models/resultado_cancelacion.dart';
import '../models/resultado_cancelacion_instructor.dart';
import '../models/reservas_resultado.dart';
import '../models/vista_reservas.dart';

/// El tipo de la acción «reservar clases» en la cola offline: su respuesta se revisa porque un rechazo parcial
/// (una franja ya tomada) llega como 200. Ver `verificarReservaOffline`.
const tipoReservarClases = 'reservar-clases';

/// Llamadas a la API de las clases del estudiante (CU14): ver las franjas libres de los instructores de su escuela,
/// confirmar reservas y listar sus clases.
///
/// * Un error de negocio del backend llega como [ApiException] (con su código `ERR_...`), no como un
/// * [DioException] crudo. Cualquier otro error (sin red, por ejemplo) se relanza tal cual.
class ClasesService {
  final Dio _dio;

  ClasesService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  Future<T> _llamar<T>(Future<T> Function() llamada) async {
    try {
      return await llamada();
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      if (error is AccionEncolada) throw error;
      rethrow;
    }
  }

  /// Las franjas libres que todavía no empezaron, de la escuela del estudiante, por fecha y hora.
  ///
  /// * Los filtros nulos no viajan. [momento] se traduce a una hora desde (inclusiva) y una hora hasta (exclusiva)
  /// * sobre la hora de inicio; la noche no manda hora hasta.
  Future<Pagina<FranjaDisponible>> franjasDisponibles({
    DateTime? fecha,
    MomentoDia momento = MomentoDia.todos,
    String? instructorCodigo,
    int page = 1,
    int pageSize = 100,
  }) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/franjas/disponibles',
        queryParameters: {
          'page': page,
          'pageSize': pageSize,
          if (fecha != null) 'filter[fecha]': fechaParaApi(fecha),
          'filter[horaDesde]': ?momento.horaDesde,
          'filter[horaHasta]': ?momento.horaHasta,
          'filter[instructor]': ?instructorCodigo,
        },
      );
      return Pagina.fromJson(response.data!, FranjaDisponible.fromJson);
    });
  }

  /// Los instructores de la escuela con alguna franja libre (en [fecha], si se indica), por nombre.
  Future<List<InstructorDisponible>> instructoresDisponibles({DateTime? fecha}) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/franjas/instructores',
        queryParameters: {if (fecha != null) 'filter[fecha]': fechaParaApi(fecha)},
      );
      return (response.data!['data'] as List<dynamic>)
          .map((e) => InstructorDisponible.fromJson(e as Map<String, dynamic>))
          .toList();
    });
  }

  /// Confirma las franjas elegidas, una hora de la billetera por cada una. La respuesta dice cuáles quedaron
  /// reservadas y cuáles no (con su motivo): un rechazo parcial NO es un error.
  ///
  /// * Se puede hacer sin conexión ([tipoReservarClases]): queda en la cola y el servidor decide al volver internet
  /// * cuáles franjas siguen libres; lo que rechace se avisa (el verificador revisa la respuesta, porque un rechazo
  /// * parcial llega como 200).
  Future<ReservasResultado> reservar(
    List<String> franjaCodigos, {
    String? descripcion,
    List<FranjaDisponible>? franjas,
  }) {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/reservas',
        data: ReservarFranjasRequest(franjaCodigos: franjaCodigos).toJson(),
        options: OpcionesOffline.escritura(
          descripcion ?? (franjaCodigos.length == 1 ? 'Reserva de 1 clase' : 'Reserva de ${franjaCodigos.length} clases'),
          tipo: tipoReservarClases,
          efecto: EfectoOffline.reservar,
          objetivos: franjaCodigos,
          datos: franjas == null ? null : {'franjas': franjas.map(datosDeFranja).toList()},
        ),
      );
      return ReservasResultado.fromJson(response.data!);
    });
  }

  /// Cancela una clase (CU15). A tiempo se devuelve la hora; tarde, la cancelación se permite pero la hora se pierde;
  /// si Pagos no responde, la cancelación se acepta y la hora se devuelve sola ([ResultadoCancelacion.horaDevuelta]).
  ///
  /// * Se puede hacer sin conexión: el servidor decide al recibirla si fue a tiempo (y si la hora se devuelve).
  Future<ResultadoCancelacion> cancelar(String codigoReserva, {String? descripcion}) {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/reservas/$codigoReserva/cancelar',
        options: OpcionesOffline.escritura(
          descripcion ?? 'Cancelación de clase',
          efecto: EfectoOffline.cancelar,
          objetivos: [codigoReserva],
        ),
      );
      return ResultadoCancelacion.fromJson(response.data!);
    });
  }

  /// Mueve una clase a otra franja libre (CU15), sin tocar las horas. Devuelve la clase ya en la franja nueva.
  ///
  /// * Se puede hacer sin conexión: el servidor revisa al recibirla que la franja nueva siga libre.
  Future<Reserva> reprogramar(
    String codigoReserva,
    String franjaCodigo, {
    String? descripcion,
  }) {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/reservas/$codigoReserva/reprogramar',
        data: ReprogramarRequest(franjaCodigo: franjaCodigo).toJson(),
        options: OpcionesOffline.escritura(
          descripcion ?? 'Reprogramación de clase',
          efecto: EfectoOffline.reprogramar,
          objetivos: [codigoReserva, franjaCodigo],
        ),
      );
      return Reserva.fromJson(response.data!);
    });
  }

  /// La agenda del instructor (CU16): sus clases de un [fecha], por hora de inicio.
  Future<Pagina<ClaseAgenda>> agenda({required DateTime fecha, int page = 1, int pageSize = 50}) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/reservas/agenda',
        queryParameters: {'page': page, 'pageSize': pageSize, 'filter[fecha]': fechaParaApi(fecha)},
      );
      return Pagina.fromJson(response.data!, ClaseAgenda.fromJson);
    });
  }

  /// Cancela una clase como instructor (CU16): sin plazo, el estudiante siempre recupera su hora y la franja queda
  /// bloqueada. Si Pagos no responde, la cancelación se acepta y la hora queda «en proceso».
  ///
  /// * Se puede hacer sin conexión.
  Future<ResultadoCancelacionInstructor> cancelarComoInstructor(
    String codigoReserva, {
    String? descripcion,
  }) {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/reservas/$codigoReserva/cancelar-por-instructor',
        options: OpcionesOffline.escritura(
          descripcion ?? 'Cancelación de clase',
          efecto: EfectoOffline.cancelar,
          objetivos: [codigoReserva],
        ),
      );
      return ResultadoCancelacionInstructor.fromJson(response.data!);
    });
  }

  /// Cierra una clase que ya empezó (CU17): la asistencia y, si asistió, el tema, las observaciones y los kilómetros.
  /// La reserva queda completada o con inasistencia; la hora del estudiante nunca se devuelve.
  ///
  /// * Se puede hacer sin conexión: si la petición no sale, queda en la cola con [descripcion] (lo que ve la persona
  /// * en la lista de pendientes) y se lanza [AccionEncolada]. Al volver internet el servidor la valida igual que
  /// * si se hubiera enviado en el momento.
  Future<ClaseDictada> cerrarClase(CerrarClaseRequest request, {String? descripcion}) {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/clases',
        data: request.toJson(),
        options: OpcionesOffline.escritura(
          descripcion ?? 'Cierre de clase',
          efecto: EfectoOffline.cerrar,
          objetivos: [request.reservaCodigo],
        ),
      );
      return ClaseDictada.fromJson(response.data!);
    });
  }

  /// Una clase ya cerrada, con lo guardado (CU17), para corregirla.
  Future<ClaseDictada> claseDictada(String codigoClase) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>('/clases/$codigoClase');
      return ClaseDictada.fromJson(response.data!);
    });
  }

  /// Corrige el tema, las observaciones o los kilómetros de una clase cerrada (CU17). Solo viaja lo que cambió.
  ///
  /// * Se puede hacer sin conexión.
  Future<ClaseDictada> actualizarClase(
    String codigoClase,
    ActualizarClaseRequest request, {
    String? descripcion,
  }) {
    return _llamar(() async {
      final response = await _dio.patch<Map<String, dynamic>>(
        '/clases/$codigoClase',
        data: request.toJson(),
        options: OpcionesOffline.escritura(
          descripcion ?? 'Corrección de clase',
          efecto: EfectoOffline.corregir,
          objetivos: [codigoClase],
        ),
      );
      return ClaseDictada.fromJson(response.data!);
    });
  }

  /// Los criterios con que se puntúa cada tipo de prueba de clase (CU18).
  Future<CatalogoPruebas> catalogoDePruebas() {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>('/pruebas-clase/criterios');
      return CatalogoPruebas.fromJson(response.data!);
    });
  }

  /// Evalúa una clase ya cerrada con asistencia (CU18): el puntaje de cada criterio del tipo elegido. La nota entra
  /// en el historial del estudiante.
  ///
  /// * Se puede hacer sin conexión.
  Future<PruebaClase> evaluarClase(EvaluarClaseRequest request, {String? descripcion}) {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/pruebas-clase',
        data: request.toJson(),
        options: OpcionesOffline.escritura(
          descripcion ?? 'Evaluación de clase',
          efecto: EfectoOffline.evaluar,
          objetivos: [request.claseCodigo],
        ),
      );
      return PruebaClase.fromJson(response.data!);
    });
  }

  /// Las pruebas de una clase (CU18), en el orden en que se hicieron. Una clase tiene a lo sumo una por tipo, así
  /// que una página alcanza.
  Future<List<PruebaClase>> pruebasDeClase(String claseCodigo) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/pruebas-clase',
        queryParameters: {'filter[claseCodigo]': claseCodigo, 'pageSize': 20},
      );
      return Pagina.fromJson(response.data!, PruebaClase.fromJson).items;
    });
  }

  /// Las clases del estudiante: las próximas (de la más cercana a la más lejana) o el historial (de la más
  /// reciente a la más antigua).
  Future<Pagina<Reserva>> reservas({required VistaReservas vista, int page = 1, int pageSize = 50}) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/reservas',
        queryParameters: {'page': page, 'pageSize': pageSize, 'filter[vista]': vista.valorApi},
      );
      return Pagina.fromJson(response.data!, Reserva.fromJson);
    });
  }
}
