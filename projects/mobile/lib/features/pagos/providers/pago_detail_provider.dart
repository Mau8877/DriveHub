import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/pago_detalle.dart';
import 'pagos_service_provider.dart';

/// El detalle de una compra (CU28), por su código. `autoDispose`: cada vez que se abre se vuelve a pedir.
final pagoDetailProvider = FutureProvider.autoDispose.family<PagoDetalle, String>(
  (ref, codigo) => ref.watch(pagosServiceProvider).detallePago(codigo),
);
