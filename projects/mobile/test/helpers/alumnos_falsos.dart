import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:mobile/core/network/pagina.dart';
import 'package:mobile/features/alumnos/models/consumo_clase.dart';
import 'package:mobile/features/alumnos/models/hora_alumno.dart';
import 'package:mobile/features/alumnos/models/horas_alumno_detalle.dart';
import 'package:mobile/features/alumnos/models/pago_alumno.dart';
import 'package:mobile/features/alumnos/providers/horas_alumnos_service_provider.dart';
import 'package:mobile/features/alumnos/services/horas_alumnos_service.dart';
import 'package:mobile/features/pagos/models/estado_pago_visible.dart';

HoraAlumno alumnoDePrueba({
  String codigo = 'a1',
  String nombres = 'Ana',
  String apellidos = 'Pérez',
  int comprado = 20,
  int conmigo = 6,
  int restantes = 14,
}) => HoraAlumno(
  estudianteCodigo: codigo,
  nombres: nombres,
  apellidos: apellidos,
  horasCompradas: comprado,
  horasGastadasConmigo: conmigo,
  horasRestantes: restantes,
);

HorasAlumnoDetalle detalleDeAlumno({
  String codigo = 'a1',
  List<ConsumoClase>? consumos,
  List<PagoAlumno>? pagos,
}) => HorasAlumnoDetalle(
  resumen: alumnoDePrueba(codigo: codigo),
  consumos:
      consumos ??
      [ConsumoClase(claseCodigo: 'cl1', fecha: DateTime(2026, 5, 4, 10), horas: 2)],
  pagos:
      pagos ??
      [
        PagoAlumno(
          codigo: 'p1',
          planNombre: 'Plan 10 horas',
          horas: 10,
          estado: EstadoPagoVisible.confirmado,
          creadoEn: DateTime(2026, 4, 1, 9),
        ),
      ],
);

/// Servicio de las horas de los alumnos controlado desde el test: responde lo configurado y anota cada llamada.
///
/// * Un `Dio` vacío en el constructor: el real (ApiClient) necesita dotenv cargado.
class HorasAlumnosServiceFalso extends HorasAlumnosService {
  HorasAlumnosServiceFalso() : super(dio: Dio());

  /// Los alumnos: `listar` los filtra por el texto (como lo haría el backend) y los parte en páginas.
  List<HoraAlumno> alumnos = [];
  Object? errorListado;

  /// Si es verdadero, [errorListado] solo se lanza al pedir una página mayor a la 1.
  bool errorListadoSoloDesdeLaPagina2 = false;
  final List<({String? search, int page})> listados = [];

  Map<String, HorasAlumnoDetalle> detalles = {};
  Object? errorDetalle;
  final List<String> pedidosDeDetalle = [];

  @override
  Future<Pagina<HoraAlumno>> listar({String? search, int page = 1, int pageSize = 20}) async {
    listados.add((search: search, page: page));
    final error = errorListado;
    if (error != null && (!errorListadoSoloDesdeLaPagina2 || page > 1)) throw error;
    final texto = search?.trim().toLowerCase() ?? '';
    final filtrados = alumnos.where((a) => texto.isEmpty || a.nombreCompleto.toLowerCase().contains(texto)).toList();
    final total = filtrados.length;
    final paginas = total == 0 ? 1 : (total / pageSize).ceil();
    final desde = (page - 1) * pageSize;
    final items = desde >= total ? <HoraAlumno>[] : filtrados.sublist(desde, (desde + pageSize).clamp(0, total));
    return Pagina(items: items, page: page, pageSize: pageSize, totalItems: total, totalPages: paginas);
  }

  @override
  Future<HorasAlumnoDetalle> detalle(String estudianteCodigo) async {
    pedidosDeDetalle.add(estudianteCodigo);
    final error = errorDetalle;
    if (error != null) throw error;
    return detalles[estudianteCodigo] ?? detalleDeAlumno(codigo: estudianteCodigo);
  }
}

Override horasAlumnosServiceFalso(HorasAlumnosServiceFalso falso) => horasAlumnosServiceProvider.overrideWithValue(falso);
