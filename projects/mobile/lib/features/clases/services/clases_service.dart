import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/pagina.dart';
import '../../horarios/models/fechas.dart';
import '../models/franja_disponible.dart';
import '../models/instructor_disponible.dart';
import '../models/momento_dia.dart';
import '../models/reserva.dart';
import '../models/reservar_franjas_request.dart';
import '../models/reservas_resultado.dart';
import '../models/vista_reservas.dart';

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
  Future<ReservasResultado> reservar(List<String> franjaCodigos) {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/reservas',
        data: ReservarFranjasRequest(franjaCodigos: franjaCodigos).toJson(),
      );
      return ReservasResultado.fromJson(response.data!);
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
