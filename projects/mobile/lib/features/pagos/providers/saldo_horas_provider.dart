import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'pagos_service_provider.dart';

/// Las horas disponibles en la billetera del estudiante (CU27), para el encabezado.
///
/// * `autoDispose`: vive mientras el encabezado está en pantalla; al cerrar sesión se descarta y otra persona no ve
/// * el saldo anterior.
/// * Se vuelve a pedir (`ref.invalidate`) cuando un pago se confirma y suma horas.
final saldoHorasProvider = FutureProvider.autoDispose<int>((ref) {
  return ref.watch(pagosServiceProvider).saldoDeHoras();
});
