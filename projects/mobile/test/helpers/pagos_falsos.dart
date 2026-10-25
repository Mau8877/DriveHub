import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:mobile/core/network/pagina.dart';
import 'package:mobile/features/pagos/models/estado_de_pago.dart';
import 'package:mobile/features/pagos/models/estado_pago.dart';
import 'package:mobile/features/pagos/models/estado_pago_visible.dart';
import 'package:mobile/features/pagos/models/motivo_reembolso.dart';
import 'package:mobile/features/pagos/models/pago_detalle.dart';
import 'package:mobile/features/pagos/models/reembolso_previsto.dart';
import 'package:mobile/features/pagos/models/pago_resumen.dart';
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

PagoResumen compraDePrueba({
  String codigo = 'c1',
  String planNombre = 'Plan 10 horas',
  int horas = 10,
  EstadoPagoVisible estado = EstadoPagoVisible.confirmado,
  DateTime? creadoEn,
}) => PagoResumen(
  codigo: codigo,
  planNombre: planNombre,
  horas: horas,
  montoUsd: 38.23,
  montoBs: 450,
  estado: estado,
  creadoEn: creadoEn ?? DateTime(2026, 5, 4, 12),
);

PagoDetalle detalleDePrueba({
  String codigo = 'c1',
  EstadoPagoVisible estado = EstadoPagoVisible.confirmado,
  int? horasRestantes = 7,
  bool confirmado = true,
  bool reembolsable = false,
}) =>
    PagoDetalle(
      codigo: codigo,
      planNombre: 'Plan 10 horas',
      horas: 10,
      montoUsd: 38.23,
      montoBs: 450,
      estado: estado,
      creadoEn: DateTime(2026, 5, 4, 12),
      horasRestantes: horasRestantes,
      confirmadoEn: confirmado ? DateTime(2026, 5, 4, 12, 1) : null,
      reembolsable: reembolsable,
      reembolso: reembolsable
          ? const ReembolsoPrevisto(montoDevuelto: 36.32, montoRetenido: 1.91, porcentajeRetenido: 5)
          : null,
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

  /// Las compras del historial: `listarPagos` las filtra por estado y las parte en páginas del tamaño pedido.
  List<PagoResumen> compras = [];
  Object? errorListado;

  /// Si es verdadero, [errorListado] solo se lanza al pedir una página mayor a la 1.
  bool errorListadoSoloDesdeLaPagina2 = false;
  final List<({EstadoPagoVisible? estado, int page})> listados = [];

  /// Los detalles que responde `detallePago`, por código; [errorDetalle] hace que falle.
  Map<String, PagoDetalle> detalles = {};
  Object? errorDetalle;
  int pedidosDeDetalle = 0;

  @override
  Future<Pagina<PagoResumen>> listarPagos({EstadoPagoVisible? estado, int page = 1, int pageSize = 20}) async {
    listados.add((estado: estado, page: page));
    final error = errorListado;
    if (error != null && (!errorListadoSoloDesdeLaPagina2 || page > 1)) throw error;
    final filtradas = compras.where((c) => estado == null || c.estado == estado).toList();
    final total = filtradas.length;
    final paginas = total == 0 ? 1 : (total / pageSize).ceil();
    final desde = (page - 1) * pageSize;
    final items = desde >= total ? <PagoResumen>[] : filtradas.sublist(desde, (desde + pageSize).clamp(0, total));
    return Pagina(items: items, page: page, pageSize: pageSize, totalItems: total, totalPages: paginas);
  }

  /// Lo que responde `solicitarReembolso`; [errorReembolso] hace que falle. Anota cada pedido.
  Object? errorReembolso;
  Future<void> Function()? antesDeReembolsar;
  final List<({String codigo, MotivoReembolso motivo, String? detalle})> reembolsos = [];

  @override
  Future<PagoDetalle> solicitarReembolso(String codigo, MotivoReembolso motivo, {String? motivoDetalle}) async {
    reembolsos.add((codigo: codigo, motivo: motivo, detalle: motivoDetalle));
    await antesDeReembolsar?.call();
    final error = errorReembolso;
    if (error != null) throw error;
    final enProceso = detalleDePrueba(codigo: codigo, estado: EstadoPagoVisible.reembolsoEnProceso, horasRestantes: 0);
    detalles = {...detalles, codigo: enProceso};
    return enProceso;
  }

  @override
  Future<PagoDetalle> detallePago(String codigo) async {
    pedidosDeDetalle++;
    final error = errorDetalle;
    if (error != null) throw error;
    return detalles[codigo] ?? detalleDePrueba(codigo: codigo);
  }

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
