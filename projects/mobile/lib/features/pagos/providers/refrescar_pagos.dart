import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'pago_detail_provider.dart';
import 'pagos_list_provider.dart';
import 'saldo_horas_provider.dart';

/// Vuelve a pedir el saldo de horas, el historial y el detalle abierto: el único punto para refrescarlos
/// cuando algo cambia (una compra confirmada, un reembolso).
///
/// * Recibe `ref.invalidate` y `ref.exists` (de un `Ref` o de un `WidgetRef`):
///   `refrescarPagos(ref.invalidate, ref.exists)`.
/// * Solo invalida lo que ya se pidió alguna vez: lo que nadie leyó se pedirá fresco cuando se abra.
void refrescarPagos(
  void Function(ProviderOrFamily) invalidar,
  bool Function(ProviderBase<Object?>) existe,
) {
  if (existe(saldoHorasProvider)) invalidar(saldoHorasProvider);
  if (existe(pagosListProvider)) invalidar(pagosListProvider);
  invalidar(pagoDetailProvider);
}
