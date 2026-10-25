import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:mobile/core/network/pagina.dart';
import 'package:mobile/features/pagos/models/estado_de_pago.dart';
import 'package:mobile/features/pagos/models/estado_pago.dart';
import 'package:mobile/features/pagos/models/pago_iniciado.dart';
import 'package:mobile/features/pagos/models/plan_disponible.dart';
import 'package:mobile/features/pagos/providers/pagos_service_provider.dart';
import 'package:mobile/features/pagos/services/pagos_service.dart';

PlanDisponible planDePrueba({String codigo = 'p10', String nombre = 'Plan 10 horas', int horas = 10, double costoBs = 450, double costoUsd = 38.23}) =>
    PlanDisponible(codigo: codigo, nombre: nombre, horas: horas, costoBs: costoBs, costoUsd: costoUsd);

PagoIniciado pagoDePrueba({
  String codigo = 'g1',
  EstadoPago estado = EstadoPago.pendiente,
  String? urlPago = 'https://checkout.stripe.com/c/pay/cs_test_1',
  DateTime? expiraEn,
  String planNombre = 'Plan 10 horas',
  int horas = 10,
}) => PagoIniciado(
  codigo: codigo,
  estado: estado,
  urlPago: urlPago,
  expiraEn: expiraEn ?? DateTime.now().add(const Duration(minutes: 30)),
  planNombre: planNombre,
  horas: horas,
  montoUsd: 38.23,
  montoBs: 450,
);

/// Servicio de pagos controlado desde el test: responde lo configurado y anota cada llamada.
///
/// * Un `Dio` vacío en el constructor: el real (ApiClient) necesita dotenv cargado.
class PagosServiceFalso extends PagosService {
  PagosServiceFalso() : super(dio: Dio());

  List<PlanDisponible> planes = [];
  Object? errorPlanes;
  int pedidosDePlanes = 0;

  /// Lo que responde `iniciarPago`; por defecto, un pago pendiente nuevo.
  PagoIniciado Function()? pagoAIniciar;
  Object? errorAlIniciar;

  /// Si es verdadero, [errorAlIniciar] se lanza una sola vez (después responde bien).
  bool errorAlIniciarUnaVez = false;
  Future<void> Function()? antesDeIniciar;
  final List<({String plan, String clave})> inicios = [];

  /// Los estados que va informando `estadoDePago`, en orden; el último se repite.
  List<EstadoPago> estados = [EstadoPago.pendiente];

  /// Errores que `estadoDePago` lanza en las primeras consultas, uno por consulta (un `null` es una consulta buena).
  List<Object?> erroresDeEstado = [];
  final List<String> consultasDeEstado = [];

  /// Las horas de la billetera que responde `saldoDeHoras`; [errorSaldo] hace que falle.
  int saldo = 0;
  Object? errorSaldo;
  int pedidosDeSaldo = 0;

  @override
  Future<int> saldoDeHoras() async {
    pedidosDeSaldo++;
    final error = errorSaldo;
    if (error != null) throw error;
    return saldo;
  }

  @override
  Future<Pagina<PlanDisponible>> planesDisponibles({int page = 1, int pageSize = 50}) async {
    pedidosDePlanes++;
    final error = errorPlanes;
    if (error != null) throw error;
    return Pagina(items: planes, page: 1, pageSize: pageSize, totalItems: planes.length, totalPages: 1);
  }

  @override
  Future<PagoIniciado> iniciarPago(String planCodigo, String idempotencyKey) async {
    inicios.add((plan: planCodigo, clave: idempotencyKey));
    await antesDeIniciar?.call();
    final error = errorAlIniciar;
    if (error != null) {
      if (errorAlIniciarUnaVez) errorAlIniciar = null;
      throw error;
    }
    return (pagoAIniciar ?? pagoDePrueba).call();
  }

  @override
  Future<EstadoDePago> estadoDePago(String codigo) async {
    consultasDeEstado.add(codigo);
    if (erroresDeEstado.isNotEmpty) {
      final error = erroresDeEstado.removeAt(0);
      if (error != null) throw error;
    }
    final estado = estados.length > 1 ? estados.removeAt(0) : estados.first;
    return EstadoDePago(codigo: codigo, estado: estado);
  }
}

Override pagosServiceFalso(PagosServiceFalso falso) => pagosServiceProvider.overrideWithValue(falso);
