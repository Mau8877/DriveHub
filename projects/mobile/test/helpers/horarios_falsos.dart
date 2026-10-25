import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:mobile/core/network/pagina.dart';
import 'package:mobile/features/horarios/models/crear_plantilla_horario_request.dart';
import 'package:mobile/features/horarios/models/dia_bloqueado.dart';
import 'package:mobile/features/horarios/models/estado_plantilla.dart';
import 'package:mobile/features/horarios/models/fechas.dart';
import 'package:mobile/features/horarios/models/franja.dart';
import 'package:mobile/features/horarios/models/plantilla_horario.dart';
import 'package:mobile/features/horarios/models/plantilla_horario_creada.dart';
import 'package:mobile/features/horarios/providers/horarios_service_provider.dart';
import 'package:mobile/features/horarios/services/horarios_service.dart';

PlantillaHorario horarioDePrueba({
  String codigo = 'h1',
  String nombre = 'Mañanas',
  EstadoPlantilla estado = EstadoPlantilla.activa,
  List<int> dias = const [1, 3],
  String horaInicio = '08:00',
  String horaFin = '12:00',
  int duracion = 60,
  DateTime? vigenteHasta,
}) => PlantillaHorario(
  codigo: codigo,
  nombre: nombre,
  diasSemana: dias,
  horaInicio: horaInicio,
  horaFin: horaFin,
  duracionClaseMin: duracion,
  vigenteDesde: DateTime(2026, 10, 26),
  vigenteHasta: vigenteHasta,
  estado: estado,
);

Franja franjaDePrueba({
  String codigo = 'f1',
  EstadoFranja estado = EstadoFranja.libre,
  String horaInicio = '10:00',
  String horaFin = '11:00',
  DateTime? fecha,
}) => Franja(
  codigo: codigo,
  fecha: fecha ?? DateTime(2099, 11, 2),
  horaInicio: horaInicio,
  horaFin: horaFin,
  estado: estado,
);

Pagina<T> paginaDe<T>(List<T> items) =>
    Pagina(items: items, page: 1, pageSize: 50, totalItems: items.length, totalPages: 1);

/// Servicio de horarios controlado desde el test: responde lo configurado y anota cada llamada.
///
/// * Un `Dio` vacío en el constructor: el real (ApiClient) necesita dotenv cargado.
/// * Las franjas de prueba están en el año 2099 para que nunca "ya hayan empezado".
class HorariosServiceFalso extends HorariosService {
  HorariosServiceFalso() : super(dio: Dio());

  List<PlantillaHorario> horarios = [];
  List<Franja> franjas = [];
  Object? error;

  /// Si es verdadero, el [error] se lanza una sola vez (después el servicio vuelve a responder bien).
  bool errorUnaVez = false;
  Future<void> Function()? antesDeResponder;

  /// Cuántas franjas dice haber cambiado `bloquearDia` y `desbloquearDia`.
  int franjasDelDia = 3;

  final List<String> llamadas = [];
  Set<EstadoPlantilla>? ultimosEstados;
  DateTime? ultimoDesde;
  DateTime? ultimoHasta;
  CrearPlantillaHorarioRequest? ultimoRequest;

  Future<void> _preparar(String llamada) async {
    llamadas.add(llamada);
    await antesDeResponder?.call();
    final fallo = error;
    if (fallo != null) {
      if (errorUnaVez) error = null;
      throw fallo;
    }
  }

  @override
  Future<Pagina<PlantillaHorario>> listarPlantillas({
    Set<EstadoPlantilla>? estados,
    int page = 1,
    int pageSize = 50,
  }) async {
    ultimosEstados = estados;
    await _preparar('listarPlantillas');
    return paginaDe(horarios);
  }

  @override
  Future<PlantillaHorarioCreada> crearPlantilla(CrearPlantillaHorarioRequest request) async {
    ultimoRequest = request;
    await _preparar('crearPlantilla');
    return PlantillaHorarioCreada(horario: horarioDePrueba(), franjasGeneradas: 14);
  }

  @override
  Future<void> pausarPlantilla(String codigo) async {
    await _preparar('pausarPlantilla:$codigo');
  }

  @override
  Future<void> reanudarPlantilla(String codigo) async {
    await _preparar('reanudarPlantilla:$codigo');
  }

  @override
  Future<void> desactivarPlantilla(String codigo) async {
    await _preparar('desactivarPlantilla:$codigo');
  }

  @override
  Future<Pagina<Franja>> listarFranjas({
    DateTime? desde,
    DateTime? hasta,
    EstadoFranja? estado,
    int page = 1,
    int pageSize = 50,
  }) async {
    ultimoDesde = desde;
    ultimoHasta = hasta;
    await _preparar('listarFranjas');
    return paginaDe(franjas);
  }

  @override
  Future<Franja> bloquearFranja(String codigo) async {
    await _preparar('bloquearFranja:$codigo');
    return franjaDePrueba(codigo: codigo, estado: EstadoFranja.bloqueada);
  }

  @override
  Future<Franja> desbloquearFranja(String codigo) async {
    await _preparar('desbloquearFranja:$codigo');
    return franjaDePrueba(codigo: codigo);
  }

  @override
  Future<DiaBloqueado> bloquearDia(DateTime fecha) async {
    await _preparar('bloquearDia:${fechaParaApi(fecha)}');
    return DiaBloqueado(fecha: fecha, franjasAfectadas: franjasDelDia);
  }

  @override
  Future<DiaBloqueado> desbloquearDia(DateTime fecha) async {
    await _preparar('desbloquearDia:${fechaParaApi(fecha)}');
    return DiaBloqueado(fecha: fecha, franjasAfectadas: franjasDelDia);
  }
}

Override horariosServiceFalso(HorariosServiceFalso falso) => horariosServiceProvider.overrideWithValue(falso);
