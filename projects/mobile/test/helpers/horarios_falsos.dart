import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:mobile/features/horarios/models/crear_plantilla_horario_request.dart';
import 'package:mobile/features/horarios/models/franja.dart';
import 'package:mobile/features/horarios/models/pagina.dart';
import 'package:mobile/features/horarios/models/plantilla_horario.dart';
import 'package:mobile/features/horarios/models/plantilla_horario_creada.dart';
import 'package:mobile/features/horarios/providers/horarios_service_provider.dart';
import 'package:mobile/features/horarios/services/horarios_service.dart';

PlantillaHorario horarioDePrueba({String codigo = 'h1', bool activa = true, int dia = 1}) => PlantillaHorario(
  codigo: codigo,
  diaSemana: dia,
  horaInicio: '08:00',
  horaFin: '12:00',
  duracionClaseMin: 60,
  vigenteDesde: DateTime(2026, 10, 26),
  activa: activa,
);

Franja franjaDePrueba({
  String codigo = 'f1',
  EstadoFranja estado = EstadoFranja.libre,
  String horaInicio = '10:00',
  String horaFin = '11:00',
  DateTime? fecha,
}) => Franja(codigo: codigo, fecha: fecha ?? DateTime(2026, 11, 2), horaInicio: horaInicio, horaFin: horaFin, estado: estado);

Pagina<T> paginaDe<T>(List<T> items) =>
    Pagina(items: items, page: 1, pageSize: 50, totalItems: items.length, totalPages: 1);

/// Servicio de horarios controlado desde el test: responde lo configurado y anota cada llamada.
///
/// * Un `Dio` vacío en el constructor: el real (ApiClient) necesita dotenv cargado.
class HorariosServiceFalso extends HorariosService {
  HorariosServiceFalso() : super(dio: Dio());

  List<PlantillaHorario> horarios = [];
  List<Franja> franjas = [];
  Object? error;

  /// Si es verdadero, el [error] se lanza una sola vez (después el servicio vuelve a responder bien).
  bool errorUnaVez = false;
  Future<void> Function()? antesDeResponder;

  final List<String> llamadas = [];
  bool? ultimaActiva;
  DateTime? ultimaFecha;
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
  Future<Pagina<PlantillaHorario>> listarPlantillas({bool? activa, int page = 1, int pageSize = 50}) async {
    ultimaActiva = activa;
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
  Future<void> desactivarPlantilla(String codigo) async {
    await _preparar('desactivarPlantilla:$codigo');
  }

  @override
  Future<Pagina<Franja>> listarFranjas({DateTime? fecha, EstadoFranja? estado, int page = 1, int pageSize = 50}) async {
    ultimaFecha = fecha;
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
}

Override horariosServiceFalso(HorariosServiceFalso falso) => horariosServiceProvider.overrideWithValue(falso);
