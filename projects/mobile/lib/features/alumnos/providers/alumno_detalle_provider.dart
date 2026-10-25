import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/horas_alumno_detalle.dart';
import 'horas_alumnos_service_provider.dart';

/// El detalle de un alumno (CU29), por su código de estudiante. `autoDispose`: cada vez que se abre se vuelve a
/// pedir.
final alumnoDetalleProvider = FutureProvider.autoDispose
    .family<HorasAlumnoDetalle, String>(
      (ref, codigo) => ref.watch(horasAlumnosServiceProvider).detalle(codigo),
    );
