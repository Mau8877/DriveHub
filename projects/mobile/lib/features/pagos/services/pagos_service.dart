import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/pagina.dart';
import '../models/estado_de_pago.dart';
import '../models/estado_pago_visible.dart';
import '../models/pago_detalle.dart';
import '../models/pago_resumen.dart';
import '../models/pago_iniciado.dart';
import '../models/plan_disponible.dart';

/// Llamadas a la API de la compra de horas (CU26): planes disponibles, iniciar la compra y consultar su estado.
///
/// * Un error de negocio del backend llega como [ApiException] (con su código `ERR_...`), no como un
/// * [DioException] crudo. Cualquier otro error (sin red, por ejemplo) se relanza tal cual.
class PagosService {
  final Dio _dio;

  PagosService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  Future<T> _llamar<T>(Future<T> Function() llamada) async {
    try {
      return await llamada();
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }

  /// Los planes activos de la escuela del estudiante, de menos a más horas.
  Future<Pagina<PlanDisponible>> planesDisponibles({
    int page = 1,
    int pageSize = 50,
  }) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/planes-pago/disponibles',
        queryParameters: {'page': page, 'pageSize': pageSize},
      );
      return Pagina.fromJson(response.data!, PlanDisponible.fromJson);
    });
  }

  /// Inicia la compra de un plan. [idempotencyKey] identifica ESTA intención de compra: reintentarla con la
  /// misma clave devuelve el mismo pago y la misma página de pago, nunca un segundo cobro.
  Future<PagoIniciado> iniciarPago(String planCodigo, String idempotencyKey) {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/pagos',
        data: {'planCodigo': planCodigo},
        options: Options(headers: {'Idempotency-Key': idempotencyKey}),
      );
      return PagoIniciado.fromJson(response.data!);
    });
  }

  Future<EstadoDePago> estadoDePago(String codigo) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/pagos/$codigo/estado',
      );
      return EstadoDePago.fromJson(response.data!);
    });
  }

  /// Las horas que el estudiante puede gastar hoy (su billetera).
  Future<int> saldoDeHoras() {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/billeteras-horas/mia',
      );
      return (response.data!['horasDisponibles'] as num).toInt();
    });
  }

  /// Las compras del estudiante (CU28), de la más reciente a la más antigua. [estado] filtra por el estado que
  /// ve el estudiante (`filter[estado]`); sin él trae todas.
  Future<Pagina<PagoResumen>> listarPagos({
    EstadoPagoVisible? estado,
    int page = 1,
    int pageSize = 20,
  }) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/pagos',
        queryParameters: {
          'page': page,
          'pageSize': pageSize,
          if (estado != null) 'filter[estado]': estado.valorApi,
        },
      );
      return Pagina.fromJson(response.data!, PagoResumen.fromJson);
    });
  }

  /// El detalle de una compra (CU28). Una compra ajena o inexistente llega como [ApiException] `ERR_PAG_07`.
  Future<PagoDetalle> detallePago(String codigo) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>('/pagos/$codigo');
      return PagoDetalle.fromJson(response.data!);
    });
  }
}
