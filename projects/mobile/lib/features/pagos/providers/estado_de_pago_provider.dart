import 'dart:async';
import 'dart:math';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/estado_de_pago.dart';
import 'pagos_service_provider.dart';

/// Cada cuánto se consulta el estado de un pago mientras sigue pendiente. Es un provider para acortarlo en los
/// tests.
final intervaloConsultaEstadoProvider = Provider<Duration>((ref) => const Duration(seconds: 3));

/// Cuánto se sigue consultando después de que la página de pago venció, por si el aviso de la pasarela llega tarde.
const margenTrasElVencimiento = Duration(minutes: 2);

/// Qué pago se sigue y hasta cuándo se espera. Es la clave de la familia de providers: dos con el mismo pago y la
/// misma hora de vencimiento comparten el seguimiento.
class ConsultaDeEstado {
  final String codigo;
  final DateTime expiraEn;

  const ConsultaDeEstado(this.codigo, this.expiraEn);

  @override
  bool operator ==(Object other) =>
      other is ConsultaDeEstado && other.codigo == codigo && other.expiraEn == expiraEn;

  @override
  int get hashCode => Object.hash(codigo, expiraEn);
}

/// Lo último que se supo del pago. [sinNovedades] es verdadero cuando se dejó de consultar (pasó el plazo) y el
/// pago seguía pendiente: la pantalla le dice a la persona que puede ver el estado más tarde.
class SeguimientoDePago {
  final EstadoDePago estado;
  final bool sinNovedades;

  const SeguimientoDePago(this.estado, {this.sinNovedades = false});
}

/// Consulta el estado de un pago cada pocos segundos mientras sea `PENDIENTE` (CU26).
///
/// * Se detiene al confirmarse, fallar o reembolsarse, al pasar el vencimiento más [margenTrasElVencimiento]
/// * (queda `sinNovedades`) y al desecharse el provider.
/// * Un error de red en una consulta NO corta el seguimiento: se reintenta en el siguiente ciclo. Solo si falla
/// * la PRIMERA consulta el provider queda en error (la pantalla ofrece reintentar).
/// * [refrescar] consulta de inmediato: se usa al volver a la app desde la página de pago.
/// * ! El tiempo se mide sumando los intervalos, no con el reloj del teléfono: así no depende de que la hora esté bien.
class EstadoDePagoNotifier extends AutoDisposeFamilyAsyncNotifier<SeguimientoDePago, ConsultaDeEstado> {
  Timer? _temporizador;
  late Duration _intervalo;
  late Duration _limite;
  Duration _transcurrido = Duration.zero;
  bool _vivo = true;

  @override
  Future<SeguimientoDePago> build(ConsultaDeEstado arg) async {
    _vivo = true;
    _intervalo = ref.read(intervaloConsultaEstadoProvider);
    final faltaParaVencer = arg.expiraEn.difference(DateTime.now());
    _limite = Duration(milliseconds: max(0, faltaParaVencer.inMilliseconds)) + margenTrasElVencimiento;
    _transcurrido = Duration.zero;
    ref.onDispose(() {
      _vivo = false;
      _temporizador?.cancel();
    });

    final primero = await _consultar();
    if (!primero.estado.esFinal) _programar();
    return SeguimientoDePago(primero);
  }

  Future<EstadoDePago> _consultar() => ref.read(pagosServiceProvider).estadoDePago(arg.codigo);

  void _programar() {
    _temporizador?.cancel();
    _temporizador = Timer(_intervalo, () {
      _transcurrido += _intervalo;
      unawaited(_ciclo());
    });
  }

  Future<void> _ciclo() async {
    if (!await _actualizar()) return;
    if (_transcurrido >= _limite) {
      final actual = state.valueOrNull;
      if (_vivo && actual != null) state = AsyncData(SeguimientoDePago(actual.estado, sinNovedades: true));
      return;
    }
    _programar();
  }

  /// Consulta y publica el resultado. Devuelve `false` si el seguimiento terminó (el estado es final o el
  /// provider ya no existe); un error de red devuelve `true`: se sigue intentando.
  Future<bool> _actualizar() async {
    try {
      final nuevo = await _consultar();
      if (!_vivo) return false;
      state = AsyncData(SeguimientoDePago(nuevo));
      return !nuevo.estado.esFinal;
    } catch (_) {
      return _vivo;
    }
  }

  /// Consulta ahora mismo. Si el pago sigue pendiente, retoma las consultas periódicas.
  Future<void> refrescar() async {
    _temporizador?.cancel();
    if (!await _actualizar()) return;
    if (_transcurrido >= _limite) {
      final actual = state.valueOrNull;
      if (_vivo && actual != null) state = AsyncData(SeguimientoDePago(actual.estado, sinNovedades: true));
      return;
    }
    _programar();
  }
}

final estadoDePagoProvider = AsyncNotifierProvider.autoDispose
    .family<EstadoDePagoNotifier, SeguimientoDePago, ConsultaDeEstado>(EstadoDePagoNotifier.new);
