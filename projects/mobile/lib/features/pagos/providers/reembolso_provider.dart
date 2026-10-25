import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/estado_pago_visible.dart';
import '../models/motivo_reembolso.dart';
import 'pagos_service_provider.dart';
import 'refrescar_pagos.dart';

/// Cada cuánto se consulta una compra mientras su reembolso está en proceso. Es un provider para acortarlo en
/// los tests.
final intervaloConsultaReembolsoProvider = Provider<Duration>(
  (ref) => const Duration(seconds: 5),
);

/// Cuánto se sigue consultando antes de dejar de insistir (la persona puede volver a mirar más tarde).
final plazoConsultaReembolsoProvider = Provider<Duration>(
  (ref) => const Duration(minutes: 5),
);

/// Cómo va el reembolso de una compra desde esta pantalla (CU31).
///
/// * [ninguno]: no se pidió (o no se sigue ninguno).
/// * [enProceso]: pedido; se espera el aviso de la pasarela.
/// * [reembolsado]: terminó; el dinero se devolvió.
/// * [fallo]: la devolución falló y la compra volvió a estar confirmada, con sus horas.
enum ResultadoReembolso { ninguno, enProceso, reembolsado, fallo }

/// Pide el reembolso de una compra y sigue su estado hasta que termina (CU31).
///
/// * `solicitar` no lanza: devuelve `true` si el backend lo aceptó; si falló deja el error en el estado
///   (un `ApiException` con su código, o uno de red) para que la pantalla lo muestre. Atiende un pedido a la vez.
/// * Al aceptarse (y al terminar el seguimiento) refresca el saldo, el listado y el detalle con [refrescarPagos].
/// * `vigilar` consulta el detalle cada pocos segundos mientras sigue `REEMBOLSO_EN_PROCESO`; es idempotente.
/// * ! El tiempo se mide sumando los intervalos, no con el reloj del teléfono.
class ReembolsoNotifier
    extends AutoDisposeFamilyAsyncNotifier<ResultadoReembolso, String> {
  bool _enCurso = false;
  bool _vigilando = false;
  bool _vivo = true;
  Timer? _temporizador;
  Duration _transcurrido = Duration.zero;

  @override
  FutureOr<ResultadoReembolso> build(String arg) {
    _vivo = true;
    ref.onDispose(() {
      _vivo = false;
      _temporizador?.cancel();
    });
    return ResultadoReembolso.ninguno;
  }

  Future<bool> solicitar(
    MotivoReembolso motivo, {
    String? motivoDetalle,
  }) async {
    if (_enCurso) return false;
    _enCurso = true;
    state = const AsyncLoading<ResultadoReembolso>().copyWithPrevious(state);
    try {
      await ref
          .read(pagosServiceProvider)
          .solicitarReembolso(arg, motivo, motivoDetalle: motivoDetalle);
      if (!_vivo) return true;
      state = const AsyncData(ResultadoReembolso.enProceso);
      refrescarPagos(ref.invalidate, ref.exists);
      vigilar();
      return true;
    } catch (error, pila) {
      if (_vivo) state = AsyncError(error, pila);
      return false;
    } finally {
      _enCurso = false;
    }
  }

  /// Sigue la compra hasta que su reembolso termine o pase el plazo. No hace nada si ya la está siguiendo.
  void vigilar() {
    if (_vigilando || !_vivo) return;
    _vigilando = true;
    _transcurrido = Duration.zero;
    if (state.valueOrNull == ResultadoReembolso.ninguno) {
      state = const AsyncData(ResultadoReembolso.enProceso);
    }
    _programar();
  }

  void _programar() {
    final intervalo = ref.read(intervaloConsultaReembolsoProvider);
    _temporizador?.cancel();
    _temporizador = Timer(intervalo, () {
      _transcurrido += intervalo;
      unawaited(_ciclo());
    });
  }

  Future<void> _ciclo() async {
    final terminado = await _consultar();
    if (!_vivo) return;
    if (terminado || _transcurrido >= ref.read(plazoConsultaReembolsoProvider)) {
      _vigilando = false;
      return;
    }
    _programar();
  }

  /// Consulta la compra. Devuelve `true` si el reembolso ya terminó; un error de red no corta el seguimiento.
  Future<bool> _consultar() async {
    try {
      final detalle = await ref.read(pagosServiceProvider).detallePago(arg);
      if (!_vivo) return true;
      final resultado = switch (detalle.estado) {
        EstadoPagoVisible.reembolsado => ResultadoReembolso.reembolsado,
        EstadoPagoVisible.confirmado => ResultadoReembolso.fallo,
        _ => null,
      };
      if (resultado == null) return false;
      state = AsyncData(resultado);
      refrescarPagos(ref.invalidate, ref.exists);
      return true;
    } catch (_) {
      return !_vivo;
    }
  }
}

final reembolsoProvider = AsyncNotifierProvider.autoDispose
    .family<ReembolsoNotifier, ResultadoReembolso, String>(
      ReembolsoNotifier.new,
    );
