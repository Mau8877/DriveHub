import 'dart:async';
import 'dart:math';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../models/pago_iniciado.dart';
import '../models/plan_disponible.dart';
import 'pagos_service_provider.dart';
import 'planes_provider.dart';

/// Una clave nueva para una intención de compra: 32 caracteres hexadecimales de un generador seguro.
///
/// * El backend la usa como `Idempotency-Key`: reintentar con la misma clave devuelve el mismo pago, nunca
/// * un segundo cobro. Cabe en el máximo de 64 caracteres que acepta el backend.
String generarClaveDeCompra([Random? azar]) {
  final generador = azar ?? Random.secure();
  return List.generate(
    16,
    (_) => generador.nextInt(256).toRadixString(16).padLeft(2, '0'),
  ).join();
}

/// De dónde sale la clave de cada compra; es un provider para fijarla en los tests.
final generadorDeClaveProvider = Provider<String Function()>(
  (ref) => generarClaveDeCompra,
);

/// La compra que el estudiante tiene entre manos: el plan elegido, la clave de idempotencia de esa
/// intención de compra y, cuando el backend la creó, el pago.
class CompraEnCurso {
  final PlanDisponible? plan;
  final String? clave;
  final PagoIniciado? pago;

  const CompraEnCurso({this.plan, this.clave, this.pago});

  static const vacia = CompraEnCurso();
}

/// Estado puro de UI de la compra en curso (no guarda datos del backend más que el pago recién creado).
///
/// * La clave NACE al elegir el plan y vive hasta que la compra termina: un toque doble o un reintento por
/// * mala red reenvían la misma clave. Elegir OTRO plan genera una nueva; volver a elegir el mismo la conserva
/// * (así se retoma la misma página de pago si todavía está vigente).
class CompraEnCursoNotifier extends Notifier<CompraEnCurso> {
  @override
  CompraEnCurso build() => CompraEnCurso.vacia;

  void elegirPlan(PlanDisponible plan) {
    if (state.plan?.codigo == plan.codigo) {
      state = CompraEnCurso(plan: plan, clave: state.clave, pago: state.pago);
      return;
    }
    state = CompraEnCurso(
      plan: plan,
      clave: ref.read(generadorDeClaveProvider)(),
    );
  }

  void registrarPago(PagoIniciado pago) {
    state = CompraEnCurso(plan: state.plan, clave: state.clave, pago: pago);
  }

  /// La compra terminó (confirmada, fallida o cancelada): la próxima usa una clave nueva.
  void terminar() => state = CompraEnCurso.vacia;
}

final compraUiProvider = NotifierProvider<CompraEnCursoNotifier, CompraEnCurso>(
  CompraEnCursoNotifier.new,
);

/// Iniciar la compra (CU26): pide al backend el pago y la página de pago de la pasarela.
///
/// * El estado es el pago recién creado, o el error (un `ApiException` con su código, o uno de red) para que
/// * la pantalla lo muestre. `iniciar` no lanza: devuelve `null` si no hay plan elegido, si ya hay una llamada
/// * en curso o si falló.
/// * Con `ERR_PAG_05` (el plan ya no está disponible) se vuelve a pedir la lista de planes.
class CompraNotifier extends AsyncNotifier<PagoIniciado?> {
  bool _enCurso = false;

  // * Síncrono a propósito: arranca ya en `AsyncData(null)` y el resultado de una compra no lo pisa un `build` tardío
  @override
  FutureOr<PagoIniciado?> build() => null;

  Future<PagoIniciado?> iniciar() async {
    final compra = ref.read(compraUiProvider);
    final plan = compra.plan;
    final clave = compra.clave;
    if (plan == null || clave == null || _enCurso) return null;

    _enCurso = true;
    state = const AsyncLoading<PagoIniciado?>().copyWithPrevious(state);
    try {
      final pago = await ref
          .read(pagosServiceProvider)
          .iniciarPago(plan.codigo, clave);
      ref.read(compraUiProvider.notifier).registrarPago(pago);
      state = AsyncData(pago);
      return pago;
    } catch (error, pila) {
      if (error is ApiException && error.code == 'ERR_PAG_05') {
        ref.invalidate(planesDisponiblesProvider);
      }
      state = AsyncError(error, pila);
      return null;
    } finally {
      _enCurso = false;
    }
  }

  /// Borra el error o el pago anterior (al elegir otro plan o empezar de nuevo).
  void limpiar() {
    if (!_enCurso) state = const AsyncData(null);
  }
}

final compraProvider = AsyncNotifierProvider<CompraNotifier, PagoIniciado?>(
  CompraNotifier.new,
);
