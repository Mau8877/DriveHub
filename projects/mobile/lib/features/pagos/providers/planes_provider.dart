import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/plan_disponible.dart';
import 'pagos_service_provider.dart';

/// Los planes activos de la escuela del estudiante, de menos a más horas (CU26).
///
/// * Pide la página 1 de 50: una escuela ofrece muy pocos planes.
/// * `autoDispose`: cada vez que se abre «Comprar horas» se vuelve a pedir, así un plan desactivado desde
/// * el panel no sigue ofreciéndose.
final planesDisponiblesProvider =
    FutureProvider.autoDispose<List<PlanDisponible>>((ref) async {
      final pagina = await ref
          .watch(pagosServiceProvider)
          .planesDisponibles(pageSize: 50);
      return pagina.items;
    });
